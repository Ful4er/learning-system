CREATE TABLE exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    teacher_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    duration_minutes INTEGER,
    passing_score INTEGER,
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT'
);

CREATE TABLE exam_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (exam_id, student_id),
    CONSTRAINT fk_exam_assignments_exam
      FOREIGN KEY (exam_id)
          REFERENCES exams(id)
          ON DELETE CASCADE
);

CREATE TABLE exam_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    status ENUM('IN_PROGRESS', 'FINISHED', 'TIMED_OUT') NOT NULL DEFAULT 'IN_PROGRESS',
    calculated_score DOUBLE,

    CONSTRAINT fk_exam_attempts_exam
       FOREIGN KEY (exam_id)
           REFERENCES exams(id)
           ON DELETE CASCADE
);

CREATE TABLE questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    type ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TEXT') NOT NULL,
    points INTEGER DEFAULT 1,

    CONSTRAINT fk_questions_exam
       FOREIGN KEY (exam_id)
           REFERENCES exams(id)
           ON DELETE CASCADE
);

CREATE TABLE question_options (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    is_correct BOOLEAN DEFAULT false,
    order_index INTEGER,

    CONSTRAINT fk_question_options_question
      FOREIGN KEY (question_id)
          REFERENCES questions(id)
          ON DELETE CASCADE
);

CREATE TABLE student_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_ids JSON,
    text_answer TEXT,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_student_answers_attempt
     FOREIGN KEY (attempt_id)
         REFERENCES exam_attempts(id)
         ON DELETE CASCADE,
    CONSTRAINT fk_student_answers_question
     FOREIGN KEY (question_id)
         REFERENCES questions(id)
         ON DELETE CASCADE
);

CREATE INDEX idx_exams_teacher_id ON exams(teacher_id);
CREATE INDEX idx_exams_status ON exams(status);
CREATE INDEX idx_exam_assignments_exam_id ON exam_assignments(exam_id);
CREATE INDEX idx_exam_assignments_student_id ON exam_assignments(student_id);
CREATE INDEX idx_exam_attempts_exam_id ON exam_attempts(exam_id);
CREATE INDEX idx_exam_attempts_student_id ON exam_attempts(student_id);
CREATE INDEX idx_exam_attempts_status ON exam_attempts(status);
CREATE INDEX idx_questions_exam_id ON questions(exam_id);
CREATE INDEX idx_question_options_question_id ON question_options(question_id);
CREATE INDEX idx_student_answers_attempt_id ON student_answers(attempt_id);
CREATE INDEX idx_student_answers_question_id ON student_answers(question_id);