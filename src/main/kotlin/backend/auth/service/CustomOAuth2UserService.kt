package backend.auth.service

import backend.auth.domain.User
import backend.auth.domain.UserRole
import backend.auth.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(private val userRepository: UserRepository) :
        DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val attributes = oAuth2User.attributes
        val spotifyId = attributes["id"] as? String ?: error("Missing Spotify User ID")
        val email = attributes["email"] as? String
        val displayName = attributes["display_name"] as? String

        val user =
                userRepository
                        .findById(spotifyId)
                        .map { existingUser ->
                            existingUser.apply {
                                this.email = email
                                this.displayName = displayName
                                this.lastActiveAt = java.time.LocalDateTime.now()
                            }
                        }
                        .orElseGet {
                            User(
                                    id = spotifyId,
                                    email = email,
                                    displayName = displayName,
                                    role = UserRole.MEMBER
                            )
                        }

        userRepository.save(user)

        return oAuth2User
    }
}
