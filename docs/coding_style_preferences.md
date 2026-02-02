# Tierify Backend Coding Style Guide

이 문서는 Tierify 백엔드 프로젝트의 코딩 스타일 규칙을 정의합니다.

---

## 1. Architecture & Structure

### Package Organization
- **DDD-lite 구조**: 도메인별 패키지 구성 (`backend.{domain}.{layer}`)
- **계층 분리**: `domain`, `dto`, `repository`, `service`, `controller`, `exception` 패키지로 분리
- **One Class Per File**: 각 클래스, Enum, Exception은 별도 파일로 분리

### Exception Architecture
- **도메인별 Exception 패키지**: `backend.{domain}.exception`
- **Abstract Base**: `BusinessException`은 abstract class로 직접 인스턴스화 불가
- **Domain-Specific Exceptions**: 각 도메인은 자체 Exception 정의
  ```kotlin
  // ✅ 각 도메인의 exception 패키지
  backend.auth.exception.UserNotFoundException
  backend.tierlist.exception.TierListNotFoundException
  backend.common.exception.server.InternalServerException
  ```

---

## 2. API & Data Access

### Entity Design Principles

**Rule 1: No `@OneToMany`** (엄격 금지)
- 모든 관계는 자식→부모 방향으로 `@ManyToOne`만 사용
- 순환 참조 및 N+1 문제 원천 차단

**Rule 2: Clean Annotations**
- `@Table(name=...)`, `@Column(name=...)` 사용 금지 (필수 상황 제외)
- 클래스명과 필드명을 그대로 DB 테이블/컬럼으로 매핑

**Rule 3: Prefer Immutability**
- 가능한 `val` 사용, 변경 필요한 필드만 `var` 허용

### Service Layer Best Practices

**N+1 Problem Prevention**
```kotlin
// ❌ Loop 안에서 Repository 호출
items.forEach { item ->
    val user = userRepository.findById(item.userId)  // N+1 발생!
}

// ✅ Batch Fetch로 한 번에 조회
val userIds = items.map { it.userId }
val users = userRepository.findByIdIn(userIds)
val userMap = users.associateBy { it.id }
items.forEach { item ->
    val user = userMap[item.userId]
}
```

**Transaction Management**
```kotlin
@Service
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
class UserService {
    
    @Transactional  // 쓰기 메소드만 명시적으로 재정의
    fun createUser(request: CreateUserRequest): User { ... }
}
```

---

## 3. Exception Handling

### Exception Hierarchy

```
BusinessException (abstract)
├── Domain Exceptions
│   ├── UserNotFoundException
│   └── TierListNotFoundException
└── Server Exceptions
    └── InternalServerException
```

### Exception Usage Rules

**1. Never throw `BusinessException` directly**
```kotlin
// ❌ Generic BusinessException
throw BusinessException(ErrorCode.USER_NOT_FOUND)

// ✅ Domain-specific exception
throw UserNotFoundException()
```

**2. Wrap unexpected errors in `InternalServerException`**
```kotlin
// ❌ Generic error()
val token = response["access_token"] as? String
    ?: error("Token not found")

// ✅ InternalServerException wrapping
val token = response["access_token"] as? String
    ?: throw InternalServerException(
        IllegalStateException("Token not found")
    )
```

**3. No `!!` operator - use safe calls + exceptions**
```kotlin
// ❌ Unsafe
val userId = user.id!!

// ✅ Safe with domain exception
val userId = user.id
    ?: throw InternalServerException(
        IllegalStateException("User ID missing")
    )
```

---

## 4. Asynchrony & Performance

### Coroutines for External I/O
```kotlin
// ✅ 외부 API는 suspend function
suspend fun searchArtists(query: String): List<Artist> {
    return webClient
        .get()
        .uri("/search?q=$query")
        .awaitBody<Response>()  // Non-blocking
}
```

### Redis Caching
- **TTL 필수**: 모든 Redis 저장 시 만료 시간 설정
- **SpotifyTokenManager 참고**: `redisTemplate.opsForValue().set(key, value, ttl)`

### Concurrency Control
- **Mutex**: Coroutine 환경에서 `synchronized` 대신 `Mutex` 사용
- **Virtual Threads**: Spring Boot 21+ 환경에서는 blocking I/O도 효율적이나, 외부 API는 Coroutines 권장

---

## 5. Configuration & Environment

### Externalize All Constants
```kotlin
// ❌ Hardcoded
val tokenUrl = "https://accounts.spotify.com/api/token"

// ✅ ConfigurationProperties
@ConfigurationProperties(prefix = "spotify.security")
data class SpotifySecurityProperties(
    val tokenUrl: String,
    val clientId: String,
    val clientSecret: String
)
```

### Use application.yml
```yaml
spotify:
  security:
    token-url: https://accounts.spotify.com/api/token
    client-id: ${SPOTIFY_CLIENT_ID}
    client-secret: ${SPOTIFY_CLIENT_SECRET}
```

---

## 6. Kotlin Code Formatting

### Indentation
- **4 spaces** (not 2, not tabs)
- **Trailing newline** at end of file

### Assignment Operators
```kotlin
// ✅ Simple assignment on same line
val user = User(providerId = id, role = UserRole.GUEST)

// ✅ Complex multi-line after =
val response = webClient
    .get()
    .retrieve()
    .awaitBody<Response>()

// ❌ Avoid unnecessary line break
val user =
    User(providerId = id)
```

### Constructor Parameters
```kotlin
// ✅ Multi-line constructors: one parameter per line
class TierList(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) 
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "user_id") 
    val user: User,
    @Column(nullable = false) 
    var title: String
)
```

### Import Organization
```kotlin
// ✅ Explicit imports
import backend.auth.domain.User
import backend.auth.repository.UserRepository
import backend.common.exception.InternalServerException

// ❌ Wildcard imports
import backend.auth.domain.*
```

**Import order**: Domain imports → Spring imports → JDK imports (with blank line separator)

### Method Chaining
```kotlin
// ✅ Consistent indentation
webClient
    .get()
    .uri("/path")
    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    .retrieve()
    .awaitBody<Response>()
```

---

## 7. Logging

### Use Kotlin-Logging
```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

// ✅ Structured logging
logger.info { "Creating tier list: ${request.title}" }
logger.error(exception) { "Failed to fetch Spotify data" }
```

---

## 8. Commit Convention

### Gitmoji Prefixes
- `:sparkles:` feat - 새 기능
- `:recycle:` refactor - 리팩토링
- `:bug:` fix - 버그 수정
- `:art:` style - 코드 포맷팅
- `:memo:` docs - 문서
- `:wrench:` chore - 빌드/설정
- `:white_check_mark:` test - 테스트
- `:see_no_evil:` - .gitignore

### Commit Granularity
- **작은 단위**: 논리적으로 독립적인 변경사항으로 커밋 분리
- **명확한 메시지**: 무엇을 왜 바꿨는지 설명

---

이 가이드를 따름으로써 **가독성 높고 유지보수 가능한 코드**를 작성할 수 있습니다.
