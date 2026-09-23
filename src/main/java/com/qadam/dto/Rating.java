package com.qadam.dto;

import com.qadam.model.RatingLevel;

import java.util.List;

/**
 * Transparent rating of a task card, see {@link com.qadam.service.RatingService}.
 *
 * @param missing codes of card fields that are not filled meaningfully
 * @param tips    what to fill in to raise the rating, with the number of points
 */
public record Rating(
        int score,
        RatingLevel level,
        String levelName,
        List<RatingItem> breakdown,
        List<String> missing,
        List<String> tips
) {

    public record RatingItem(String criterion, int points, int maxPoints, String reason) {
    }
}
