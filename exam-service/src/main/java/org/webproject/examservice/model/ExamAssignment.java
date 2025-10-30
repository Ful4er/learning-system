package org.webproject.examservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "exam_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id", "student_id"})
})
@Data
public class ExamAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();
}