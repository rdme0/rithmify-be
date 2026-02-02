package backend.spotify.controller

import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.TrackResponse
import backend.spotify.service.SpotifySearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/spotify")
class SpotifyController(private val spotifySearchService: SpotifySearchService) {

    @GetMapping("/search")
    suspend fun searchArtist(
        @RequestParam("q") query: String
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
}
