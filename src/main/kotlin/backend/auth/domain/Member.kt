package backend.auth.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @Column(unique = true, nullable = false)
    val providerId: String, // Spotify ID or UUID for Guest
    var email: String? = null,
    var displayName: String? = null,
    @Enumerated(EnumType.STRING) val role: Role,
    var lastActiveAt: LocalDateTime = LocalDateTime.now()
)
