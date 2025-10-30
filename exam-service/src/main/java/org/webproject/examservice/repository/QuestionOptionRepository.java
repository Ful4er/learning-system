package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.QuestionOption;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findAllByQuestionIdOrderByOrderIndexAsc(Long questionId);
}



