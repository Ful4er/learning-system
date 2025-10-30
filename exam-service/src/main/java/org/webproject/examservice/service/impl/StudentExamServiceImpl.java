package org.webproject.examservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webproject.examservice.dto.request.StartExamAttemptRequest;
import org.webproject.examservice.dto.request.SubmitAnswerRequest;
import org.webproject.examservice.dto.response.ExamAttemptResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;
import org.webproject.examservice.exception.ExamNotFoundException;
import org.webproject.examservice.exception.InvalidExamStateException;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.ExamAssignment;
import org.webproject.examservice.model.ExamAttempt;
import org.webproject.examservice.model.Question;
import org.webproject.examservice.model.QuestionOption;
import org.webproject.examservice.model.StudentAnswer;
import org.webproject.examservice.repository.ExamAssignmentRepository;
import org.webproject.examservice.repository.ExamAttemptRepository;
import org.webproject.examservice.repository.ExamRepository;
import org.webproject.examservice.repository.QuestionOptionRepository;
import org.webproject.examservice.repository.QuestionRepository;
import org.webproject.examservice.repository.StudentAnswerRepository;
import org.webproject.examservice.service.StudentExamService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentExamServiceImpl implements StudentExamService {

    private final ExamRepository examRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExamResponse> getAssignedExamsForStudent(Long studentId) {
        List<ExamAssignment> assignments = examAssignmentRepository.findAllByStudentId(studentId);
        List<Long> examIds = assignments.stream().map(ExamAssignment::getExamId).collect(Collectors.toList());
        return examRepository.findAllById(examIds).stream()
                .filter(e -> e.getStatus() == Exam.ExamStatus.PUBLISHED)
                .map(this::toExamResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getExamDetails(Long examId, Long studentId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        return toExamResponse(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getExamQuestions(Long examId, Long studentId) {
        return questionRepository.findAllByExamIdOrderByIdAsc(examId).stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());
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

    private QuestionResponse toQuestionResponse(Question q) {
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
            return ro;
        }).collect(Collectors.toList());
        r.setOptions(mapped);
        return r;
    }

    @Override
    @Transactional
    public ExamAttemptResponse startExamAttempt(Long studentId, StartExamAttemptRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(request.getExamId()));
        
        if (exam.getStatus() != Exam.ExamStatus.PUBLISHED) {
            throw new InvalidExamStateException("Exam is not published");
        }

        boolean isAssigned = examAssignmentRepository.findByExamIdAndStudentId(request.getExamId(), studentId)
                .isPresent();
        if (!isAssigned) {
            throw new IllegalArgumentException("Student is not assigned to this exam");
        }

        List<ExamAttempt> existingAttempts = examAttemptRepository.findAllByExamIdAndStudentId(request.getExamId(), studentId);
        Optional<ExamAttempt> activeAttempt = existingAttempts.stream()
                .filter(attempt -> attempt.getStatus() == ExamAttempt.AttemptStatus.IN_PROGRESS)
                .findFirst();
        
        if (activeAttempt.isPresent()) {
            throw new IllegalArgumentException("Student already has an active attempt for this exam");
        }
        
        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamId(request.getExamId());
        attempt.setStudentId(studentId);
        attempt.setStatus(ExamAttempt.AttemptStatus.IN_PROGRESS);
        
        ExamAttempt saved = examAttemptRepository.save(attempt);
        return toExamAttemptResponse(saved);
    }

    @Override
    @Transactional
    public ExamAttemptResponse finishExamAttempt(Long studentId, Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        
        if (!attempt.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Attempt does not belong to this student");
        }
        
        if (attempt.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS) {
            throw new InvalidExamStateException("Attempt is not in progress");
        }
        
        attempt.setStatus(ExamAttempt.AttemptStatus.FINISHED);
        attempt.setFinishedAt(Instant.now());

        double score = calculateScore(attemptId);
        attempt.setCalculatedScore(score);
        
        ExamAttempt saved = examAttemptRepository.save(attempt);
        return toExamAttemptResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamAttemptResponse getCurrentAttempt(Long studentId, Long examId) {
        List<ExamAttempt> attempts = examAttemptRepository.findAllByExamIdAndStudentId(examId, studentId);
        Optional<ExamAttempt> activeAttempt = attempts.stream()
                .filter(attempt -> attempt.getStatus() == ExamAttempt.AttemptStatus.IN_PROGRESS)
                .findFirst();
        
        if (activeAttempt.isEmpty()) {
            throw new IllegalArgumentException("No active attempt found");
        }
        
        return toExamAttemptResponse(activeAttempt.get());
    }

    @Override
    @Transactional
    public void submitAnswer(Long studentId, Long attemptId, SubmitAnswerRequest request) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        
        if (!attempt.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Attempt does not belong to this student");
        }
        
        if (attempt.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS) {
            throw new InvalidExamStateException("Attempt is not in progress");
        }
        
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        
        if (!question.getExamId().equals(attempt.getExamId())) {
            throw new IllegalArgumentException("Question does not belong to this exam");
        }

        List<StudentAnswer> existingAnswers = studentAnswerRepository.findAllByAttemptId(attemptId);
        Optional<StudentAnswer> existingAnswer = existingAnswers.stream()
                .filter(answer -> answer.getQuestionId().equals(request.getQuestionId()))
                .findFirst();
        
        StudentAnswer answer;
        if (existingAnswer.isPresent()) {
            answer = existingAnswer.get();
        } else {
            answer = new StudentAnswer();
            answer.setAttemptId(attemptId);
            answer.setQuestionId(request.getQuestionId());
        }
        
        if (request.getSelectedOptionIds() != null && !request.getSelectedOptionIds().isEmpty()) {
            answer.setSelectedOptionIds(String.join(",", request.getSelectedOptionIds().toString()));
        }
        if (request.getTextAnswer() != null) {
            answer.setTextAnswer(request.getTextAnswer());
        }
        
        studentAnswerRepository.save(answer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamAttemptResponse> getStudentAttempts(Long studentId, Long examId) {
        List<ExamAttempt> attempts = examAttemptRepository.findAllByExamIdAndStudentId(examId, studentId);
        return attempts.stream()
                .map(this::toExamAttemptResponse)
                .collect(Collectors.toList());
    }

    private double calculateScore(Long attemptId) {
        List<StudentAnswer> answers = studentAnswerRepository.findAllByAttemptId(attemptId);
        double totalScore = 0.0;
        double maxScore = 0.0;
        
        for (StudentAnswer answer : answers) {
            Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
            if (question == null) continue;
            
            maxScore += question.getPoints();
            
            if (question.getType() == Question.QuestionType.TEXT) {
                if (answer.getTextAnswer() != null && !answer.getTextAnswer().trim().isEmpty()) {
                    totalScore += question.getPoints() * 0.5;
                }
            } else {
                List<QuestionOption> correctOptions = questionOptionRepository.findAllByQuestionIdOrderByOrderIndexAsc(question.getId())
                        .stream()
                        .filter(QuestionOption::getIsCorrect)
                        .toList();
                
                if (answer.getSelectedOptionIds() != null && !answer.getSelectedOptionIds().isEmpty()) {
                    String[] selectedIds = answer.getSelectedOptionIds().replaceAll("[\\[\\]]", "").split(",");
                    long correctSelected = 0;
                    long totalCorrect = correctOptions.size();
                    
                    for (String idStr : selectedIds) {
                        try {
                            Long selectedId = Long.parseLong(idStr.trim());
                            if (correctOptions.stream().anyMatch(opt -> opt.getId().equals(selectedId))) {
                                correctSelected++;
                            }
                        } catch (NumberFormatException ignored) {
                            // Skip invalid IDs
                        }
                    }
                    
                    if (totalCorrect > 0) {
                        totalScore += question.getPoints() * (double) correctSelected / totalCorrect;
                    }
                }
            }
        }
        
        return maxScore > 0 ? (totalScore / maxScore) * 100 : 0.0;
    }

    private ExamAttemptResponse toExamAttemptResponse(ExamAttempt attempt) {
        ExamAttemptResponse response = new ExamAttemptResponse();
        response.setId(attempt.getId());
        response.setExamId(attempt.getExamId());
        response.setStudentId(attempt.getStudentId());
        response.setStartedAt(attempt.getStartedAt());
        response.setFinishedAt(attempt.getFinishedAt());
        response.setStatus(attempt.getStatus().name());
        response.setCalculatedScore(attempt.getCalculatedScore());
        return response;
    }
}


