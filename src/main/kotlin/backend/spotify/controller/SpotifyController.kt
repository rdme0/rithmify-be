package backend.spotify.controller

import backend.common.annotation.ResolvePageable
import backend.common.constant.TrackSortField
import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.TrackResponse
import backend.spotify.service.SpotifySearchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/spotify")
class SpotifyController(private val spotifySearchService: SpotifySearchService) {

    @GetMapping("/search/artists")
    suspend fun searchArtist(
        @RequestParam("query") query: String
    ): ResponseEntity<List<ArtistSearchResponse>> {
        val results = spotifySearchService.searchArtist(query)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/artists/{id}/top-tracks")
    suspend fun getArtistTopTracks(
        @PathVariable id: String,
        @RequestParam(defaultValue = "false") requirePreview: Boolean
    ): ResponseEntity<List<TrackResponse>> {
        val tracks = spotifySearchService.getArtistTopTracks(id, requirePreview)
        return ResponseEntity.ok(tracks)
    }

    @GetMapping("/artists/{id}/tracks")
    suspend fun getArtistTracks(
        @PathVariable id: String,
        @ResolvePageable(
            allowed = [TrackSortField.NAME, TrackSortField.DURATION, TrackSortField.ALBUM_NAME, TrackSortField.ARTIST_NAME]
        )
        pageable: Pageable
    ): ResponseEntity<Page<TrackResponse>> {
        val trackPage = spotifySearchService.getArtistTracks(id, pageable)
        return ResponseEntity.ok(trackPage)
    }
}
