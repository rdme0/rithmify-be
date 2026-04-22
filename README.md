# Rithmify Backend

Spotify 데이터를 기반으로 음악 티어메이커를 만들기 위한 Spring Boot 백엔드입니다.

현재 MVP의 핵심 범위는 Spotify 아티스트/앨범/트랙 조회 API입니다. 티어리스트 도메인과 OAuth2 로그인 관련 코드는 일부 존재하지만, 컨트롤러와 로그인 플로우는 현재 비활성화되어 있습니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| Language | Kotlin 2.2.21, JDK 21 |
| Framework | Spring Boot 3.5.10, Spring MVC |
| Data | Spring Data JPA, PostgreSQL |
| Cache | Redis |
| External API | Spotify Web API |
| HTTP Client | WebClient, Kotlin Coroutines |
| Resilience | Resilience4j RateLimiter |
| Security | Spring Security, CORS |
| Build | Gradle Wrapper |

## 주요 기능

- Spotify 아티스트 검색
- 아티스트 인기 트랙 조회
- 아티스트 앨범 목록 조회
- 앨범 수록곡 조회
- Spotify access token Redis 캐싱
- 앨범/트랙 조회 결과 Redis 캐싱
- Spotify API rate limit 대응
- 커스텀 페이지네이션 및 정렬 파라미터 검증

## 프로젝트 구조

```text
src/main/kotlin/backend
├── auth        # 회원, 권한, OAuth2 사용자 서비스
├── common      # 공통 설정, 예외 처리, 페이지네이션, 상수
├── config      # Web MVC, Resilience4j 설정
├── spotify     # Spotify API 연동, DTO, 컨트롤러, 서비스
└── tierlist    # 티어리스트 도메인, 서비스, 저장소
```

## 실행 요구사항

- JDK 21
- PostgreSQL
- Redis
- Spotify Developer App의 `client_id`, `client_secret`

애플리케이션 기본 포트는 `10001`입니다.

## 환경 변수

루트 디렉터리에 `.env` 파일을 생성합니다.

```powershell
Copy-Item .env.example .env
```

macOS/Linux 환경에서는 다음 명령을 사용할 수 있습니다.

```bash
cp .env.example .env
```

개발 환경에서 필요한 기본 값은 다음과 같습니다.

```dotenv
SPRING_PROFILES_ACTIVE=dev

DEV_DATABASE_URL=jdbc:postgresql://localhost:5432/rithmify
DEV_DATABASE_USERNAME=postgres
DEV_DATABASE_PASSWORD=dev_password

DEV_REDIS_HOST=localhost
DEV_REDIS_PORT=6379

SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
```

운영 프로필(`prod`)에서는 다음 값도 필요합니다.

```dotenv
PROD_DATABASE_URL=jdbc:postgresql://prod-host:5432/rithmify_prod
PROD_DATABASE_USERNAME=rithmify_user
PROD_DATABASE_PASSWORD=secure_prod_password
PROD_REDIS_HOST=prod-redis-host
PROD_REDIS_PORT=6379
PROD_REDIS_PASSWORD=secure_redis_password
```

## 로컬 실행

PostgreSQL과 Redis를 먼저 실행한 뒤 애플리케이션을 시작합니다.

```powershell
.\gradlew.bat bootRun
```

macOS/Linux 환경에서는 다음 명령을 사용할 수 있습니다.

```bash
./gradlew bootRun
```

정상 실행 후 기본 API 주소는 다음과 같습니다.

```text
http://localhost:10001/api
```

## 테스트

```powershell
.\gradlew.bat test
```

macOS/Linux:

```bash
./gradlew test
```

## Docker

이미지를 빌드합니다.

```bash
docker build -t rithmify-be .
```

컨테이너 실행 예시:

```bash
docker run --rm -p 10001:10001 --env-file .env rithmify-be
```

Dockerfile은 `SPRING_PROFILES_ACTIVE=prod`를 기본값으로 사용합니다. 로컬 `.env`만으로 컨테이너를 실행하려면 운영용 DB/Redis 환경 변수를 함께 준비하거나 실행 시 프로필을 `dev`로 덮어써야 합니다.

```bash
docker run --rm -p 10001:10001 --env-file .env -e SPRING_PROFILES_ACTIVE=dev rithmify-be
```

## 활성 API

