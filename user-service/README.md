# User Service

Назначение: регистрация/аутентификация пользователей, управление профилями и выдача ролей (TEACHER/STUDENT/ADMIN).

Порт: 8081 (по умолчанию)

Основные API-префиксы:
- `POST /api/auth/login` — логин
- `POST /api/auth/register` — регистрация
- `GET /api/users/me` — инфо о текущем пользователе
- `GET /api/users/search?email=` — поиск пользователя по email

Структура БД (важные таблицы):
- `users` — `id`, `first_name`, `last_name`, `email` (unique), `password`, `role`, `created_at`
- `user_profiles` — `user_id` (one-to-one), `avatar_url`, `phone_number`, `date_of_birth`

Особенности и примечания:
Особенности и примечания:
- Пароли хранятся в колонке `password` и не возвращаются в ответах API (аннотация `@JsonIgnore`).
- JWT: `JwtTokenUtil` генерирует токены с `userId` и `role` в качестве claim'ов; секрет для подписи читается из `jwt.secret` / переменной окружения `JWT_SECRET`. Токен используется другими сервисами (gateway и resource servers) для проверки и получения контекста пользователя.
- Security: фильтр JWT пропускает `/api/auth/*`, остальные эндпоинты защищены по ролям.
