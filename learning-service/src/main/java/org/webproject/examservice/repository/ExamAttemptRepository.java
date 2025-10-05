package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.ExamAttempt;

import java.util.List;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findAllByExamIdAndStudentId(Long examId, Long studentId);
}


