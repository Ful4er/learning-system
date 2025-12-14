# Learning System

Это учебная платформа в виде набора микросервисов — фронтенд на Vue 3 и набор Spring Boot сервисов, связанных через Eureka + Gateway и использующих MySQL.

Почему этот проект интересен:
- Чёткое разделение ответственности по микросервисам (аутентификация/профили, экзамены, маршрутизация).
- Простой и понятный домен экзаменов: экзамены, вопросы, варианты ответов, назначения и попытки.
- Некоторые архитектурные решения: JWT-авторизация, сервис-дискавери (Eureka), шлюз (Gateway), хранение выбранных опций ответа как JSON для гибкой структуры ответов.

Архитектура (упрощённо):

```
 [Frontend] <---> [API Gateway] <--> [user-service]
                                     [exam-service]
                                     [eureka-server]
                                     [mysql]
```

Ключевые сервисы и обязанности:
- `eureka-server` — сервис обнаружения (служит источником правды для адресов сервисов)
- `api-gateway` — единая точка входа, маршрутизация, фильтрация запросов
- `user-service` — регистрация/аутентификация, профили, роли
- `exam-service` — создание экзаменов, вопросы, назначение студентам, попытки и подсчёт баллов
- `frontend` — SPA на Vue 3 (Vite) с прокси на `/api` в разработке и nginx в production

Краткая схема БД (важные таблицы):

- `users` — id, first_name, last_name, email, password, role, created_at
- `user_profiles` — id, user_id (one-to-one), avatar_url, phone_number, date_of_birth
- `exams` — id, title, description, teacher_id, duration_minutes, passing_score, status, created_at
- `questions` — id, exam_id, text (TEXT), type (SINGLE_CHOICE/MULTIPLE_CHOICE/TEXT), points
- `question_options` — id, question_id, text (TEXT), is_correct, order_index
- `exam_assignments` — id, exam_id, student_id, assigned_at (unique on exam_id+student_id)
- `exam_attempts` — id, exam_id, student_id, started_at, finished_at, status, calculated_score
- `student_answers` — id, attempt_id, question_id, selected_option_ids (JSON), text_answer

Особенности реализации (интересные архитектурные решения):
- Аутентификация и авторизация реализованы на JWT: `user-service` генерирует токены (`JwtTokenUtil`), в них включены `userId` и `role` claims; сервисы валидируют токен через `spring-boot-starter-oauth2-resource-server` и `NimbusJwtDecoder` с секретом из `jwt.secret` / `JWT_SECRET`.
- Межсервисные вызовы: `exam-service` использует `UserServiceClient` для получения данных о пользователях (по email или id). Клиент извлекает текущий JWT из SecurityContext и пробрасывает его к `user-service` (через `app.services.user-service.url` — по умолчанию `http://api-gateway:8080`). Это упрощает доверенные запросы и сохранение контекста пользователя.
- Данные и ограничения: уникальность назначений гарантируется на уровне БД (`exam_assignments (exam_id, student_id)`), а ответы студентов (`student_answers`) используют JSON-поле `selected_option_ids` для поддержки как одиночного, так и множественного выбора.
- Сервисы авторизуют вызовы по ролям: `TEACHER` / `STUDENT`, эндпоинты сгруппированы по префиксам `/api/auth`, `/api/users`, `/api/teacher/*`, `/api/student/*`.

---

Как запустить

1) Docker (рекомендуется, единая команда — поднимает все сервисы и БД):

```bash
docker compose up --build
```

2) Локальная разработка отдельных сервисов:
- Backend (пример — `exam-service`):
    - Linux/macOS: `cd exam-service && ./mvnw spring-boot:run`
    - Windows: `cd exam-service && mvnw.cmd spring-boot:run`
- Frontend (dev server): `cd frontend && npm install && npm run dev`

3) Проверки после запуска:
- Eureka UI: http://localhost:8761
- API Gateway health: http://localhost:8080/actuator/health
- Frontend (в Docker): http://localhost:3000

Советы:
- Перед поднятием через Docker можно подставить свои секреты/параметры в .env.
- Для разработки удобно использовать `VITE_API_BASE_URL=/api` (frontend) и `JWT_SECRET` с простым значением, но для тестов/продакшена замените на надёжный секрет.