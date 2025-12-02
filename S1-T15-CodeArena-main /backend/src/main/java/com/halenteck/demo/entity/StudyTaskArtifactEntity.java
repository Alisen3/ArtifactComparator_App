package com.halenteck.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "study_task_artifacts")
public class StudyTaskArtifactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private StudyTaskDefinitionEntity definition;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    private ArtifactEntity artifact;

    @Column(nullable = false)
    private int position = 0;

    public StudyTaskArtifactEntity() {
    }

    public StudyTaskArtifactEntity(StudyTaskDefinitionEntity definition, ArtifactEntity artifact, int position) {
        this.definition = definition;
        this.artifact = artifact;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public StudyTaskDefinitionEntity getDefinition() {
        return definition;
    }

    public ArtifactEntity getArtifact() {
        return artifact;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}

