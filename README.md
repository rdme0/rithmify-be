# Tierify - Spotify Tier Maker Backend

**Tierify**는 Spotify의 트랙, 앨범, 아티스트 정보를 활용하여 나만의 티어표(Tier List)를 만들 수 있는 서비스의 백엔드입니다.
**Spring Boot (MVC)**와 **Kotlin**을 기반으로 하며, **가상 스레드(Virtual Threads)**와 **코루틴(Coroutines)**을 적극
활용하여 고성능 처리를 지향합니다.

## 🤖 AI 개발자 가이드 (핸드오버 노트)

이 문서는 추후 프론트엔드 개발이나 유지보수를 담당할 AI 및 개발자를 위해 작성되었습니다.
**핵심 목표**: React 프론트엔드와 연동될 견고하고 빠른 REST API 서버 구축.

---

## 🛠 기술 스택 (Tech Stack)

- **Language**: Kotlin (JDK 21)
- **Framework**: Spring Boot 3.5.10 (Spring MVC)
- **Concurrency**: **Virtual Threads** Enabled (`spring.threads.virtual.enabled=true`)
- **Database**: PostgreSQL (JPA/Hibernate)
- **Cache**: Redis (Token Caching)
- **Auth**: Spring Security + OAuth2 Client (Spotify)
- **Async & Network**: **Kotlin Coroutines** + **WebClient** (Non-blocking I/O)
- **Logging**: `kotlin-logging` (v7)

---

## 🏗 설계 원칙 및 아키텍처 (Architecture)

이 프로젝트는 **DDD-lite** 접근 방식을 따르며, 기능(Feature) 단위로 패키지가 분리되어 있습니다.

### 1. 패키지 구조 (`src/main/kotlin/backend`)

* **`auth`**: 사용자 인증 및 권한.
    * `token` 관리는 Redis를 사용하지 않고 Session/Cookie 기반(Stateful) 혹은 추후 JWT 확장 가능.
    * 현재는 **Hybrid Auth Model** (Spotify Member + UUID Guest) 구현.
* **`tierlist`**: 티어표 핵심 도메인.
    * **N+1 문제 해결**: `IN` 절을 활용한 Batch Fetching 구현 (`TierListService`).
    * **Relation**: `@OneToMany` 사용 금지. 단방향 `@ManyToOne` 사용.
* **`spotify`**: Spotify Web API 연동 계층.
    * **Coroutines**: `suspend` 함수와 `awaitBody()`로 완전한 Non-blocking 구현.
    * **Mutex**: 토큰 갱신 시 동시성 제어 (`SpotifyTokenManager`).
    * **Redis**: Access Token 캐싱 (TTL 적용).
* **`common`**: 전역 예외 처리(`GlobalExceptionHandler`), 공통 상수(`Domain`, `ErrorCode`) 등.

### 2. 코딩 스타일 및 규칙 (Critical Rules)

다음 규칙은 **엄격하게** 준수되어야 합니다.

* **Safety First**: `!!` 연산자 절대 사용 금지. (`?: error(...)` 활용)
* **Configuration**: 하드코딩(URL, Key) 금지. `application.yml` 및 `@ConfigurationProperties` 사용.
* **Entity**: `@Table(name=...)`, `@Column(name=...)` 등 불필요한 명시적 네이밍 지양.
* **Performance**: 반복문 내 DB 호출 금지.

---

## 📡 주요 API 엔드포인트

### 🎵 Spotify Domain

| Method | Endpoint                               | Description                              |
|:-------|:---------------------------------------|:-----------------------------------------|
| `GET`  | `/api/spotify/search`                  | 아티스트 검색 (Query: `q`).                    |
| `GET`  | `/api/spotify/artists/{id}/top-tracks` | 아티스트 탑 트랙 조회. (`requirePreview=true` 가능) |

### 🏆 Tier List Domain

| Method | Endpoint               | Description               |
|:-------|:-----------------------|:--------------------------|
| `POST` | `/api/tier-lists`      | 티어표 생성 (JSON, Validated). |
| `GET`  | `/api/tier-lists/{id}` | 티어표 조회 (Full Aggregate).  |

---

## 🚀 실행 방법 (Setup & Run)

1. **Prerequisites**:
    * PostgreSQL (`localhost:5432`, db: `tierify`)
    * Redis (`localhost:6379`)
2. **Environment Variables**:
    * `SPOTIFY_CLIENT_ID`
    * `SPOTIFY_CLIENT_SECRET`
3. **Run**:
   ```bash
   ./gradlew bootRun
   ```

---

## 💾 데이터 모델 (ERD Concept)

```mermaid
erDiagram
    USERS { string id PK, string role }
    TIER_LISTS { long id PK, string title }
    TIER_GROUPS { long id PK, string label, string color_hex }
    TIER_ITEMS { long id PK, string spotify_id }

    USERS ||--o{ TIER_LISTS : owns
    TIER_LISTS ||--|{ TIER_GROUPS : "logical parent"
    TIER_GROUPS ||--|{ TIER_ITEMS : "logical parent"
```

*(실제 DB FK는 `ManyToOne`으로 자식들이 부모 ID를 가짐)*
