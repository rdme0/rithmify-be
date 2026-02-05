# 🧠 Rithmify Backend Development Workflow Guide

이 문서는 Rithmify 백엔드에 **새로운 기능을 추가하거나 리팩토링**할 때 따라야 할 **사고 흐름(Thought Process)**을 정의합니다.

**핵심 원칙**: 가독성, 안정성(Safety), 고성능(Performance)

---

## 1. 🚀 새로운 도메인 추가 시 Workflow

새로운 기능(예: `Comment`, `Like`)을 만들 때 다음 순서를 따르십시오.

### Step 1: Exception 정의 (Define Failure First)

"무엇이 실패할 수 있는가?"를 먼저 정의합니다.

#### 1-1. Domain Enum 추가
```kotlin
// backend/common/constant/Domain.kt
enum class Domain {
    COMMON, AUTH, TIERLIST, SPOTIFY, COMMENT  // ← 추가
}
```

#### 1-2. ErrorCode 정의
```kotlin
// backend/common/exception/enums/ErrorCode.kt
enum class ErrorCode(...) {
    // Comment domain
    COMMENT_NOT_FOUND(Domain.COMMENT, HttpStatus.NOT_FOUND, 1, "존재하지 않는 댓글입니다."),
    COMMENT_TOO_LONG(Domain.COMMENT, HttpStatus.BAD_REQUEST, 2, "댓글이 너무 깁니다."),
}
```

#### 1-3. Domain-Specific Exception 생성
```kotlin
// backend/comment/exception/CommentNotFoundException.kt
package backend.comment.exception

import backend.common.exception.BusinessException
import backend.common.exception.enums.ErrorCode

class CommentNotFoundException : BusinessException(ErrorCode.COMMENT_NOT_FOUND)
```

**✅ 중요**: `BusinessException`을 직접 throw하지 말고, 항상 도메인별 Exception을 만들어 사용합니다.

---

### Step 2: Entity 설계 (Domain Model)

#### 패키지: `backend.comment.domain`

```kotlin
@Entity
class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) 
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_list_id")
    val tierList: TierList,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
    
    @Column(nullable = false, length = 500)
    var content: String,
    
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

**필수 규칙**:
- ❌ `@OneToMany` 절대 사용 금지
- ✅ 모든 관계는 자식→부모 방향 `@ManyToOne`만
- ✅ Clean Annotations: `@Table(name=...)` 사용하지 않음
- ✅ `val` 우선, 변경 필요 시만 `var`

---

### Step 3: DTO 설계 (Contract)

Entity를 직접 반환하지 말고 DTO를 사용합니다.

#### 패키지: `backend.comment.dto.request`, `backend.comment.dto.response`

```kotlin
// Request
data class CreateCommentRequest(
    @field:NotBlank(message = "댓글 내용은 필수입니다")
    @field:Size(max = 500, message = "댓글은 500자 이하여야 합니다")
    val content: String
)

