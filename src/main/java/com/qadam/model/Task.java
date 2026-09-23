package com.qadam.model;

import com.qadam.dto.TaskCard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * A business task: the original draft, the structured card and its stored rating.
 * {@code score} and {@code level} are recalculated on every card change and used for catalog sorting and filtering.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Industry industry;

    @Lob
    @Column(nullable = false)
    private String draftText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status = TaskStatus.DRAFT;

    @Column(nullable = false, length = TaskCard.TITLE_MAX)
    private String title = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String context = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String need = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String users = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String data = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String constraints = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String expectedResult = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String successCriteria = "";

    @Column(nullable = false, length = TaskCard.CONTACT_MAX)
    private String contact = "";

    @Column(nullable = false, length = TaskCard.FIELD_MAX)
    private String interactionFormat = "";

    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RatingLevel level = RatingLevel.DRAFT;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public TaskCard getCard() {
        return new TaskCard(title, context, need, users, data, constraints, expectedResult, successCriteria,
                contact, interactionFormat);
    }

    public void setCard(TaskCard card) {
        title = card.title();
        context = card.context();
        need = card.need();
        users = card.users();
        data = card.data();
        constraints = card.constraints();
        expectedResult = card.expectedResult();
        successCriteria = card.successCriteria();
        contact = card.contact();
        interactionFormat = card.interactionFormat();
    }
}
