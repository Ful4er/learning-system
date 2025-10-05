package org.webproject.examservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webproject.examservice.dto.request.AddQuestionRequest;
import org.webproject.examservice.dto.request.AssignStudentsRequest;
import org.webproject.examservice.dto.request.CreateExamRequest;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.ExamAssignment;
import org.webproject.examservice.model.Question;
import org.webproject.examservice.model.QuestionOption;
import org.webproject.examservice.repository.ExamAssignmentRepository;
import org.webproject.examservice.repository.ExamRepository;
import org.webproject.examservice.repository.QuestionOptionRepository;
import org.webproject.examservice.repository.QuestionRepository;
import org.webproject.examservice.service.ExamService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

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
    @Transactional
    public ExamResponse publishExam(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        ensureOwner(exam, teacherId);
        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        return toExamResponse(examRepository.save(exam));
    }

    @Override
    @Transactional
    public void assignStudents(Long examId, Long teacherId, AssignStudentsRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        ensureOwner(exam, teacherId);
        for (Long studentId : request.getStudentIds()) {
            boolean exists = examAssignmentRepository.findByExamIdAndStudentId(examId, studentId).isPresent();
            if (!exists) {
                ExamAssignment ea = new ExamAssignment();
                ea.setExamId(examId);
                ea.setStudentId(studentId);
                examAssignmentRepository.save(ea);
            }
        }
    }

    @Override
    @Transactional
    public QuestionResponse addQuestion(Long examId, Long teacherId, AddQuestionRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        ensureOwner(exam, teacherId);

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

        return toQuestionResponse(savedQuestion);
    }

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
    public ExamResponse getExamDetails(Long examId, Long requesterId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        return toExamResponse(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getExamQuestions(Long examId, Long requesterId) {
        return questionRepository.findAllByExamIdOrderByIdAsc(examId).stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());
    }

    private void ensureOwner(Exam exam, Long teacherId) {
        if (!Objects.equals(exam.getTeacherId(), teacherId)) {
            throw new IllegalArgumentException("Forbidden: not exam owner");
        }
    }

    private ExamResponse toExamResponse(Exam exam) {
        ExamResponse r = new ExamResponse();
        r.setId(exam.getId());
        r.setTitle(exam.getTitle());
        r.setDescription(exam.getDescription());
        r.setTeacherId(exam.getTeacherId());
        r.setDurationMinutes(exam.getDurationMinutes());
        r.setPassingScore(exam.getPassingScore());
        r.setStatus(exam.getStatus().name());
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
}


