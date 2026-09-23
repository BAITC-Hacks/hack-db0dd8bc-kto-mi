package com.qadam.dto;

import com.qadam.model.Team;

import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        List<String> interests,
        List<String> skills,
        List<String> technologies,
        int points
) {

    public static TeamResponse of(Team team) {
        return new TeamResponse(team.getId(), team.getName(), List.copyOf(team.getInterests()),
                List.copyOf(team.getSkills()), List.copyOf(team.getTechnologies()), team.getPoints());
    }
}
