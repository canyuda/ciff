# Repository Guidelines

## Project Structure & Module Organization
This repository is a multi-module Maven + Vue monorepo.

- Backend modules: `ciff-common` (shared DTO/config), `ciff-provider`, `ciff-mcp`, `ciff-knowledge`, `ciff-agent`, `ciff-workflow`, `ciff-chat`, and `ciff-app` (Spring Boot entrypoint).
- Frontend: `ciff-web` (Vue 3 + TypeScript + Vite).
- Standards/docs: `rules/` (engineering and architecture rules), `docs/` (database and architecture docs).
- Typical Java layout: `src/main/java`, `src/main/resources`, `src/test/java`.

Respect one-way dependencies: `common -> provider/mcp -> knowledge -> agent -> workflow -> chat -> app`.
Cross-module calls must go through `facade` interfaces, not direct service/mapper injection.

## Build, Test, and Development Commands
- `make start`: start local backend + frontend using `start.sh`.
- `make stop`: stop local services via `stop.sh`.
- `make build`: build backend (`mvn package -pl ciff-app -am -DskipTests`) and frontend (`npm run build`).
- `make clean`: clean Maven artifacts and frontend `dist/`.
- `make package`: produce `ciff.tar.gz` with JAR + frontend bundle.
- `cd ciff-web && npm run dev`: run Vite dev server (port `3000`).
- `cd ciff-web && npm run test`: run frontend tests with Vitest.

## Coding Style & Naming Conventions
- Java: JDK 17, UTF-8, Spring Boot 3.3.x, constructor injection (`@RequiredArgsConstructor`) only.
- Logging: use SLF4J placeholders (`log.info("id={}", id)`), no string concatenation.
- Avoid hardcoded config; externalize in `application.yml`.
- Naming examples:
  - `AgentFacade`, `AgentFacadeImpl`, `AgentController`, `AgentDTO`, `AgentVO`, `AgentConvertor`, `AgentMapper`, `AgentPO`.
- REST base pattern: `/api/v1/{resources}`; controllers return `Result<T>`.

## Testing Guidelines
- Backend: JUnit 5 + Spring Boot Test + Mockito + MockMvc.
- Frontend: Vitest.
- Test class naming: `{Target}Test` (e.g., `AgentServiceTest`).
- Test method naming: `{method}_{scenario}_{expected}`.
- Prefer focused unit/slice tests (`@WebMvcTest`, Mockito); avoid loading full context unless required.

## Commit & Pull Request Guidelines
Use Conventional Commits as seen in history: `feat: ...`, `fix: ...`, `refactor: ...`, optionally scoped (`feat(web): ...`).

For PRs, include:
- Clear summary and affected modules (e.g., `ciff-provider`, `ciff-web`).
- Linked issue/task and migration/config notes (if any).
- API changes with example request/response.
- UI changes with screenshots or short GIFs.
- Test evidence (commands run and results).

## Security & Configuration Tips
- Never commit secrets; keep API keys in environment variables or local config overlays.
- Follow timeout, retry, and logging desensitization rules in `rules/02-llm-calling.md` and `rules/05-engineering.md`.
