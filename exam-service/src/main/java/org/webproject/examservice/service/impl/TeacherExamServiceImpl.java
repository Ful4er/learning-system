package org.webproject.examservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webproject.examservice.client.UserServiceClient;
import org.webproject.examservice.dto.request.AddQuestionRequest;
import org.webproject.examservice.dto.request.AssignStudentsRequest;
import org.webproject.examservice.dto.request.CreateExamRequest;
import org.webproject.examservice.dto.request.UpdateExamRequest;
import org.webproject.examservice.dto.request.UpdateQuestionRequest;
import org.webproject.examservice.dto.response.ExamAssignmentResponse;
import org.webproject.examservice.dto.response.ExamAttemptResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;
import org.webproject.examservice.dto.response.StudentAnswerResponse;
import org.webproject.examservice.dto.response.StudentExamResultResponse;
import org.webproject.examservice.dto.response.UserDto;
import org.webproject.examservice.exception.*;
import org.webproject.examservice.model.*;
import org.webproject.examservice.model.StudentAnswer;
import org.webproject.examservice.repository.*;
import org.webproject.examservice.service.TeacherExamService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    private final ExamAttemptRepository examAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final UserServiceClient userServiceClient;

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
    @Transactional(readOnly = true)
    public List<UserDto> getStudentsByTeacher(Long teacherId) {
        List<Exam> exams = examRepository.findAllByTeacherId(teacherId);
        List<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toList());

        List<ExamAssignment> assignments = examAssignmentRepository.findAllByExamIdIn(examIds);
        List<Long> studentIds = assignments.stream()
                .map(ExamAssignment::getStudentId)
                .distinct()
                .toList();

        List<UserDto> students = new ArrayList<>();
        for (Long studentId : studentIds) {
            UserDto user = userServiceClient.getUserById(studentId);
            if (user != null) {
                students.add(user);
            }
        }
        
        return students;
    }

    @Transactional
    @Override
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

        UserDto user = userServiceClient.getUserById(assignment.getStudentId());
        if (user != null) {
            r.setStudentName(user.getFirstName() + " " + user.getLastName());
            r.setStudentFirstName(user.getFirstName());
            r.setStudentLastName(user.getLastName());
            r.setStudentEmail(user.getEmail());
        }

        List<ExamAttempt> attempts = examAttemptRepository.findAllByExamIdAndStudentId(
                assignment.getExamId(),
                assignment.getStudentId()
        );
        if (!attempts.isEmpty()) {
            ExamAttempt latestAttempt = attempts.stream()
                    .max(Comparator.comparing(this::resolveAttemptTimestamp))
                    .orElse(attempts.get(attempts.size() - 1));

            r.setCompletedAt(latestAttempt.getFinishedAt());
            if (latestAttempt.getCalculatedScore() != null) {
                r.setScore((int) Math.round(latestAttempt.getCalculatedScore()));
            }
        }

        return r;
    }

    private Instant resolveAttemptTimestamp(ExamAttempt attempt) {
        return attempt.getFinishedAt() != null ? attempt.getFinishedAt() : attempt.getStartedAt();
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

    @Override
    @Transactional(readOnly = true)
    public List<StudentExamResultResponse> getStudentResultsForTeacher(Long teacherId, Long studentId) {
        List<StudentExamResultResponse> results = new ArrayList<>();

        List<Exam> exams = examRepository.findAllByTeacherId(teacherId);
        for (Exam exam : exams) {
            StudentExamResultResponse r = new StudentExamResultResponse();
            r.setExamId(exam.getId());
            r.setExamTitle(exam.getTitle());
            r.setExamStatus(exam.getStatus().name());
            r.setPassingScore(exam.getPassingScore());

            examAssignmentRepository.findByExamIdAndStudentId(exam.getId(), studentId).ifPresent(a -> {
                r.setAssigned(true);
                r.setAssignedAt(a.getAssignedAt());
            });

            List<ExamAttempt> attempts = examAttemptRepository.findAllByExamIdAndStudentId(exam.getId(), studentId);
            r.setAttemptsCount(attempts.size());
            if (!attempts.isEmpty()) {
                ExamAttempt last = attempts.stream()
                        .max((a, b) -> a.getStartedAt().compareTo(b.getStartedAt()))
                        .orElse(attempts.get(attempts.size() - 1));
                r.setLastAttemptId(last.getId());
                r.setLastAttemptStatus(last.getStatus().name());
                r.setLastAttemptScore(last.getCalculatedScore());
                r.setLastAttemptFinishedAt(last.getFinishedAt());
            }

            results.add(r);
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamAttemptResponse> getExamAttemptsForTeacher(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException("Exam not found"));
        if (!Objects.equals(exam.getTeacherId(), teacherId)) {
            throw new RuntimeException("Not authorized");
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamAttemptResponse getExamAttemptDetails(Long examId, Long attemptId, Long teacherId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException("Exam not found"));
        if (!Objects.equals(exam.getTeacherId(), teacherId)) {
            throw new RuntimeException("Not authorized");
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (!Objects.equals(attempt.getExamId(), examId)) {
            throw new RuntimeException("Attempt does not belong to this exam");
        }
        ExamAttemptResponse r = new ExamAttemptResponse();
        r.setId(attempt.getId());
        r.setExamId(attempt.getExamId());
        r.setStudentId(attempt.getStudentId());
        r.setStartedAt(attempt.getStartedAt());
        r.setFinishedAt(attempt.getFinishedAt());
        r.setStatus(attempt.getStatus().name());
        r.setScore(attempt.getCalculatedScore() != null ? attempt.getCalculatedScore() : 0D);
        r.setExamTitle(exam.getTitle());
        r.setTotalQuestions(questionRepository.findAllByExamIdOrderByIdAsc(examId).size());
        r.setPassed(r.getScore() >= exam.getPassingScore());
        r.setAnswers(mapStudentAnswers(attemptId));
        return r;
    }

    private List<StudentAnswerResponse> mapStudentAnswers(Long attemptId) {
        return studentAnswerRepository.findAllByAttemptId(attemptId).stream()
                .map(this::toStudentAnswerResponse)
                .collect(Collectors.toList());
    }

    private StudentAnswerResponse toStudentAnswerResponse(StudentAnswer answer) {
        StudentAnswerResponse resp = new StudentAnswerResponse();
        resp.setQuestionId(answer.getQuestionId());

        if (answer.getSelectedOptionIds() != null && !answer.getSelectedOptionIds().isBlank()) {
            List<Long> ids = Arrays.stream(answer.getSelectedOptionIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(id -> {
                        try {
                            return Long.parseLong(id);
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            resp.setSelectedOptionIds(ids);
        } else {
            resp.setSelectedOptionIds(Collections.emptyList());
        }

        resp.setTextAnswer(answer.getTextAnswer());
        return resp;
    }
}




