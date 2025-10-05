package org.webproject.examservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "exams")
@Data
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "passing_score")
    private Integer passingScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExamStatus status = ExamStatus.DRAFT;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum ExamStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}