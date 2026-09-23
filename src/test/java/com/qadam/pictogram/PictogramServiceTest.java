package com.qadam.pictogram;

import com.qadam.config.PictogramProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PictogramServiceTest {

    private static final String API_URL = "https://api.arasaac.test/v1";
    private static final String IMAGE_URL = "https://static.arasaac.org/pictograms";

    private static final String WATER_RESULTS = """
            [
              {"_id": 32464, "keywords": [{"type": 2, "keyword": "вода"}], "schematic": true},
              {"_id": 2248, "keywords": [{"type": 2, "keyword": "вода"}], "schematic": false}
            ]
            """;

    private MockRestServiceServer server;
    private PictogramService service;

    @BeforeEach
    void setUp() {
        service = createService(true);
    }

    @Test
    void returnsImageUrlOfFirstSearchResult() {
        expectSearch("вода", withSuccess(WATER_RESULTS, MediaType.APPLICATION_JSON));

        assertThat(service.findPictogramUrl("вода"))
                .contains("https://static.arasaac.org/pictograms/32464/32464_500.png");
        server.verify();
    }

    @Test
    void normalizesWordBeforeSearch() {
        expectSearch("вода", withSuccess(WATER_RESULTS, MediaType.APPLICATION_JSON));

        assertThat(service.findPictogramUrl("  Вода! ")).isPresent();
        server.verify();
    }

    @Test
    void emptyResultReturnsEmpty() {
        expectSearch("вода", withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(service.findPictogramUrl("вода")).isEmpty();
        server.verify();
    }

    @Test
    void notFoundReturnsEmpty() {
        expectSearch("фотосинтез", withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("[]"));

        assertThat(service.findPictogramUrl("фотосинтез")).isEmpty();
        server.verify();
    }

    @Test
    void serverErrorReturnsEmpty() {
        expectSearch("вода", withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(service.findPictogramUrl("вода")).isEmpty();
        server.verify();
    }

    @Test
    void networkErrorReturnsEmpty() {
        expectSearch("вода", withException(new IOException("Connection refused")));

        assertThat(service.findPictogramUrl("вода")).isEmpty();
        server.verify();
    }

    @Test
    void repeatedWordIsServedFromCache() {
        expectSearch("вода", withSuccess(WATER_RESULTS, MediaType.APPLICATION_JSON));

        assertThat(service.findPictogramUrl("вода")).isPresent();
        assertThat(service.findPictogramUrl("Вода.")).isPresent();
        server.verify();
    }

    @Test
    void notFoundIsCached() {
        expectSearch("фотосинтез", withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("[]"));

        assertThat(service.findPictogramUrl("фотосинтез")).isEmpty();
        assertThat(service.findPictogramUrl("фотосинтез")).isEmpty();
        server.verify();
    }

    @Test
    void serverErrorIsNotCached() {
        expectSearch("вода", withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        expectSearch("вода", withSuccess(WATER_RESULTS, MediaType.APPLICATION_JSON));

        assertThat(service.findPictogramUrl("вода")).isEmpty();
        assertThat(service.findPictogramUrl("вода")).isPresent();
        server.verify();
    }

    @Test
    void blankWordIsNotSearched() {
        assertThat(service.findPictogramUrl(null)).isEmpty();
        assertThat(service.findPictogramUrl(" ?! ")).isEmpty();
        server.verify();
    }

    @Test
    void disabledServiceMakesNoRequests() {
        PictogramService disabled = createService(false);

        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.findPictogramUrl("вода")).isEmpty();
        server.verify();
    }

    @Test
    void normalizeTrimsPunctuationAndLowercases() {
        assertThat(PictogramService.normalize("  «Солнце»,\t")).isEqualTo("солнце");
        assertThat(PictogramService.normalize("Части растения")).isEqualTo("части растения");
    }

    private PictogramService createService(boolean enabled) {
        RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        PictogramProperties properties = new PictogramProperties(enabled, "ru", Duration.ofSeconds(5), API_URL, IMAGE_URL);
        return new PictogramService(builder.build(), properties);
    }

    private void expectSearch(String word, ResponseCreator response) {
        URI uri = UriComponentsBuilder.fromUriString(API_URL + PictogramService.SEARCH_PATH)
                .buildAndExpand("ru", word)
                .encode()
                .toUri();
        server.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(response);
    }
}
