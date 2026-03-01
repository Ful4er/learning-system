package org.webproject.examservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
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
import org.webproject.examservice.util.Role;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
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
    @CacheEvict(value = "exam-list", key = "'teacher:' + #request.teacherId")
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
    @Cacheable(value = "exam-details", key = "#examId")
    public ExamResponse getExamById(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        int questionCount = (int) questionRepository.countByExamId(examId);
        int assignedCount = (int) examAssignmentRepository.countByExamId(examId);
        return toExamResponse(exam, questionCount, assignedCount);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "exam-details", key = "#examId"),
            @CacheEvict(value = "exam-list", key = "'teacher:' + #teacherId"),
            @CacheEvict(value = "student-results", key = "'teacher:' + #teacherId", allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "exam-details", key = "#examId"),
            @CacheEvict(value = "exam-list", key = "'teacher:' + #teacherId"),
            @CacheEvict(value = "question-list", allEntries = true),
            @CacheEvict(value = "student-results", key = "'teacher:' + #teacherId", allEntries = true)
    })
    public void deleteExam(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        examRepository.delete(exam);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "exam-details", key = "#examId"),
            @CacheEvict(value = "exam-list", key = "'teacher:' + #teacherId"),
            @CacheEvict(value = "student-results", key = "'teacher:' + #teacherId", allEntries = true)
    })
    public ExamResponse publishExam(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        if (exam.getStatus() == Exam.ExamStatus.PUBLISHED) {
            return toExamResponse(exam);
        }

        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        Exam saved = examRepository.save(exam);
        return toExamResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "exam-details", key = "#examId"),
            @CacheEvict(value = "exam-list", key = "'teacher:' + #teacherId"),
            @CacheEvict(value = "student-results", key = "'teacher:' + #teacherId", allEntries = true)
    })
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
    @Cacheable(value = "exam-list", key = "'teacher:' + #teacherId")
    public List<ExamResponse> getExamsByTeacher(Long teacherId) {
        List<Exam> exams = examRepository.findAllByTeacherId(teacherId);
        if (exams.isEmpty()) {
            return List.of();
        }

        List<Long> examIds = exams.stream().map(Exam::getId).filter(Objects::nonNull).toList();
        Map<Long, Long> questionCountByExamId = questionRepository.countByExamIdIn(examIds).stream()
                .collect(Collectors.toMap(QuestionRepository.ExamIdCount::getExamId, QuestionRepository.ExamIdCount::getCnt));
        Map<Long, Long> assignedCountByExamId = examAssignmentRepository.countByExamIdIn(examIds).stream()
                .collect(Collectors.toMap(ExamAssignmentRepository.ExamIdCount::getExamId, ExamAssignmentRepository.ExamIdCount::getCnt));

        return exams.stream()
                .map(exam -> toExamResponse(exam,
                        questionCountByExamId.getOrDefault(exam.getId(), 0L).intValue(),
                        assignedCountByExamId.getOrDefault(exam.getId(), 0L).intValue()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamAssignmentResponse> getExamAssignments(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        List<ExamAssignment> assignments = examAssignmentRepository.findAllByExamId(examId);
        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = assignments.stream()
                .map(ExamAssignment::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserDto> usersById;
        if (studentIds.isEmpty()) {
            usersById = new HashMap<>();
        } else {
            usersById = userServiceClient.getUsersByIds(studentIds).stream()
                    .filter(Objects::nonNull)
                    .filter(u -> u.getId() != null)
                    .collect(Collectors.toMap(
                            UserDto::getId,
                            u -> u,
                            (a, b) -> a
                    ));
        }

        return mapExamAssignmentsWithAttempts(examId, assignments, usersById);
    }

    private List<ExamAssignmentResponse> mapExamAssignmentsWithAttempts(Long examId, List<ExamAssignment> assignments, Map<Long, UserDto> usersById) {
        List<Long> studentIds = assignments.stream()
                .map(ExamAssignment::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ExamAttempt> latestAttemptByStudentId;
        if (studentIds.isEmpty()) {
            latestAttemptByStudentId = new HashMap<>();
        } else {
            latestAttemptByStudentId = examAttemptRepository.findAllByExamIdAndStudentIdIn(examId, studentIds).stream()
                    .filter(a -> a.getStudentId() != null)
                    .collect(Collectors.toMap(
                            ExamAttempt::getStudentId,
                            a -> a,
                            (a, b) -> resolveAttemptTimestamp(a).isAfter(resolveAttemptTimestamp(b)) ? a : b
                    ));
        }
        return assignments.stream()
                .map(a -> toExamAssignmentResponse(a, usersById, latestAttemptByStudentId.get(a.getStudentId())))
                .toList();
    }

    @Override
    @Transactional
    public void assignStudents(Long examId, Long teacherId, AssignStudentsRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        List<Long> assignedStudentIds = new ArrayList<>();
        boolean handled = false;

        log.info("Assign students called for exam {}: emailsCount={}, idsCount={}",
                examId,
                request.getStudentEmails() == null ? 0 : request.getStudentEmails().size(),
                request.getStudentIds() == null ? 0 : request.getStudentIds().size());

        if (request.getStudentEmails() != null && !request.getStudentEmails().isEmpty()) {
            handled = true;
            for (String studentEmail : request.getStudentEmails()) {
                if (studentEmail == null || studentEmail.trim().isEmpty()) {
                    throw new IllegalArgumentException("Student email cannot be empty");
                }
                String sanitizedEmail = studentEmail.replaceAll("\\p{C}", "").trim();
                log.info("AssignStudents: resolving email id for original='{}', sanitized='{}'", studentEmail, sanitizedEmail);
                UserDto user = userServiceClient.getUserByEmail(sanitizedEmail);
                if (user == null || user.getId() == null) {
                    throw new UserNotFoundException(sanitizedEmail);
                }

                Long studentId = user.getId();
                if (user.getRole() != null && !Role.STUDENT.equals(user.getRole())) {
                    throw new IllegalArgumentException("User with email " + sanitizedEmail + " is not a student");
                }
                if (examAssignmentRepository.existsByExamIdAndStudentId(examId, studentId)) {
                    throw new StudentAlreadyAssignedException(examId, sanitizedEmail);
                }

                ExamAssignment assignment = new ExamAssignment();
                assignment.setExamId(examId);
                assignment.setStudentId(studentId);
                examAssignmentRepository.save(assignment);
                assignedStudentIds.add(studentId);
            }
        }

        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            handled = true;
            for (Long studentId : request.getStudentIds()) {
                if (studentId == null) {
                    throw new IllegalArgumentException("Student id cannot be null");
                }
                if (examAssignmentRepository.existsByExamIdAndStudentId(examId, studentId)) {
                    throw new StudentAlreadyAssignedException(examId, studentId);
                }
                ExamAssignment assignment = new ExamAssignment();
                assignment.setExamId(examId);
                assignment.setStudentId(studentId);
                examAssignmentRepository.save(assignment);
                assignedStudentIds.add(studentId);
            }
        }

        if (!handled) {
            throw new IllegalArgumentException("At least one of studentEmails or studentIds must be provided");
        }

        // Очищаем кэш для всех назначенных студентов
        for (Long studentId : assignedStudentIds) {
            evictStudentResultsCache(teacherId, studentId);
        }
    }

    @CacheEvict(value = "student-results", key = "#teacherId + ':' + #studentId")
    public void evictStudentResultsCache(Long teacherId, Long studentId) {
        log.debug("Evicting student-results cache for teacher: {}, student: {}", teacherId, studentId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "student-results", key = "#teacherId + ':' + #studentId")
    })
    public void removeStudentAssignment(Long examId, Long studentId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        ExamAssignment assignment = examAssignmentRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        examAssignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> getStudentsByTeacher(Long teacherId) {
        log.info("Getting students for teacher: {}", teacherId);

        List<Exam> teacherExams = examRepository.findByTeacherId(teacherId);

        if (teacherExams.isEmpty()) {
            return List.of();
        }

        List<Long> teacherExamIds = teacherExams.stream()
                .map(Exam::getId)
                .filter(Objects::nonNull)
                .toList();

        if (teacherExamIds.isEmpty()) {
            return List.of();
        }

        List<ExamAssignment> assignments = examAssignmentRepository.findAllByExamIdIn(teacherExamIds);

        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = assignments.stream()
                .map(ExamAssignment::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        List<UserDto> students = userServiceClient.getUsersByIds(studentIds);

        log.info("Found {} students for teacher {}", students.size(), teacherId);
        return students;
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = {"question-list", "exam-details"}, allEntries = true),
            @CacheEvict(value = "student-results", key = "'teacher:' + #teacherId", allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = {"question-list", "exam-details"}, allEntries = true),
            @CacheEvict(value = "student-results", key = "'teacher:' + #teacherId", allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = {"question-list", "exam-details"}, allEntries = true),
            @CacheEvict(value = "student-results", key = "'teacher:' + #teacherId", allEntries = true)
    })
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
    @Cacheable(value = "question-list", key = "#examId + ':teacher'")
    public List<QuestionResponse> getExamQuestionsForTeacher(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        ensureOwner(exam, teacherId);

        List<Question> questions = questionRepository.findAllByExamIdOrderByIdAsc(examId);
        if (questions.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = questions.stream().map(Question::getId).filter(Objects::nonNull).toList();
        Map<Long, List<QuestionOption>> optionsByQuestionId = questionOptionRepository
                .findAllByQuestionIdInOrderByQuestionIdAscOrderIndexAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        return questions.stream()
                .map(q -> toQuestionResponse(q, true, optionsByQuestionId.getOrDefault(q.getId(), List.of())))
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
        int questionCount = (int) questionRepository.countByExamId(exam.getId());
        int assignedCount = (int) examAssignmentRepository.countByExamId(exam.getId());
        return toExamResponse(exam, questionCount, assignedCount);
    }

    private ExamResponse toExamResponse(Exam exam, Integer questionCount, Integer assignedStudentCount) {
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
        r.setQuestionCount(questionCount);
        r.setAssignedStudentCount(assignedStudentCount);
        return r;
    }

    private ExamAssignmentResponse toExamAssignmentResponse(ExamAssignment assignment, Map<Long, UserDto> usersById) {
        return toExamAssignmentResponse(assignment, usersById, null);
    }

    private ExamAssignmentResponse toExamAssignmentResponse(ExamAssignment assignment, Map<Long, UserDto> usersById, ExamAttempt latestAttempt) {
        ExamAssignmentResponse r = new ExamAssignmentResponse();
        r.setId(assignment.getId());
        r.setExamId(assignment.getExamId());
        r.setStudentId(assignment.getStudentId());
        r.setAssignedAt(assignment.getAssignedAt());

        UserDto user = usersById.get(assignment.getStudentId());
        if (user != null) {
            r.setStudentName(user.getFirstName() + " " + user.getLastName());
            r.setStudentFirstName(user.getFirstName());
            r.setStudentLastName(user.getLastName());
            r.setStudentEmail(user.getEmail());
        }

        if (latestAttempt != null) {
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
        List<QuestionOption> opts = questionOptionRepository.findAllByQuestionIdOrderByOrderIndexAsc(q.getId());
        return toQuestionResponse(q, showCorrectAnswers, opts);
    }

    private QuestionResponse toQuestionResponse(Question q, boolean showCorrectAnswers, List<QuestionOption> opts) {
        QuestionResponse r = new QuestionResponse();
        r.setId(q.getId());
        r.setExamId(q.getExamId());
        r.setText(q.getText());
        r.setType(q.getType().name());
        r.setPoints(q.getPoints());

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
    @Cacheable(value = "student-results", key = "#teacherId + ':' + #studentId")
    public List<StudentExamResultResponse> getStudentResultsForTeacher(Long teacherId, Long studentId) {
        log.info("Getting results for teacher: {}, student: {}", teacherId, studentId);

        List<Exam> exams = examRepository.findAllByTeacherId(teacherId);
        if (exams.isEmpty()) {
            log.debug("No exams found for teacher {}", teacherId);
            return List.of();
        }

        List<Long> examIds = exams.stream().map(Exam::getId).filter(Objects::nonNull).toList();

        List<ExamAssignment> assignments = examAssignmentRepository.findAllByExamIdInAndStudentId(examIds, studentId);
        Map<Long, ExamAssignment> assignmentByExamId = assignments.stream()
                .collect(Collectors.toMap(ExamAssignment::getExamId, a -> a, (a, b) -> a));

        List<ExamAttempt> attempts = examAttemptRepository.findAllByExamIdInAndStudentId(examIds, studentId);
        Map<Long, List<ExamAttempt>> attemptsByExamId = attempts.stream()
                .collect(Collectors.groupingBy(ExamAttempt::getExamId));

        List<StudentExamResultResponse> results = new ArrayList<>();

        for (Exam exam : exams) {
            StudentExamResultResponse r = new StudentExamResultResponse();

            r.setExamId(exam.getId());
            r.setExamTitle(exam.getTitle());
            r.setExamStatus(exam.getStatus() != null ? exam.getStatus().name() : null);
            r.setPassingScore(exam.getPassingScore());

            ExamAssignment assignment = assignmentByExamId.get(exam.getId());
            if (assignment != null) {
                r.setAssigned(true);
                r.setAssignedAt(assignment.getAssignedAt());
            }

            List<ExamAttempt> examAttempts = attemptsByExamId.getOrDefault(exam.getId(), List.of());
            r.setAttemptsCount(examAttempts.size());

            if (!examAttempts.isEmpty()) {
                ExamAttempt last = examAttempts.stream()
                        .max(Comparator.comparing(ExamAttempt::getStartedAt))
                        .orElse(examAttempts.getFirst());

                r.setLastAttemptId(last.getId());
                r.setLastAttemptStatus(last.getStatus() != null ? last.getStatus().name() : null);
                r.setLastAttemptScore(last.getCalculatedScore());
                r.setLastAttemptFinishedAt(last.getFinishedAt());
            }

            results.add(r);
        }

        log.info("Returning {} results for teacher: {}, student: {}", results.size(), teacherId, studentId);
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
        r.setTotalQuestions((int) questionRepository.countByExamId(examId));
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