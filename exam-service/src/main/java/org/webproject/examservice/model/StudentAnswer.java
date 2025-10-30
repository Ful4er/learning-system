package org.webproject.examservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "student_answers")
@Data
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option_ids", columnDefinition = "json")
    private String selectedOptionIds;

    @Column(columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "answered_at")
    private Instant answeredAt = Instant.now();
}