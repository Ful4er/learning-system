package org.webproject.examservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "questions")
@Data
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(name = "points")
    private Integer points = 1;

    @OrderBy("orderIndex ASC")
    @OneToMany(mappedBy = "questionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuestionOption> options;

    public enum QuestionType {
        SINGLE_CHOICE, MULTIPLE_CHOICE, TEXT
    }
}