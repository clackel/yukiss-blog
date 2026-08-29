# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Full-stack blog / community app. Two independent projects in one repo:

- `server/` — Spring Boot 4 (Java 21), MyBatis, MySQL, JWT (`com.auth0:java-jwt`), Lombok. Package root `moon.yukiss`.
- `web/` — Vue 3 (`<script setup>`), Vite, Vue Router, Element Plus, Axios, markdown-it + DOMPurify.

Project docs are in Chinese: `README.md` (overview, quick start), `docs/API.md` (authoritative HTTP API reference — keep it in sync when changing endpoints or response shapes), `docs/DEVELOPMENT.md` (config table, architecture, conventions, build/release). Update `docs/DEVELOPMENT.md` too when changing config keys or architecture. UI strings and error messages are in Chinese.

## Commands

`start.bat` launches both (backend on :4000 via `mvn clean spring-boot:run`, frontend on 127.0.0.1:3000 via `npm run dev`). There is no `mvnw` script; use a system `mvn`.

Backend (run in `server/`):

```bash
mvn spring-boot:run
mvn test
mvn test -Dtest=ArticleServiceImplTest
mvn test -Dtest=ArticleServiceImplTest#methodName
```

Frontend (run in `web/`):

```bash
npm run dev
npm test
npx vitest run src/utils/markdown.test.js
npm run build
```

Vitest is configured inside `vite.config.js` (jsdom, globals). Tests live next to the source as `*.test.js`. No linter is configured for either project.

## Configuration

All backend settings in `server/src/main/resources/application.yml` are overridable by `BLOG_*` env vars (see `server/.env.example`): DB URL/credentials (MySQL `my_blog_db`, auto-created), JWT secret/expiry, CORS origins, upload dir (defaults to `<repo>/uploads`, served at `/uploads/**`), and `BLOG_EMAIL_DEV_RETURN_CODE` — when true, email verification codes are returned in the API response instead of being sent. Frontend reads `VITE_API_BASE_URL` (default `http://localhost:4000`).

## Architecture

### Backend

Layered: `controller` → `service` (interface) → `service/impl` → `mapper` (MyBatis interfaces; complex SQL in `resources/mapper/*.xml`, simple ones via annotations). `map-underscore-to-camel-case` is on.

- **Schema is created in code, not by migration files.** `config/BlogSchemaInitializer` and `config/UserSchemaInitializer` are `ApplicationRunner`s that run `CREATE TABLE IF NOT EXISTS` plus idempotent `addColumnIfMissing` / `addIndexIfMissing` upgrades on startup. Schema changes go there. They can be disabled with `app.schema-initializer.enabled=false` (the context-load test does this).
- **Auth**: `interceptors/LoginInterceptor` guards every path except those excluded in `config/WebConfig` (login/register/recover, uploads, static SPA assets). The JWT is sent raw in the `Authorization` header — **no `Bearer` prefix**. Parsed claims are stored per-request in `utils/ThreadLocalUtil` and cleared in `afterCompletion`; services read the current user from there. Certain GET endpoints (`isPublicRead`: article list/page, comment list, etc.) are public but still parse an optional token so fields like `likedByMe` reflect the viewer.
- **Responses/errors**: every endpoint returns `common/ApiResponse` (`{success, message, data}`); paged lists use `PageResult`. Throw `common/BusinessException` with an HTTP status for expected failures; `GlobalExceptionHandler` maps it and validation errors to the proper status code.
- `src/main/resources/static/` contains a prebuilt copy of the frontend so the backend can serve the SPA directly; it is not regenerated automatically by the build.

### Frontend

- `utils/request.js` is the shared Axios instance: attaches the token from `localStorage`, shows `ElMessage` errors globally, and on 401 clears the session and dispatches a `yukiss:auth-expired` window event. Use `apiData(response)` to unwrap the `ApiResponse` envelope (throws when `success` is false).
- `composables/useUser.js` holds module-level singleton state (token, user, auth dialog visibility/mode, post-login redirect) shared across all components; it listens for `yukiss:auth-expired`.
- `router/index.js`: routes with `meta.requiresAuth` redirect to `/?login=1&redirect=...` when no token exists; the landing page opens the auth dialog from that query.
- Markdown rendering goes through `utils/markdown.js` (markdown-it + DOMPurify sanitization) and `components/MarkdownContent.vue`; `utils/media.js` normalizes upload/avatar URLs against the API base.

### Tests

Backend tests are plain JUnit 5 + Mockito unit tests that construct `*ServiceImpl` with mocked mappers and set the current user via `ThreadLocalUtil.set(...)` (cleared in `@AfterEach`); they don't need a database except `BlogBackendApplicationTests`.
