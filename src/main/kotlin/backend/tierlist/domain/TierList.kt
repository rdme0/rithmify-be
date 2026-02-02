package backend.tierlist.domain

import backend.auth.domain.Member
import jakarta.persistence.*

@Entity
class TierList(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id") val member: Member,
        @Column(nullable = false) var title: String,
        @Column(columnDefinition = "TEXT") var description: String? = null,
        @Enumerated(EnumType.STRING) @Column(nullable = false) var category: TierCategory,
        var isPublic: Boolean = true
)