Base URL:

```text
/api
```

### 아티스트 검색

```http
GET /api/spotify/search/artists?query={query}
```

응답 예시:

```json
[
  {
    "id": "0TnOYISbd1XYRBk9myaseg",
    "name": "Pitbull",
    "images": [
      {
        "url": "https://i.scdn.co/image/...",
        "height": 640,
        "width": 640
      }
    ],
    "genres": ["dance pop", "miami hip hop"]
  }
]
```

### 아티스트 인기 트랙 조회

```http
GET /api/spotify/artists/{artistId}/top-tracks
```

응답 예시:

```json
[
  {
    "id": "0dA2Mk56wEzDgegdC6R17Y",
    "name": "Timber",
    "artistName": "Pitbull, Kesha",
    "albumName": "Meltdown",
    "imageUrl": "https://i.scdn.co/image/...",
    "releaseDate": "2013-01-01",
    "durationMs": 204160
  }
]
```

### 아티스트 앨범 목록 조회

```http
GET /api/spotify/artists/{artistId}/albums?page=0&size=20&sort=release_date&direction=desc
```

쿼리 파라미터:

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| `page` | `0` | 0부터 시작하는 페이지 번호 |
| `size` | `20` | 페이지 크기. 허용 범위: 1-100 |
| `sort` | 없음 | 허용 값: `name`, `release_date` |
| `direction` | `asc` | 허용 값: `asc`, `desc` |

응답은 Spring `Page<AlbumResponse>` 형태입니다.

```json
{
  "content": [
    {
      "id": "4aawyAB9vmqN3uQ7FjRGTy",
      "name": "Global Warming",
      "images": [
        {
          "url": "https://i.scdn.co/image/...",
          "height": 640,
          "width": 640
        }
      ],
      "releaseDate": "2012-11-16",
      "totalTracks": 12
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### 앨범 수록곡 조회

```http
GET /api/spotify/albums/{albumId}/tracks
```

응답 예시:

```json
[
  {
    "id": "3BovdzfaX4jb5KFQwoPfAw",
    "name": "Give Me Everything",
    "artistName": "Pitbull, Ne-Yo, Afrojack, Nayer",
    "albumName": "Planet Pit",
    "imageUrl": "https://i.scdn.co/image/...",
    "releaseDate": "2011-06-17",
    "durationMs": 251067
  }
]
```

## 캐싱 및 외부 API 호출 정책

- Spotify access token은 Redis의 `spotify:access_token` 키에 저장됩니다.
- 아티스트 앨범 목록은 `spotify:v4:artist:{artistId}:albums` 키로 24시간 캐싱됩니다.
- 앨범 수록곡은 `spotify:v4:album:{albumId}:tracks` 키로 24시간 캐싱됩니다.
- Spotify API 호출은 30초당 50회로 제한됩니다.
- Spotify가 `429 Too Many Requests`를 반환하면 `Retry-After` 헤더를 기준으로 최대 3회 재시도합니다.

## 에러 응답

공통 에러 응답 형식:

```json
{
  "code": "COMMON_400_003",
  "message": "유효하지 않은 페이지네이션 필드입니다: sort"
}
```

주요 에러:

| 코드 예시 | HTTP 상태 | 의미 |
| --- | --- | --- |
| `COMMON_400_003` | 400 | 페이지네이션 또는 정렬 파라미터 오류 |
| `SPOTIFY_502_001` | 502 | Spotify API 호출 실패 |
| `COMMON_500_001` | 500 | 서버 내부 오류 |

## 현재 비활성화된 기능

다음 기능은 코드 일부가 존재하지만 현재 HTTP API로 노출되어 있지 않습니다.

- `/api/tier-lists` 티어리스트 생성/조회 컨트롤러
- Spotify OAuth2 로그인 플로우

관련 코드는 유지되어 있으므로 기능을 다시 열 때는 다음 파일을 우선 확인하면 됩니다.

- `src/main/kotlin/backend/tierlist/controller/TierListController.kt`
- `src/main/kotlin/backend/common/config/SecurityConfig.kt`
- `src/main/kotlin/backend/auth/service/CustomOAuth2UserService.kt`

## 참고 문서

- `docs/api_documentation.md`
- `docs/coding_style_preferences.md`
- `docs/llm_development_workflow.md`
