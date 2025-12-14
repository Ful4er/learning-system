# API Gateway

Роль: централизованный вход в систему — маршрутизирует запросы к сервисам по правилам, подтягиваемым из Eureka. Также концентрирует cross-cutting concerns (rate-limiting, фильтры, auth-прокси).

Порт: 8080 (по умолчанию)

Особенности и примечания:
- Discovery-based routing: правила в `src/main/resources/application.yml` под ключом `spring.cloud.gateway.routes`.
- Gateway проксирует префиксы `/api/users/**`, `/api/auth/**`, `/api/admin/**` на `user-service` и `/api/exams/**`, `/api/teacher/**`, `/api/student/**` на `exam-service`.
- Health и Actuator включены — используйте `/actuator/health` для проверок состояния.

Где смотреть:
- Конфигурация маршрутов: `src/main/resources/application.yml` и `application-docker.yml`.
- Место для расширения: фильтры и глобальные пред- и пост- обработчики находятся в `src/main/java`.

Переменные окружения (упоминание): `EUREKA_SERVER_URL`, `SPRING_PROFILES_ACTIVE`.
