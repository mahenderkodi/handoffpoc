# Employee Management POC — Implementation Instructions

> **Audience:** This document is written for an AI coding agent (or a developer) to read once and implement the entire application from. It contains **no source code** — only requirements, structure, and contracts. Write all code yourself based on these instructions, following standard best practices for each named technology.

---

## 1. Objective

Build a small, clean, production-quality **Employee Management POC**:

- Backend: **Java 21 + Spring Boot 3.x + Spring Data JPA + MySQL 8**
- Frontend: **Angular 22 + TypeScript (strict) + Standalone Components + Reactive Forms**
- One domain entity: `Employee`
- Five capabilities only: list/search, view details, create, edit, delete

Keep the implementation as simple as possible while still following clean architecture and SOLID principles. Do not add anything beyond what is listed here unless explicitly instructed.

---

## 2. Scope

### In scope (implement all of these)
1. View employees (paginated list)
2. Search employees by name or email
3. View a single employee's details
4. Create an employee
5. Edit an employee
6. Delete an employee (with confirmation on the frontend)

### Out of scope (do NOT implement)
- Authentication / authorization / login of any kind
- Payroll, attendance, leave management, notifications
- Microservices, message queues, caching layers, Kubernetes
- Event sourcing / CQRS
- Any entity other than `Employee`
- Any database migration tool (see Section 6 — explicitly not using Flyway/Liquibase)

If you are ever tempted to add something not listed above, don't — flag it as a "future enhancement" in your own implementation notes instead of building it.

---

## 3. Technology Stack (must use exactly this)

| Concern | Choice |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 3.x (latest stable) |
| Backend build tool | Maven |
| Web layer | Spring Web (REST controllers) |
| Persistence | Spring Data JPA + Hibernate |
| Database | **MySQL 8** (not PostgreSQL) |
| DB driver | `mysql-connector-j` |
| Validation | Jakarta Bean Validation |
| Schema management | **Spring Boot's built-in SQL init (`schema.sql`) — do NOT use Flyway or Liquibase** |
| Frontend framework | Angular 22, standalone components only (no NgModules) |
| Frontend language | TypeScript, strict mode |
| Frontend forms | Reactive Forms |
| Frontend HTTP | `HttpClient` with a functional interceptor for error handling |
| Containerization | Docker + Docker Compose (local dev only) |
| Backend tests | JUnit 5, Mockito, Spring Boot Test |
| Frontend tests | Angular's default test setup (Jasmine/Karma) |

---

## 4. Data Model

Single entity: **Employee**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `id` | numeric identifier | generated | primary key, auto-increment |
| `employeeCode` | string | yes | max 20 chars, uppercase letters/digits/dashes only, unique, **immutable after creation** |
| `firstName` | string | yes | max 100 chars |
| `lastName` | string | yes | max 100 chars |
| `email` | string | yes | valid email format, max 150 chars, unique |
| `phone` | string | no | if present, must match a reasonable phone pattern (7–20 chars, digits/spaces/dashes/leading `+`) |
| `department` | string | yes | max 100 chars |
| `jobTitle` | string | yes | max 100 chars |
| `dateOfJoining` | date | yes | must not be in the future |
| `status` | enum | yes | one of `ACTIVE`, `INACTIVE`; default `ACTIVE` |
| `createdAt` | timestamp | system-set | set once on creation, never changed afterward |
| `updatedAt` | timestamp | system-set | updated every time the row is modified |

Apply every validation rule above **both** in the backend (Bean Validation) and in the frontend (Angular reactive form validators). The backend is the real boundary; the frontend is for immediate user feedback.

---

## 5. Backend Requirements

### 5.1 Structure

Use **package-by-layer**, not package-by-feature, because there is only one entity in this POC — a feature-based split would just add a redundant folder with no separation benefit. Organize the backend under a single base package (e.g. `com.example.employee`) with these layers:

