package org.webproject.examservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "question_options")
@Data
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "is_correct")
    private Boolean isCorrect = false;

    @Column(name = "order_index")
    private Integer orderIndex;
}