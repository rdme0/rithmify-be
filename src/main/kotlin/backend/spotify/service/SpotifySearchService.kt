package backend.spotify.service

import backend.spotify.client.SpotifyClient
import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.SpotifyImageResponse
import backend.spotify.dto.response.TrackResponse
import org.springframework.stereotype.Service

@Service
class SpotifySearchService(private val spotifyClient: SpotifyClient) {

    suspend fun searchArtist(query: String): List<ArtistSearchResponse> {
        val response = spotifyClient.searchArtist(query)

        return response?.artists?.items?.map { artist ->
            ArtistSearchResponse(
                    id = artist.id,
                    name = artist.name,
                    images =
                            artist.images.map { img ->
                                SpotifyImageResponse(
                                        url = img.url,
                                        height = img.height,
                                        width = img.width
                                )
                            },
                    genres = artist.genres
            )
        }
                ?: emptyList()
    }

    suspend fun getArtistTopTracks(artistId: String, requirePreview: Boolean): List<TrackResponse> {
        val tracks = spotifyClient.getArtistTopTracks(artistId)

        return tracks.filter { !requirePreview || !it.previewUrl.isNullOrEmpty() }.map { track ->
            TrackResponse(
                    id = track.id,
                    name = track.name,
                    artistName = track.artists.joinToString(", ") { it.name },
                    albumName = track.album.name,
                    imageUrl = track.album.images.firstOrNull()?.url,
                    previewUrl = track.previewUrl,
                    durationMs = track.durationMs
            )
        }
    }
}
