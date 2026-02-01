package backend.auth.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class User(
        @Id val id: String, // Spotify ID or UUID for Guest
        var email: String? = null,
        var displayName: String? = null,
        @Enumerated(EnumType.STRING) val role: UserRole,
        var lastActiveAt: LocalDateTime = LocalDateTime.now()
)
