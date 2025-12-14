# Exam Service

Назначение: всё, что связано с экзаменами — создание/редактирование, управление вопросами, назначение студентам, хранение попыток и ответов, подсчёт результатов.

Порт: 8082 (по умолчанию)

API (основные префиксы):
- `GET /api/teacher/exams` — список преподавателя
- `POST /api/teacher/exams` — создать экзамен
- `GET /api/teacher/exams/{examId}/questions` — вопросы экзамена
- `POST /api/student/exams/attempts` — начать попытку (студент)
- `POST /api/student/exams/attempts/{attemptId}/answers` — отправить ответ

Ключевые таблицы и форматы данных:
- `exams` — базовая информация; поля: `title`, `teacher_id`, `duration_minutes`, `passing_score`, `status`
- `questions` — `text` (TEXT), `type` (SINGLE/MULTI/TEXT), `points`
- `question_options` — варианты с флагом `is_correct`
- `exam_assignments` — уникальная пара `(exam_id, student_id)`
- `exam_attempts` и `student_answers` — для хранения попыток и ответов; `student_answers.selected_option_ids` — JSON

Особенности и полезные примечания:
Особенности и полезные примечания:
- Роли: эндпоинты `/api/teacher/**` и `/api/student/**` защищены и требуют соответствующих ролей.
- `exam-service` использует `UserServiceClient` для разрешения пользователей по email/id. Клиент автоматически извлекает JWT из SecurityContext и пробрасывает его в заголовке при вызове `user-service`, что упрощает доверенную коммуникацию между сервисами.
- Валидация бизнес-логики (публикация, архивация, изменение состояния экзамена) реализована в сервисном слое.
- При расширении формата ответов удобно использовать JSON-поля — уже используется в `student_answers`.
