package com.qadam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A student team. {@code points} grow with every confirmed milestone of an accepted proposal.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 2000)
    private List<String> interests = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 2000)
    private List<String> skills = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 2000)
    private List<String> technologies = new ArrayList<>();

    private int points;

    public Team(String name, List<String> interests, List<String> skills, List<String> technologies) {
        this.name = name;
        this.interests = new ArrayList<>(interests);
        this.skills = new ArrayList<>(skills);
        this.technologies = new ArrayList<>(technologies);
    }
}
