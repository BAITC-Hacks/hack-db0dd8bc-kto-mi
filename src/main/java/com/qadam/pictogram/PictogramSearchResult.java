package com.qadam.pictogram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One item of the ARASAAC {@code GET /pictograms/{locale}/search/{word}} response; only the id is used.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PictogramSearchResult(@JsonProperty("_id") long id) {
}
