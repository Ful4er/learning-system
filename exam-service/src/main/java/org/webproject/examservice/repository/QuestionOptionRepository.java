package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.QuestionOption;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findAllByQuestionIdOrderByOrderIndexAsc(Long questionId);

    List<QuestionOption> findAllByQuestionIdInOrderByQuestionIdAscOrderIndexAsc(List<Long> questionIds);

    @Query("select qo from QuestionOption qo where qo.questionId in :questionIds and qo.isCorrect = true order by qo.questionId asc, qo.orderIndex asc")
    List<QuestionOption> findCorrectOptionsByQuestionIdInOrderByQuestionIdAscOrderIndexAsc(List<Long> questionIds);
}



