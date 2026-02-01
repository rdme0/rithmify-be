package backend.tierlist.dto.request

import backend.tierlist.domain.TierCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class CreateTierListRequest(
        @field:NotBlank(message = "Title is required") val title: String,
        val description: String?,
        @field:NotNull(message = "Category is required") val category: TierCategory,
        val isPublic: Boolean = true,
        @field:Valid val groups: List<CreateTierGroupRequest>
)

data class CreateTierGroupRequest(
        @field:NotBlank(message = "Label is required") val label: String,
        @field:Pattern(
                regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
                message = "Invalid color format"
        )
        val colorHex: String,
        @field:Min(0) val sortOrder: Int,
        @field:Valid val items: List<CreateTierItemRequest>
)

data class CreateTierItemRequest(
        @field:NotBlank(message = "Spotify ID is required") val spotifyId: String,
        @field:NotBlank(message = "Name is required") val name: String,
        val imageUrl: String?,
        val previewUrl: String?,
        @field:Min(0) val sortOrder: Int
)
