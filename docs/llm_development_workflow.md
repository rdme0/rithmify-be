# 🧠 LLM & 개발자를 위한 Tierify 백엔드 개발 사고(Thought) 가이드

이 문서는 **Tierify** 프로젝트에 새로운 기능을 추가하거나 유지보수할 때, 개발자(혹은 AI)가 따라야 할 **생각의 흐름(Flow)**과 **행동 패턴**을 정의합니다.
이 프로젝트는 단순한 기능 구현을 넘어 **가독성, 안정성(Safety), 고성능(Concurrency)**을 최우선으로 합니다.

---

## 1. 🚀 새로운 도메인(기능)을 추가할 때의 사고 흐름

새로운 기능(예: `Comment`, `Like` 등)을 만들어야 한다면, 다음 순서대로 사고하고 코드를 작성하십시오.

### **Step 1: 에러 정의부터 시작하라 (Define Failure First)**
"이 기능에서 무엇이 실패할 수 있는가?"를 먼저 정의함으로써 비즈니스 로직의 경계를 명확히 합니다.
1.  **`backend.common.constant.Domain` Enum 추가**
    *   예: `COMMENT` 추가.
2.  **`backend.common.exception.enums.ErrorCode` 정의**
    *   해당 도메인에서 발생할 수 있는 구체적인 에러 상황을 정의합니다.
    *   예: `COMMENT_NOT_FOUND`, `COMMENT_TOO_LONG`
    *   *규칙*: HTTP Status와 도메인을 명확히 매핑할 것.

### **Step 2: 엔티티 설계 (Entity Design)**
DB 스키마를 정의합니다. 단, JPA의 "Magic"을 경계하십시오.
1.  **패키지 생성**: `backend.comment.domain`
2.  **Entity 클래스 작성**:
    *   **규칙 1 (Clean Annotations)**: `@Table(name=...)`, `@Column(name=...)`을 쓰지 마십시오. 클래스명과 필드명을 그대로 사용합니다.
    *   **규칙 2 (No OneToMany)**: **절대** `@OneToMany`를 사용하지 마십시오. 모든 관계는 자식 쪽에서 `@ManyToOne`으로 단방향 참조만 가집니다. (순환 참조 및 N+1 원천 차단)
    *   **규칙 3 (Immutability)**: 가능한 `val`을 사용하고, 변경이 필요한 필드만 `var`를 허용합니다.

### **Step 3: DTO 설계 (Contract Design)**
클라이언트와의 약속을 정의합니다. Entity를 절대로 직접 반환하지 마십시오.
1.  **패키지**: `backend.comment.dto.request`, `backend.comment.dto.response`
2.  **Request DTO**:
    *   `data class` 사용.
    *   **Validation**: `jakarta.validation` 어노테이션(`@NotBlank`, `@Min`)을 필수로 작성합니다.
3.  **Response DTO**:
    *   필요한 데이터만 노출합니다.

### **Step 4: 비즈니스 로직 구현 (Service Layer)**
가장 중요한 단계입니다. 성능과 트랜잭션을 고려합니다.
1.  **Transactional**: 클래스 레벨에 `@Transactional(readOnly = true)`를 걸고, 쓰기 작업 메소드에만 `@Transactional`을 붙입니다.
2.  **N+1 문제 의식적 회피**:
    *   반복문(Loop) 안에서 Repository를 호출하지 마십시오.
    *   **Batch Fetching**: `Repository`에 `findByIdIn(...)` 같은 메소드를 만들고, Service에서 ID 목록을 추출해 **한 번에 조회**한 뒤 메모리에서 조합하십시오.
3.  **DTO 변환**: Entity -> DTO 변환은 Service 레이어(혹은 별도 Mapper)에서 명시적으로 수행합니다.

### **Step 5: API 노출 (Controller Layer)**
1.  **RESTful**: 자원 중심의 URL 설계 (`POST /api/comments`, `GET /api/comments/{id}`).
2.  **Response**: `ResponseEntity`를 사용하여 명확한 상태 코드를 반환합니다.

---

## 2. ⚡ 외부 API 연동 시 사고 흐름 (External Integration)

Spotify 같은 외부 API를 연동해야 한다면?

1.  **Blocking은 죄악이다**: 무조건 **Kotlin Coroutines** (`suspend` function)를 사용하십시오.
2.  **Client 구현**:
    *   Spring `WebClient` + `awaitBody()` 패턴 사용.
    *   `.block()` 절대 금지.
3.  **동시성 제어 (Concurrency)**:
    *   토큰 갱신 등 공유 자원 접근 시 `Mutex`를 사용하십시오 (`synchronized` 대신).
    *   *(Note: 최근 리팩토링에서 단순화를 위해 Mutex를 제거했으나, 복잡한 락 필요 시 Mutex가 표준입니다.)*
4.  **캐싱 (Caching)**:
    *   빈번한 호출이나 토큰은 **Redis**에 저장합니다 (`StringRedisTemplate`).
    *   반드시 **TTL(만료 시간)**을 설정하십시오.
5.  **설정 분리**:
    *   URL, Key 등은 코드가 아니라 `application.yml` -> `@ConfigurationProperties`로 관리합니다.

---

## 3. 🧹 코딩 컨벤션 및 스타일 (Coding Style)

코드를 작성할 때 항상 다음 질문을 던지십시오.

*   **"이 코드가 안전한가?"**
    *   `!!` (Double Bang) 연산자 발견 시 즉시 리팩토링하십시오. (`?: error(...)` 사용)
    *   `Unsafe Cast` (`as`) 대신 `Safe Cast` (`as?`)를 사용하십시오.
*   **"이 코드가 읽기 쉬운가?"**
    *   매직 넘버/스트링을 상수로 추출했는가?
    *   함수가 너무 길지 않은가?
*   **"이 코드가 효율적인가?"**
    *   불필요한 DB 호출이 없는가?
    *   가상 스레드(`Virtual Threads`) 환경임을 인지하고 있는가? (Blocking I/O가 물리 스레드를 잡지 않으므로 동기 스타일 코드로도 높은 처리량을 낼 수 있음, 단 외부 요청은 Coroutines 권장)

---

## 4. 📂 디렉토리 구조 맵 (Project Map)

```text
backend
├── auth          # 인증 (User, OAuth2, Guest)
├── tierlist      # 핵심 기능 (TierList, TierGroup, TierItem)
├── spotify       # 외부 연동 (Client, Token, Search)
└── common        # 공통 (Exception, Constants, Config)
```

이 가이드를 준수함으로써, 우리는 **시간이 지나도 썩지 않는 코드**를 유지할 수 있습니다.
