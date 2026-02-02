package backend.auth.service

import backend.auth.domain.Member
import backend.auth.domain.Role
import backend.auth.repository.MemberRepository
import backend.common.exception.server.InternalServerException
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(private val memberRepository: MemberRepository) :
        DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val attributes = oAuth2User.attributes
        val spotifyId =
                attributes["id"] as? String
                        ?: throw InternalServerException(
                                IllegalArgumentException("Missing Spotify User ID")
                        )
        val email = attributes["email"] as? String
        val displayName = attributes["display_name"] as? String

        val member =
                memberRepository.findByProviderId(spotifyId)?.apply {
                    this.email = email
                    this.displayName = displayName
                    this.lastActiveAt = java.time.LocalDateTime.now()
                }
                        ?: Member(
                                providerId = spotifyId,
                                email = email,
                                displayName = displayName,
                                role = Role.MEMBER
                        )

        memberRepository.save(member)

        return oAuth2User
    }
}
