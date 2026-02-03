package backend.spotify.controller

import backend.spotify.dto.internal.SpotifyAlbumDTO
import backend.spotify.dto.response.TrackResponse
import backend.spotify.service.SpotifySearchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/spotify")
class SpotifyController(private val spotifySearchService: SpotifySearchService) {

    @GetMapping("/search/artists")
    suspend fun searchArtist(query: String): ResponseEntity<Any> {
        val artists = spotifySearchService.searchArtist(query)
        return ResponseEntity.ok(artists)
    }

    @GetMapping("/artists/{id}/top-tracks")
    suspend fun getArtistTopTracks(@PathVariable id: String): ResponseEntity<List<TrackResponse>> {
        val tracks = spotifySearchService.getArtistTopTracks(id, requirePreview = false)
        return ResponseEntity.ok(tracks)
    }

    @GetMapping("/artists/{id}/albums")
    suspend fun getArtistAlbums(
            @PathVariable id: String,
            @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<SpotifyAlbumDTO>> {
        val albums = spotifySearchService.getArtistAlbums(id, pageable)
        return ResponseEntity.ok(albums)
    }

    @GetMapping("/albums/{id}/tracks")
    suspend fun getAlbumTracks(@PathVariable id: String): ResponseEntity<List<TrackResponse>> {
        val tracks = spotifySearchService.getAlbumTracks(id)
        return ResponseEntity.ok(tracks)
    }
}
