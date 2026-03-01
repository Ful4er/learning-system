package org.webproject.examservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.webproject.examservice.model.ExamAssignment;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, Long> {
    List<ExamAssignment> findAllByStudentId(Long studentId);
    List<ExamAssignment> findAllByExamId(Long examId);
    List<ExamAssignment> findAllByExamIdIn(List<Long> examIds);
    List<ExamAssignment> findAllByExamIdInAndStudentId(List<Long> examIds, Long studentId);
    Optional<ExamAssignment> findByExamIdAndStudentId(Long examId, Long studentId);
    boolean existsByExamIdAndStudentId(Long examId, Long studentId);
    long countByExamId(Long examId);
    @Query("select ea.examId as examId, count(ea.id) as cnt from ExamAssignment ea where ea.examId in :examIds group by ea.examId")
    List<ExamIdCount> countByExamIdIn(List<Long> examIds);

    interface ExamIdCount {
        Long getExamId();
        Long getCnt();
    }
}