// Response
data class CommentResponse(
    val id: Long,
    val content: String,
    val writerName: String,
    val createdAt: LocalDateTime
)
```

---

### Step 4: Service Layer 구현 (Business Logic)

#### 4-1. Transaction 관리
```kotlin
@Service
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
class CommentService(
    private val commentRepository: CommentRepository,
    private val tierListRepository: TierListRepository,
    private val userRepository: UserRepository
) {
    
    @Transactional  // 쓰기 작업만 명시적으로
    fun createComment(
        tierListId: Long,
        userId: Long,
        request: CreateCommentRequest
    ): CommentResponse {
        val tierList = tierListRepository.findByIdOrNull(tierListId)
            ?: throw TierListNotFoundException()
        
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException()
        
        val comment = Comment(
            tierList = tierList,
            user = user,
            content = request.content
        )
        
        val saved = commentRepository.save(comment)
        return toResponse(saved)
    }
}
```

#### 4-2. N+1 문제 방지 (Batch Fetching)
```kotlin
fun getCommentsForTierList(tierListId: Long): List<CommentResponse> {
    // 1. Comments 조회
    val comments = commentRepository.findByTierListId(tierListId)
    
    // 2. User IDs 추출
    val userIds = comments.mapNotNull { it.user.id }
    
    // 3. Batch Fetch Users (한 번에 조회)
    val users = userRepository.findByIdIn(userIds)
    val userMap = users.associateBy { it.id }
    
    // 4. 메모리에서 조합
    return comments.map { comment ->
        val user = userMap[comment.user.id]!!
        CommentResponse(
            id = comment.id!!,
            content = comment.content,
            writerName = user.displayName ?: "Anonymous",
            createdAt = comment.createdAt
        )
    }
}
```

**핵심**: Loop 안에서 Repository를 호출하지 말 것!

---

### Step 5: Controller (API Layer)

```kotlin
@RestController
@RequestMapping("/api/tier-lists/{tierListId}/comments")
class CommentController(
    private val commentService: CommentService
) {
    
    @PostMapping
    fun createComment(
        @PathVariable tierListId: Long,
        @RequestBody @Valid request: CreateCommentRequest,
        @AuthenticationPrincipal userDetails: OAuth2User
    ): ResponseEntity<CommentResponse> {
        val userId = userDetails.getAttribute<Long>("id")
            ?: throw InternalServerException(
                IllegalStateException("User ID missing")
            )
        
        val response = commentService.createComment(tierListId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @GetMapping
    fun getComments(@PathVariable tierListId: Long): ResponseEntity<List<CommentResponse>> {
        val comments = commentService.getCommentsForTierList(tierListId)
        return ResponseEntity.ok(comments)
    }
}
```

---

## 2. ⚡ 외부 API 연동 Workflow

Spotify, 결제 API 등 외부 연동 시 패턴입니다.

### 원칙
1. **Blocking 금지**: 무조건 **Kotlin Coroutines** (`suspend` function)
2. **WebClient**: Spring WebClient + `awaitBody()` 사용
3. **Caching**: Redis에 TTL과 함께 저장
4. **Configuration**: 모든 URL/Key는 `@ConfigurationProperties`로 외부화

### 예시: Token Manager
```kotlin
@Component
class ExternalApiTokenManager(
    private val properties: ExternalApiProperties,
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        private const val REDIS_KEY = "external:access_token"
        private const val TOKEN_BUFFER_SECONDS = 60L
    }
    
    suspend fun getToken(): String {
        return redisTemplate.opsForValue().get(REDIS_KEY)
            ?: refreshToken()
    }
    
    private suspend fun refreshToken(): String {
        val client = WebClient.create()
        val response = client.post()
            .uri(properties.tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(/* auth body */)
            .retrieve()
            .awaitBody<Map<String, Any>>()
        
        val token = response["access_token"] as? String
            ?: throw InternalServerException(
                IllegalStateException("Failed to retrieve token")
            )
        
        val expiresIn = (response["expires_in"] as? Int)?.toLong() ?: 3600L
        val ttl = Duration.ofSeconds(expiresIn - TOKEN_BUFFER_SECONDS)
        
        redisTemplate.opsForValue().set(REDIS_KEY, token, ttl)
        return token
    }
}
```

---

## 3. 🧹 Code Quality Checklist

코드 작성 후 다음 질문들을 점검하십시오:

### Safety (안전성)
- [ ] `!!` 연산자가 없는가? (있다면 Exception으로 대체)
- [ ] `as` 대신 `as?` (Safe Cast) 사용했는가?
- [ ] `error()` 대신 도메인 Exception 사용했는가?

### Readability (가독성)
- [ ] 매직 넘버/스트링을 상수로 추출했는가?
- [ ] 함수 길이가 적절한가? (50줄 이하 권장)
- [ ] 변수명이 명확한가?

### Performance (성능)
- [ ] Loop 안에 DB 호출이 없는가?
- [ ] `findByIdIn()`으로 Batch Fetch 했는가?
- [ ] 외부 API는 `suspend` + `awaitBody()` 사용했는가?

### Transaction (트랜잭션)
- [ ] 클래스 레벨에 `@Transactional(readOnly = true)` 있는가?
- [ ] 쓰기 메소드에만 `@Transactional` 재정의했는가?

---

## 4. 📂 Project Structure Map

```text
backend
├── auth/              # 인증 (User, OAuth2, Guest)
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── exception/     # UserNotFoundException
│
├── tierlist/          # 핵심 기능 (TierList, TierGroup, TierItem)
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── exception/     # TierListNotFoundException
│
├── spotify/           # 외부 연동 (Client, Token, Search)
│   ├── client/
│   ├── config/
│   └── dto/
│
└── common/            # 공통 (Exception, Constants, Config)
    ├── constant/      # Domain Enum
    ├── exception/     # BusinessException, ErrorCode
    │   ├── enums/
    │   └── server/    # InternalServerException
    └── config/        # Security, Redis, Web
```

---

## 5. 🎯 Development Mindset

> **"시간이 지나도 썩지 않는 코드"**를 작성하기 위해:
> 
> 1. **Exception First**: 에러 상황을 먼저 정의하라
> 2. **No Magic**: JPA의 자동화에 의존하지 말고 명시적으로 제어하라
> 3. **Explicit DTO**: Entity를 직접 노출하지 말라
> 4. **Batch Everything**: N+1을 의식적으로 회피하라
> 5. **Async by Default**: 외부 I/O는 무조건 Coroutines

이 가이드를 따라 **견고하고 확장 가능한 백엔드**를 구축하십시오.
