package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.ExamAssignment;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, Long> {
    List<ExamAssignment> findAllByStudentId(Long studentId);
    List<ExamAssignment> findAllByExamId(Long examId);
    List<ExamAssignment> findAllByExamIdIn(List<Long> examIds);
    Optional<ExamAssignment> findByExamIdAndStudentId(Long examId, Long studentId);
}