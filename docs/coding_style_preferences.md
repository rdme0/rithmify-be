# User Coding Style Preferences

## 1. Architecture & Structure
- **DDD-lite**: Package by Feature (`backend.domain.feature`).
- **Separation of Concerns**: Entity, DTO (Request/Response separation), Repository, Service, Controller in distinct packages.
- **One File Per Class**: separate files for Entities, Enums, etc.

## 2. API & Data Access
- **Entity Guidelines**:
    - **No `@OneToMany`**: Strict prohibition. Use Unidirectional `@ManyToOne` only.
    - **Clean Annotations**: No explicit `@Table(name=...)` or `@Column(name=...)` unless unavoidable.
    - **Clean Code**: No `!!` operators. Use `?: error(...)` or safe calls.
- **Service Layer**:
    - **Batch Fetching**: Use `IN` clause for fixing N+1 problems. No loops with DB calls.
    - **Manual Mapping**: Explicit DTO assembly in Service (or Mapper classes) to keep control.

## 3. Asynchrony & Performance
- **Coroutines**: Use Kotlin Coroutines (`suspend`, `awaitBody`) for I/O bound operations (External APIs).
- **WebClient**: Use `awaitBody` + `kotlinx-coroutines-reactor`.
- **Redis**: Use Redis for caching (Tokens, etc) with TTL.
- **Concurrency**: Use `Mutex` for thread-safe operations in Coroutines (Note: Simplified in SpotifyClient as of recent refactor, but general principle applies).

## 4. Configuration & Environment
- **Externalization**: No hardcoded strings (URLs, keys) in code. Use `@ConfigurationProperties` and `application.yml`.
- **Soft Coding**: Use Constants, Extension Properties for repetitive map access or keys.

## 5. Exception Handling & Logging
- **Global Handling**: `GlobalExceptionHandler` with `@RestControllerAdvice`.
- **Structured Errors**: `ErrorCode` Enum with Domain + Status + strict code format.
- **Library**: Use `io.github.oshai:kotlin-logging` (KLogging) instead of SLF4J directly.

## 6. Frontend Alignment
- **Future**: Project will serve a React/Next.js frontend.
