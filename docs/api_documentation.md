# Rithmify Backend API Documentation

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

---

### 2. Artist Top Tracks

아티스트의 인기 트랙을 가져옵니다 (최대 10곡).

**Endpoint**
```http
GET /api/spotify/artists/{artistId}/top-tracks?requirePreview={boolean}
```

**Query Parameters**
None. (이전의 `requirePreview` 파라미터는 제거되었습니다. 모든 트랙을 반환하며, `previewUrl`이 없으면 `null`을 반환합니다.)

**Response**
```json
[
  {
    "id": "0dA2Mk56wEzDgegdC6R17Y",
    "name": "Timber (feat. Ke$ha)",
    "artistName": "Pitbull, Kesha",
    "albumName": "Meltdown",
    "imageUrl": "https://i.scdn.co/image/ab67616d0000b273...",
    "releaseDate": "2013-01-01",
    "previewUrl": "https://p.scdn.co/mp3-preview/...",
    "durationMs": 324000
  }
]
```

---

### 3. Artist Albums (New)

아티스트의 앨범 목록을 가져옵니다. (Lazy Loading 1단계)

**Endpoint**
```http
GET /api/spotify/artists/{artistId}/albums
```

**Path Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `artistId` | string | ✅ | Spotify 아티스트 ID |

**Response**
```json
{
  "content": [
    {
      "id": "4aawyAB9vmqN3uQ7FjRGTy",
      "name": "Global Warming",
      "images": [
        {
          "url": "https://i.scdn.co/image/ab67616d0000b273...",
          "height": 640,
          "width": 640
        }
      ],
      "releaseDate": "2012-11-16",
      "totalTracks": 12
    }
  ],
  "pageable": { ... },
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 4. Album Tracks (New)

특정 앨범의 트랙 목록을 상세 정보(이미지 포함)와 함께 가져옵니다. (Lazy Loading 2단계)

**Endpoint**
```http
GET /api/spotify/albums/{albumId}/tracks
```

**Path Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `albumId` | string | ✅ | Spotify 앨범 ID |

**Implementation Details**
- **Bulk Fetch Optimization**: 앨범 내 트랙 ID만 먼저 수집한 후, `getTracksByIds` API를 통해 상세 정보를 한 번에 가져옵니다.
- 이를 통해 **앨범 커버 이미지**와 **미리듣기 URL**을 확실하게 확보할 수 있습니다.

**Response**
```json
[
  {
    "id": "3BovdzfaX4jb5KFQwoPfAw",
    "name": "Give Me Everything",
    "artistName": "Pitbull, Ne-Yo, Afrojack, Nayer",
    "albumName": "Planet Pit (Deluxe Version)",
    "imageUrl": "https://i.scdn.co/image/ab67616d0000b273...",
    "releaseDate": "2011-06-17",
    "previewUrl": null,
    "durationMs": 251067
  }
]
```

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
  releaseDate: string | null; // 앨범 출시일 (YYYY-MM-DD or YYYY)
  previewUrl: string | null; // 30초 미리듣기 URL (없으면 null)
  durationMs: number;      // 재생 시간 (밀리초)
}
```

### AlbumResponse
```typescript
{
    id: string;
    name: string;
    images: ImageResponse[];
    releaseDate: string | null;
    totalTracks: number;
}
```

---

## 📊 Rate Limiting Strategy (Updated)

**Lazy Loading Architecture** 도입으로 Rate Limit 이슈를 해소했습니다.
- 과도한 API 호출(Artist All Tracks)을 제거했습니다.
- 사용자의 클릭(인터랙션)에 따라 API를 호출하므로 자연스럽게 부하가 분산됩니다.
- **Circuit Breaker Configuration**: 30초당 50개 요청 제한 (안전 장치)

---

## 🔄 Version History

### v1.3.0 (2026-02-04)
- 🔊 **Preview Policy**: `requirePreview` 파라미터 제거 (Option B: Null 허용)
- ✨ **Data Quality**: `TrackResponse`에 `releaseDate` 추가, `AlbumResponse`에 `totalTracks` 추가
- 🏷️ **Refactor**: 공개 API 응답 객체 표준화 (`AlbumResponse` 도입)

### v1.2.0 (2026-02-03)
- ♻️ **Major Refactor**: Lazy Loading 구조 도입
- `GET /artists/{id}/tracks` (All Tracks) **Deprecated & Removed**
- `GET /artists/{id}/albums` Added
- `GET /albums/{id}/tracks` Added

### v1.0.0 (2026-02-02)
- 🎉 Initial Release
