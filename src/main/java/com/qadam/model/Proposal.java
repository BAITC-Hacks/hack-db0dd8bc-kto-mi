package com.qadam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A team's proposal for a published task. Accepted or rejected only manually by the business.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 2000)
    private String idea;

    @Column(nullable = false, length = 4000)
    private String plan;

    @Column(nullable = false, length = 100)
    private String duration;

    @Column(length = 500)
    private String prototypeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProposalStatus status = ProposalStatus.PENDING;

    /** Milestones confirmed by the business; each one gave the team {@code +10} points. */
    private int confirmedMilestones;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
