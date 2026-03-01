package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.StudentAnswer;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    List<StudentAnswer> findAllByAttemptId(Long attemptId);

    Optional<StudentAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}



