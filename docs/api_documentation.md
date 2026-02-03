# Tierify Backend API Documentation

> **Last Updated**: 2026-02-03  
> **Base URL**: `/api`

---

## 🎵 Spotify Integration APIs

### 1. Artist Search

아티스트를 검색합니다.

**Endpoint**
```http
GET /api/spotify/search/artists?query={query}
```

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | ✅ | 검색어 (아티스트명) |

**Response**
```json
[
  {
    "id": "0TnOYISbd1XYRBk9myaseg",
    "name": "Pitbull",
    "images": [
      {
        "url": "https://i.scdn.co/image/ab6761610000e5eb...",
        "height": 640,
        "width": 640
      }
    ],
    "genres": ["dance pop", "miami hip hop", "pop rap"]
  }
]
```

**Status Codes**
- `200 OK`: 성공
- `500 Internal Server Error`: Spotify API 오류

---

### 2. Artist Top Tracks

아티스트의 인기 트랙을 가져옵니다 (최대 10곡).

**Endpoint**
```http
GET /api/spotify/artists/{artistId}/top-tracks?requirePreview={boolean}
```

**Path Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `artistId` | string | ✅ | Spotify 아티스트 ID |

**Query Parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `requirePreview` | boolean | ❌ | `false` | 미리듣기 URL 필수 여부 |

**Response**
```json
[
  {
    "id": "0dA2Mk56wEzDgegdC6R17Y",
    "name": "Timber (feat. Ke$ha)",
    "artistName": "Pitbull, Kesha",
    "albumName": "Meltdown",
    "imageUrl": "https://i.scdn.co/image/ab67616d0000b273...",
    "previewUrl": "https://p.scdn.co/mp3-preview/...",
    "durationMs": 324000
  }
]
```

**Status Codes**
- `200 OK`: 성공
- `404 Not Found`: 아티스트를 찾을 수 없음
- `500 Internal Server Error`: Spotify API 오류

---

### 3. Artist All Tracks (Infinite Scroll)

아티스트의 전체 디스코그래피를 페이지네이션으로 제공합니다.

**Endpoint**
```http
GET /api/spotify/artists/{artistId}/tracks?page={page}&size={size}
```

**Path Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `artistId` | string | ✅ | Spotify 아티스트 ID |

**Query Parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | ❌ | `0` | 페이지 번호 (0부터 시작) |
| `size` | integer | ❌ | `20` | 페이지당 항목 수 |

**Implementation Details**

1. **Album Traversal Strategy**
   - 아티스트의 모든 앨범/싱글 정보를 가져옵니다
   - 각 앨범의 트랙을 **병렬로** 조회합니다 (Kotlin Coroutines)
   - 중복 제거: `곡명 + 재생시간` 기준으로 필터링

2. **Redis Caching**
   - 캐시 키: `spotify:artist:{artistId}:all_tracks`
   - TTL: **1시간**
   - 첫 요청 후 Redis에 전체 트랙 리스트 저장
   - 이후 요청은 Redis에서 즉시 반환

3. **In-Memory Pagination**
   - Redis에서 가져온 전체 리스트를 메모리에서 페이지네이션
   - `fromIndex = page * size`
   - 범위 초과 시 빈 배열 반환

**Response**
```json
[
  {
    "id": "3BovdzfaX4jb5KFQwoPfAw",
    "name": "Give Me Everything",
    "artistName": "Pitbull, Ne-Yo, Afrojack, Nayer",
    "albumName": "Planet Pit (Deluxe Version)",
    "imageUrl": "https://i.scdn.co/image/ab67616d0000b273...",
    "previewUrl": "https://p.scdn.co/mp3-preview/...",
    "durationMs": 251067
  },
  {
    "id": "7w87IxuO7BDcJ3YUqCyMTT",
    "name": "Time of Our Lives",
    "artistName": "Pitbull, Ne-Yo",
    "albumName": "Globalization",
    "imageUrl": "https://i.scdn.co/image/ab67616d0000b273...",
    "previewUrl": "https://p.scdn.co/mp3-preview/...",
    "durationMs": 228693
  }
]
```

**Infinite Scroll Example**

```typescript
// React/Next.js 예시
const [tracks, setTracks] = useState([]);
const [page, setPage] = useState(0);
const [hasMore, setHasMore] = useState(true);

async function loadMoreTracks(artistId: string) {
  const res = await fetch(
    `/api/spotify/artists/${artistId}/tracks?page=${page}&size=20`
  );
  const newTracks = await res.json();
  
  if (newTracks.length === 0) {
    setHasMore(false);
    return;
  }
  
  setTracks(prev => [...prev, ...newTracks]);
  setPage(prev => prev + 1);
}
```

**Performance Characteristics**
- **첫 요청**: ~2-5초 (앨범 수, Spotify API 응답 속도에 따라 변동)
- **캐시 히트**: ~50-100ms (Redis 조회 + 메모리 페이지네이션)
- **병렬 처리**: 최대 50개 앨범까지 동시 조회 가능

**Status Codes**
- `200 OK`: 성공 (빈 배열 포함)
- `404 Not Found`: 아티스트를 찾을 수 없음
- `500 Internal Server Error`: Spotify API 오류

---

## 📋 Common Response Models

### TrackResponse
```typescript
{
  id: string;              // Spotify Track ID
  name: string;            // 곡명
  artistName: string;      // 아티스트명 (콤마로 구분)
  albumName: string;       // 앨범명
  imageUrl: string | null; // 앨범 커버 이미지 URL
  previewUrl: string | null; // 30초 미리듣기 URL
  durationMs: number;      // 재생 시간 (밀리초)
}
```

### ArtistSearchResponse
```typescript
{
  id: string;              // Spotify Artist ID
  name: string;            // 아티스트명
  images: ImageResponse[]; // 프로필 이미지 배열
  genres: string[];        // 장르 목록
}
```

### ImageResponse
```typescript
{
  url: string;             // 이미지 URL
  height: number | null;   // 높이 (픽셀)
  width: number | null;    // 너비 (픽셀)
}
```

---

## 🔐 Authentication

현재는 인증이 필요하지 않습니다. (향후 JWT 인증 추가 예정)

---

## 🚨 Error Handling

### Error Response Format
```json
{
  "timestamp": "2026-02-03T12:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to fetch data from Spotify API",
  "path": "/api/spotify/artists/invalid-id/tracks"
}
```

### Common Error Codes
- `400 Bad Request`: 잘못된 요청 파라미터
- `404 Not Found`: 리소스를 찾을 수 없음
- `500 Internal Server Error`: 서버 내부 오류 (Spotify API 장애 포함)

---

## 📊 Rate Limiting & Caching Strategy

### Redis Cache Policy
- **Artist All Tracks**: 1시간 TTL
- **Spotify Access Token**: 55분 TTL (60분 토큰 만료 대비 5분 버퍼)

### Spotify API Rate Limits
- Spotify Web API 제한: 초당 약 10-20 요청
- 본 서비스는 캐싱을 통해 API 호출 최소화

---

## 🔄 Version History

### v1.1.0 (2026-02-03)
- ✨ Artist All Tracks API 추가 (무한 스크롤 지원)
- ⚡ Redis 캐싱으로 성능 최적화
- 🚀 Kotlin Coroutines를 통한 병렬 API 호출

### v1.0.0 (2026-02-02)
- 🎉 Initial Release
- Artist Search API
- Artist Top Tracks API
