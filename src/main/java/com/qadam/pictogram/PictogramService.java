package com.qadam.pictogram;

import com.qadam.config.PictogramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Finds a pictogram for a word through the ARASAAC API.
 *
 * <p>Definitive answers (found / not found) are cached in memory per normalized word. Network errors
 * and server errors are logged and not cached, so the word is retried next time. Never throws.
 */
@Slf4j
public class PictogramService {

    static final String SEARCH_PATH = "/pictograms/{locale}/search/{word}";
    static final int IMAGE_SIZE = 500;

    private static final Pattern EDGE_JUNK = Pattern.compile("^[\\p{P}\\p{S}\\s]+|[\\p{P}\\p{S}\\s]+$");
    private static final ParameterizedTypeReference<List<PictogramSearchResult>> RESULTS = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final PictogramProperties properties;
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    /**
     * @param restClient client configured with the ARASAAC API base URL and timeouts
     */
    public PictogramService(RestClient restClient, PictogramProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    /**
     * Returns the image URL of the most relevant pictogram for the word, or empty if none was found
     * or ARASAAC is unavailable.
     */
    public Optional<String> findPictogramUrl(String word) {
        if (!isEnabled() || word == null) {
            return Optional.empty();
        }
        String normalized = normalize(word);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> cached = cache.get(normalized);
        if (cached != null) {
            return cached;
        }
        return search(normalized);
    }

    static String normalize(String word) {
        return EDGE_JUNK.matcher(word.strip()).replaceAll("").toLowerCase(Locale.ROOT);
    }

    private Optional<String> search(String word) {
        try {
            List<PictogramSearchResult> results = restClient.get()
                    .uri(SEARCH_PATH, properties.locale(), word)
                    .retrieve()
                    .body(RESULTS);
            if (results == null || results.isEmpty()) {
                log.warn("No ARASAAC pictogram found for '{}'", word);
                return remember(word, Optional.empty());
            }
            return remember(word, Optional.of(imageUrl(results.getFirst().id())));
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("No ARASAAC pictogram found for '{}' (HTTP 404)", word);
            return remember(word, Optional.empty());
        } catch (RuntimeException e) {
            log.warn("ARASAAC pictogram lookup for '{}' failed: {}", word, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> remember(String word, Optional<String> url) {
        cache.putIfAbsent(word, url);
        return url;
    }

    private String imageUrl(long id) {
        return "%s/%d/%d_%d.png".formatted(properties.imageBaseUrl(), id, id, IMAGE_SIZE);
    }
}
