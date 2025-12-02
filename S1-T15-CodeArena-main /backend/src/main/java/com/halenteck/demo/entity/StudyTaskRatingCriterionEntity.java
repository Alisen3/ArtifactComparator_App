package com.halenteck.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "study_task_rating_criteria")
public class StudyTaskRatingCriterionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private StudyTaskDefinitionEntity definition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rating_criterion_id", nullable = false)
    private StudyRatingCriterionEntity ratingCriterion;

    public StudyTaskRatingCriterionEntity() {
    }

    public StudyTaskRatingCriterionEntity(StudyTaskDefinitionEntity definition,
                                          StudyRatingCriterionEntity ratingCriterion) {
        this.definition = definition;
        this.ratingCriterion = ratingCriterion;
    }

    public Long getId() {
        return id;
    }

    public StudyTaskDefinitionEntity getDefinition() {
        return definition;
    }

    public StudyRatingCriterionEntity getRatingCriterion() {
        return ratingCriterion;
    }
}

