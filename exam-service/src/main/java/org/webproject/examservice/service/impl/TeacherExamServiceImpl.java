package org.webproject.examservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webproject.examservice.dto.request.AddQuestionRequest;
import org.webproject.examservice.dto.request.AssignStudentsRequest;
import org.webproject.examservice.dto.request.CreateExamRequest;
import org.webproject.examservice.dto.request.UpdateExamRequest;
import org.webproject.examservice.dto.request.UpdateQuestionRequest;
import org.webproject.examservice.dto.response.ExamAssignmentResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;
import org.webproject.examservice.exception.*;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.ExamAssignment;
import org.webproject.examservice.model.Question;
import org.webproject.examservice.model.QuestionOption;
import org.webproject.examservice.repository.ExamAssignmentRepository;
import org.webproject.examservice.repository.ExamRepository;
import org.webproject.examservice.repository.QuestionOptionRepository;
import org.webproject.examservice.repository.QuestionRepository;
import org.webproject.examservice.service.TeacherExamService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherExamServiceImpl implements TeacherExamService {

    private final ExamRepository examRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    @Override
    @Transactional
    public ExamResponse createExam(CreateExamRequest request) {
        Exam exam = new Exam();
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setTeacherId(request.getTeacherId());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setPassingScore(request.getPassingScore());
        Exam saved = examRepository.save(exam);
        return toExamResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getExamById(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        return toExamResponse(exam);
    }

    @Override
    @Transactional
    public ExamResponse updateExam(Long examId, Long teacherId, UpdateExamRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);
        ensureNotPublished(exam);

        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setPassingScore(request.getPassingScore());

        Exam saved = examRepository.save(exam);
        return toExamResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExam(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        examRepository.delete(exam);
    }

    @Override
    @Transactional
    public ExamResponse publishExam(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        if (exam.getStatus() == Exam.ExamStatus.PUBLISHED) {
            throw new ExamAlreadyPublishedException(examId);
        }

        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        Exam saved = examRepository.save(exam);
        return toExamResponse(saved);
    }

    @Override
    @Transactional
    public ExamResponse archiveExam(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        exam.setStatus(Exam.ExamStatus.ARCHIVED);
        Exam saved = examRepository.save(exam);
        return toExamResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamResponse> getExamsByTeacher(Long teacherId) {
        return examRepository.findAllByTeacherId(teacherId).stream()
                .map(this::toExamResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamAssignmentResponse> getExamAssignments(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        return examAssignmentRepository.findAllByExamId(examId).stream()
                .map(this::toExamAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignStudents(Long examId, Long teacherId, AssignStudentsRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        for (Long studentId : request.getStudentIds()) {
            Optional<ExamAssignment> existing = examAssignmentRepository.findByExamIdAndStudentId(examId, studentId);
            if (existing.isPresent()) {
                throw new StudentAlreadyAssignedException(examId, studentId);
            }

            ExamAssignment assignment = new ExamAssignment();
            assignment.setExamId(examId);
            assignment.setStudentId(studentId);
            examAssignmentRepository.save(assignment);
        }
    }

    @Override
    @Transactional
    public void removeStudentAssignment(Long examId, Long studentId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        ExamAssignment assignment = examAssignmentRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        examAssignmentRepository.delete(assignment);
    }

    @Override
    @Transactional
    public QuestionResponse addQuestion(Long examId, Long teacherId, AddQuestionRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);
        ensureNotPublished(exam);

        Question question = new Question();
        question.setExamId(examId);
        question.setText(request.getText());
        question.setType(Question.QuestionType.valueOf(request.getType()));
        question.setPoints(request.getPoints());
        Question savedQuestion = questionRepository.save(question);

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            List<QuestionOption> options = new ArrayList<>();
            for (AddQuestionRequest.OptionPayload op : request.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(savedQuestion.getId());
                option.setText(op.getText());
                option.setIsCorrect(Boolean.TRUE.equals(op.getIsCorrect()));
                option.setOrderIndex(op.getOrderIndex());
                options.add(option);
            }
            questionOptionRepository.saveAll(options);
        }

        return toQuestionResponse(savedQuestion, true);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        return toQuestionResponse(question, false);
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(Long questionId, Long teacherId, UpdateQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        Exam exam = examRepository.findById(question.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(question.getExamId()));
        ensureOwner(exam, teacherId);
        ensureNotPublished(exam);

        question.setText(request.getText());
        question.setPoints(request.getPoints());
        Question savedQuestion = questionRepository.save(question);

        if (request.getOptions() != null) {
            List<QuestionOption> existingOptions = questionOptionRepository.findAllByQuestionIdOrderByOrderIndexAsc(questionId);
            questionOptionRepository.deleteAll(existingOptions);

            if (!request.getOptions().isEmpty()) {
                List<QuestionOption> newOptions = new ArrayList<>();
                for (UpdateQuestionRequest.OptionPayload op : request.getOptions()) {
                    QuestionOption option = new QuestionOption();
                    option.setQuestionId(questionId);
                    option.setText(op.getText());
                    option.setIsCorrect(Boolean.TRUE.equals(op.getIsCorrect()));
                    option.setOrderIndex(op.getOrderIndex());
                    newOptions.add(option);
                }
                questionOptionRepository.saveAll(newOptions);
            }
        }

        return toQuestionResponse(savedQuestion, true);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId, Long teacherId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        Exam exam = examRepository.findById(question.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(question.getExamId()));
        ensureOwner(exam, teacherId);
        ensureNotPublished(exam);

        questionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getExamQuestionsForTeacher(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        return questionRepository.findAllByExamIdOrderByIdAsc(examId).stream()
                .map(q -> toQuestionResponse(q, true))
                .collect(Collectors.toList());
    }

    private void ensureOwner(Exam exam, Long teacherId) {
        if (!Objects.equals(exam.getTeacherId(), teacherId)) {
            throw new AccessDeniedException(teacherId, "exam " + exam.getId());
        }
    }

    private void ensureNotPublished(Exam exam) {
        if (exam.getStatus() == Exam.ExamStatus.PUBLISHED) {
            throw new ExamAlreadyPublishedException(exam.getId());
        }
    }

    private ExamResponse toExamResponse(Exam exam) {
        ExamResponse r = new ExamResponse();
        r.setId(exam.getId());
        r.setTitle(exam.getTitle());
        r.setDescription(exam.getDescription());
        r.setTeacherId(exam.getTeacherId());
        r.setCreatedAt(exam.getCreatedAt());
        r.setUpdatedAt(exam.getUpdatedAt());
        r.setDurationMinutes(exam.getDurationMinutes());
        r.setPassingScore(exam.getPassingScore());
        r.setStatus(exam.getStatus().name());
        r.setQuestionCount(questionRepository.findAllByExamIdOrderByIdAsc(exam.getId()).size());
        r.setAssignedStudentCount(examAssignmentRepository.findAllByExamId(exam.getId()).size());
        return r;
    }

    private ExamAssignmentResponse toExamAssignmentResponse(ExamAssignment assignment) {
        ExamAssignmentResponse r = new ExamAssignmentResponse();
        r.setId(assignment.getId());
        r.setExamId(assignment.getExamId());
        r.setStudentId(assignment.getStudentId());
        r.setAssignedAt(assignment.getAssignedAt());
        return r;
    }

    private QuestionResponse toQuestionResponse(Question q, boolean showCorrectAnswers) {
        QuestionResponse r = new QuestionResponse();
        r.setId(q.getId());
        r.setExamId(q.getExamId());
        r.setText(q.getText());
        r.setType(q.getType().name());
        r.setPoints(q.getPoints());

        List<QuestionOption> opts = questionOptionRepository.findAllByQuestionIdOrderByOrderIndexAsc(q.getId());
        List<QuestionResponse.Option> mapped = opts.stream().map(o -> {
            QuestionResponse.Option ro = new QuestionResponse.Option();
            ro.setId(o.getId());
            ro.setText(o.getText());
            ro.setOrderIndex(o.getOrderIndex());
            if (showCorrectAnswers) {
                ro.setIsCorrect(o.getIsCorrect());
            }
            return ro;
        }).collect(Collectors.toList());
        r.setOptions(mapped);
        return r;
    }
}




