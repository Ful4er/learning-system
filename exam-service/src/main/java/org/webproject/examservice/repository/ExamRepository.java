package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.Exam;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findAllByTeacherId(Long teacherId);
    List<Exam> findByTeacherId(Long teacherId);

    @Query("""
            select e as exam,
                   count(distinct q.id) as questionCount,
                   count(distinct ea.id) as assignedStudentCount
              from Exam e
              left join Question q on q.examId = e.id
              left join ExamAssignment ea on ea.examId = e.id
             where e.id = :examId
             group by e
            """)
    Optional<ExamWithCounts> findExamWithCountsById(@Param("examId") Long examId);

    interface ExamWithCounts {
        Exam getExam();
        long getQuestionCount();
        long getAssignedStudentCount();
    }
}