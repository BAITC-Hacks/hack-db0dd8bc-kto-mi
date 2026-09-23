package com.qadam.dto;

import java.util.List;

/**
 * A published task recommended to a team.
 *
 * @param matchScore      number of team keywords (interests, skills, technologies) found in the task text
 * @param matchedKeywords those keywords, as written in the team profile
 */
public record RecommendationResponse(
        TaskResponse task,
        int matchScore,
        List<String> matchedKeywords
) {
}