- `config` — cross-cutting configuration (CORS restricted to the frontend's origin; nothing else unless a later instruction says so)
- `entity` — the JPA entity and the status enum
- `dto` — request DTO, response DTO, and the API error DTO — never expose the entity directly over REST
- `mapper` — a small, plain mapping utility between entity and DTOs (do not use a mapping library or code generator; there is only one entity, so a hand-written mapper is simpler and more transparent)
- `repository` — one Spring Data JPA repository interface for `Employee` (do not create a generic base repository — `JpaRepository` already provides everything needed)
- `service` — one service interface plus one implementation, containing all business rules (uniqueness checks, delegating persistence, mapping to response DTOs)
- `controller` — one REST controller exposing the endpoints in Section 7
- `exception` — two domain exceptions (`EmployeeNotFoundException`, `DuplicateEmployeeException`) plus one global exception handler that converts all exceptions into the standard error format in Section 9

### 5.2 Business rules the service layer must enforce
- Reject creation if `email` already exists (checked before insert).
- Reject creation if `employeeCode` already exists (checked before insert).
- Reject update if the new `email` belongs to a different existing employee.
- Never allow `employeeCode` to change on update, even if the client sends a different value — silently keep the original.
- Set `createdAt`/`updatedAt` automatically; never accept these from the client.

### 5.3 What to avoid
- No generic/abstract repository or service base classes.
- No interface for the mapper or the exceptions — only the service gets an interface (it's the one layer where a test seam and conventional DI genuinely help).
- No premature abstractions "for future flexibility" — build exactly what Section 2 lists.

---

## 6. Database Requirements (MySQL — no migration tool)

- Single table: `employee`, InnoDB engine, `utf8mb4` charset.
- Define the columns exactly as listed in Section 4's data model, using appropriate MySQL types (e.g. `VARCHAR` with the stated lengths, `DATE` for date of joining, `TIMESTAMP` for the audit columns).
- Store `status` as a `VARCHAR` with a `CHECK` constraint restricting it to `ACTIVE`/`INACTIVE` — do **not** use MySQL's native `ENUM` column type (it's harder to change later and maps awkwardly to Hibernate).
- Add named unique constraints on `employeeCode` and `email` (not just inline `UNIQUE`), so a database-level violation can be mapped back to a specific field if it ever slips past the service-layer check.
- Add indexes to support: filtering by `department`, filtering by `status`, and the name-based search described in Section 7.2.
- `updatedAt` should auto-update at the database level on every row update, as a backstop behind the application-level update logic.

### Schema management — explicitly no Flyway or Liquibase
- Put the full table definition in a single `schema.sql` file inside the backend's resources, under a `db` subfolder.
- Configure Spring Boot to run this file automatically on startup, and configure Hibernate to only **validate** the schema against the entity mapping — never to auto-generate or alter it.
- For the Dockerized database, also wire this same `schema.sql` file into MySQL's own container-init mechanism, so a fresh container is ready immediately with no manual step.
- Document clearly (in code comments or the README) that this approach has no migration history and is not safe to re-run against a database that already has diverging data — that is an accepted, explicit trade-off for this POC, not an oversight.

---

## 7. API Contract

Base path: `/api/employees`

| Method | Path | Purpose | Success status |
|---|---|---|---|
| GET | `/api/employees` | List/search employees, paginated | 200 |
| GET | `/api/employees/{id}` | Get one employee | 200 |
| POST | `/api/employees` | Create an employee | 201, with a `Location` header pointing to the new resource |
| PUT | `/api/employees/{id}` | Update an employee | 200 |
| DELETE | `/api/employees/{id}` | Delete an employee | 204, no body |

### 7.1 Pagination
Implement pagination now, not later — it costs almost nothing given Spring Data's built-in paging support, and prevents the list endpoint from ever returning an unbounded result set. Accept standard `page` and `size` query parameters, default page size 20, default sort by last name. Return a paged envelope containing: the list of items, total element count, total page count, current page index, and page size.

### 7.2 Search
Accept an optional `search` query parameter on the list endpoint. When present, match it case-insensitively against first name, last name, and email (partial match). When absent, return all employees (paginated as normal).

### 7.3 Example payloads (for reference only — implement the logic yourself)

Create request body:
```json
{
  "employeeCode": "EMP-1042",
  "firstName": "Asha",
  "lastName": "Rao",
  "email": "asha.rao@example.com",
  "phone": "+91-98765-43210",
  "department": "Engineering",
  "jobTitle": "Backend Developer",
  "dateOfJoining": "2024-03-01",
  "status": "ACTIVE"
}
```

Successful response body (create/update/get):
```json
{
  "id": 17,
  "employeeCode": "EMP-1042",
  "firstName": "Asha",
  "lastName": "Rao",
  "email": "asha.rao@example.com",
  "phone": "+91-98765-43210",
  "department": "Engineering",
  "jobTitle": "Backend Developer",
  "dateOfJoining": "2024-03-01",
  "status": "ACTIVE",
  "createdAt": "2026-09-11T10:15:30",
  "updatedAt": "2026-09-11T10:15:30"
}
```

---

## 8. Frontend Requirements

### 8.1 Structure

```
src/app/
  core/            -> HTTP interceptor for error handling, app-wide bootstrap config
  shared/          -> reusable presentational components: loading spinner, empty state, confirm dialog
  features/employees/
    models/        -> TypeScript interfaces for Employee and paged response
    services/      -> one Angular service wrapping all HTTP calls to the API
    pages/         -> employee-list, employee-detail, employee-form (one route each)
```

Do not create a separate `components/` folder inside `features/employees/` unless an employee-specific reusable widget is actually needed — with only three pages and no such widget yet, it would sit empty.

### 8.2 Screens

**Employee List**
- Table columns: employee code, name, email, department, job title, status, actions.
- Row actions: View, Edit, Delete.
- A debounced search box wired to the backend `search` parameter.
- A loading indicator while the request is in flight.
- An empty state when there are zero results (distinguish "no employees at all" from "no results for this search").
- An error state with a retry option if the request fails.
- Delete requires a confirmation dialog before the DELETE request is sent.
- Basic pagination controls tied to the backend's paged response.

**Employee Create/Edit**
- One reactive form used for both create and edit (edit pre-fills from the detail endpoint).
- Client-side validation matching every rule in Section 4, shown inline per field.
- The employee code field is disabled/read-only in edit mode.
- On a failed submission due to a server-side validation or duplicate error, map the returned field errors (Section 9) back onto the corresponding form controls.

**Employee Details**
- Read-only display of every field, plus links to Edit and back to the list.

### 8.3 Cross-cutting
- Add one functional HTTP interceptor that catches and logs API errors centrally, then re-throws so individual components can still react to specific error content (e.g., field-level messages).
- Keep all API error/field-mapping logic in one place (the service or a small shared helper) rather than duplicating it across pages.

---

## 9. Error Handling Contract

Every error response from the backend must follow this exact shape:

```json
{
  "timestamp": "2026-09-11T10:20:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/employees",
  "errors": [
    { "field": "email", "message": "Invalid email address" }
  ]
}
```

| Situation | HTTP status | `error` value |
|---|---|---|
| Request fails Bean Validation | 400 | `VALIDATION_ERROR` |
| Requested employee does not exist | 404 | `RESOURCE_NOT_FOUND` |
| Duplicate email or employee code on create/update | 409 | `DUPLICATE_RESOURCE` |
| A database constraint is violated in a way not already caught above | 409 | `DATA_INTEGRITY_VIOLATION` |
| Anything unexpected | 500 | `INTERNAL_ERROR` (never leak the underlying exception message or stack trace to the client — log it server-side instead) |

Implement this with a single global exception-handling component on the backend so no individual controller method needs its own try/catch.

---

## 10. Logging

- Use the framework's default logging facade (SLF4J/Logback) — do not add another logging library.
- Log at INFO for every successful create/update/delete, including the entity id.
- Log at WARN for handled conflict situations (e.g., a database-level integrity violation caught by the safety-net handler).
- Log at ERROR, with the full exception, for anything routed to the generic 500 handler.
- Do not log full request/response bodies (avoids putting employee PII like email/phone into logs unnecessarily).

---

## 11. Testing Requirements

### Backend
- Unit test the service layer with the repository mocked: cover duplicate-email rejection, duplicate-code rejection, not-found handling, and the immutability of `employeeCode` on update.
- Test the controller layer in isolation (mock the service): cover a valid create returning 201, an invalid payload returning 400 with the expected error shape, and a missing employee returning 404.
- Test the repository against a real MySQL instance (via a disposable test container), covering the search query with a few representative inputs.

### Frontend
- Component tests for the list page (loading/empty/error states render correctly) and the form page (validation errors show/hide correctly).
- A test for the employee service confirming it calls the right HTTP method/URL/params for each operation.
- A test confirming the reactive form is invalid when a required field is empty or a field violates its pattern, and valid when all fields are correct.

### Explicitly do not bother with
- Testing generated getters/setters or the DTOs themselves.
- Testing framework internals (Spring's or Angular's own wiring).
- Chasing 100% coverage — test the business rules and integration boundaries, not incidental code.

---

## 12. Configuration

- Externalize all database connection details (host, port, database name, username, password) as environment variables — never hardcode credentials anywhere, including in Docker Compose defaults meant for real use.
- Provide separate configuration profiles for: local development, automated tests, and a production-like profile.
- In every profile, configure Hibernate to only validate the schema, never to auto-generate or alter it — `schema.sql` is the single source of truth for structure.
- In the production-like profile, disable automatic SQL initialization entirely (it should only run in local/dev/test).
- The frontend should call the backend through a relative API path (e.g., proxied by the dev server locally and by Nginx in the containerized build) rather than hardcoding an absolute host.

---

## 13. Docker Requirements

Provide a Docker Compose setup with three services:
1. **MySQL 8** — with the `schema.sql` file mounted into its container-init path so the schema is ready on first boot; credentials driven by environment variables with local-only defaults.
2. **Backend** — built from a multi-stage Dockerfile (build stage compiles the Maven project, runtime stage runs the packaged jar on a slim JRE 21 image); depends on the database being healthy before starting.
3. **Frontend** — built from a multi-stage Dockerfile (build stage runs the Angular production build, runtime stage serves the static output, proxying API calls to the backend service).

Keep this Compose setup strictly for local development — do not attempt to make it production-grade.

---

## 14. Security Notes (even without authentication)

- Restrict CORS to the known frontend origin — never allow `*`.
- Rely on JPA's parameterized queries for all data access; never build SQL by string concatenation.
- Never return stack traces or internal exception details in API responses.
- Treat this POC as unsafe to expose outside a trusted local/dev network, since there is no authentication layer — state this explicitly in the project's README.

---

## 15. Implementation Order (recommended)

1. Set up the Maven project skeleton and the `schema.sql` file; verify the app boots and connects to MySQL.
2. Build the entity, DTOs, mapper, repository.
3. Build the service layer with its business rules and unit tests.
4. Build the controller and the global exception handler; add controller tests.
5. Verify the full API manually against the contract in Section 7 (all five endpoints, plus search and pagination).
6. Scaffold the Angular workspace with the folder structure in Section 8.1.
7. Build the employee service and models, wired to the real API.
8. Build the list page (with search, pagination, loading/empty/error states).
9. Build the create/edit form page with full validation and server-error mapping.
10. Build the detail page.
11. Add the Docker Compose setup and verify a clean `docker compose up --build` works end to end.
12. Add the remaining backend and frontend tests from Section 11.
13. Write a short README covering prerequisites and how to run everything locally.

---

## 16. Definition of Done

- All 6 functionalities in Section 2 work end to end through the UI, backed by the real API and database.
- Every validation rule in Section 4 is enforced on both backend and frontend.
- Every error scenario in Section 9 returns the exact documented shape.
- `docker compose up --build` produces a fully working stack from a clean checkout.
- Backend and frontend test suites from Section 11 pass.
- Nothing from the "out of scope" list in Section 2 has been added.

---

## Appendix A — Continuing this build from a Claude Cowork session (operational note)

This section is **operational guidance, not a functional requirement** — it doesn't change anything in Sections 1–16. It exists because this POC has been built across two different kinds of Claude sessions: a local **Claude Code CLI** session (real shell, real `git`, can run `mvn`/push directly) and a cloud **Cowork** session linked to this repo through the desktop app's device bridge (file read/write only, no shell). The two have different constraints, and this note is for the second kind.

**The constraint:** the device bridge can only read a file that sits at most **7 folders below whatever folder was connected** from the desktop app (originally `C:\handsoff\agent-handoff`). Re-requesting access to a folder that's already inside a connected one does **not** reset that count — only connecting a genuinely new folder from the desktop app does.

**Why this project hits it easily:** Java's package-per-directory convention nests deep fast. `backend/src/main/java/com/example/employee/controller/EmployeeController.java` is 8 folders below the repo root — one over the limit — and every layer added under `com.example.employee` (`service`, `repository`, `dto`, `exception`, `mapper`, …) is exactly as deep.

**The fix, going forward:** if a Cowork session needs to read or edit files under `backend/src/main/java/com/example/employee/...` and hits this, connect a deeper folder directly from the desktop app — e.g. `C:\handsoff\agent-handoff\backend` (puts every file at 7 folders or fewer) or, if still too deep, `C:\handsoff\agent-handoff\backend\src\main\java\com\example\employee` directly (puts every file at 5 or fewer). This only needs doing once per depth level, not per file. If connecting a new folder isn't convenient in the moment, pasting the needed file's content directly into the chat is the immediate fallback.

This same constraint does not apply to a local Claude Code CLI session, which reads the filesystem directly with no depth limit — it's specific to the cloud/device-bridge setup.
