package backend.tierlist.dto.response

import backend.tierlist.domain.TierCategory

data class TierListResponse(
        val id: Long,
        val title: String,
        val description: String?,
        val category: TierCategory,
        val isPublic: Boolean,
        val writer: TierListWriterResponse,
        val groups: List<TierGroupResponse>
)

data class TierListWriterResponse(
        val id: String, // User ID
        val displayName: String?
)

data class TierGroupResponse(
        val id: Long,
        val label: String,
        val colorHex: String,
        val items: List<TierItemResponse>
)

data class TierItemResponse(
        val id: Long,
        val spotifyId: String,
        val name: String,
        val imageUrl: String?,
        val previewUrl: String?
)
