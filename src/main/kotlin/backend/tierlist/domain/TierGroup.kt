package backend.tierlist.domain

import jakarta.persistence.*

@Entity
class TierGroup(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "tier_list_id")
        var tierList: TierList? = null,
        @Column(nullable = false) var label: String, // S, A, B
        var colorHex: String,
        var sortOrder: Int
)
