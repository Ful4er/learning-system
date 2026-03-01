package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.Question;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByExamIdOrderByIdAsc(Long examId);

    long countByExamId(Long examId);

    List<Question> findAllByIdIn(List<Long> ids);

    @Query("select q.examId as examId, count(q.id) as cnt from Question q where q.examId in :examIds group by q.examId")
    List<ExamIdCount> countByExamIdIn(List<Long> examIds);

    interface ExamIdCount {
        Long getExamId();
        Long getCnt();
    }
}



