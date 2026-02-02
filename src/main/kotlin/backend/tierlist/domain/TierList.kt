package backend.tierlist.domain

import backend.auth.domain.User
import jakarta.persistence.*

@Entity
class TierList(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") val user: User,
    @Column(nullable = false) var title: String,
    @Column(columnDefinition = "TEXT") var description: String? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var category: TierCategory,
    var isPublic: Boolean = true
)
