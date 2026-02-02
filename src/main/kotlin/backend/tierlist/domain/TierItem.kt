package backend.tierlist.domain

import jakarta.persistence.*

@Entity
class TierItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_group_id")
    var tierGroup: TierGroup? = null,
    @Column(nullable = false) val spotifyId: String,
    @Column(nullable = false) val name: String,
    val imageUrl: String? = null,
    val previewUrl: String? = null,
    var sortOrder: Int = 0
)
