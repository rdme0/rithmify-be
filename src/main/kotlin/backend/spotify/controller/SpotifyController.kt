package backend.spotify.controller

import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.TrackResponse
import backend.spotify.service.SpotifySearchService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
            @backend.common.annotation.ResolvePageable(
                    allowed =
                            [
                                    backend.common.constant.TrackSortField.NAME,
                                    backend.common.constant.TrackSortField.DURATION,
                                    backend.common.constant.TrackSortField.ALBUM_NAME,
                                    backend.common.constant.TrackSortField.ARTIST_NAME]
            )
            pageable: org.springframework.data.domain.Pageable
    ): ResponseEntity<org.springframework.data.domain.Page<TrackResponse>> {
        val trackPage = spotifySearchService.getArtistTracks(id, pageable)
        return ResponseEntity.ok(trackPage)
    }
}
