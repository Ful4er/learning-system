package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.ExamAttempt;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findAllByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamAttempt> findAllByExamIdAndStudentIdIn(Long examId, List<Long> studentIds);

    List<ExamAttempt> findAllByExamIdInAndStudentId(List<Long> examIds, Long studentId);

    Optional<ExamAttempt> findFirstByExamIdAndStudentIdAndStatus(Long examId, Long studentId, ExamAttempt.AttemptStatus status);

    boolean existsByExamIdAndStudentIdAndStatus(Long examId, Long studentId, ExamAttempt.AttemptStatus status);
}



