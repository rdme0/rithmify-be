package backend.tierlist.controller

import backend.tierlist.dto.request.CreateTierListRequest
import backend.tierlist.dto.response.TierListResponse
import backend.tierlist.service.TierListService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.*
import java.net.URI

//@RestController
//@RequestMapping("/api/tier-lists")
//class TierListController(private val tierListService: TierListService) {
//
//    @PostMapping
//    fun createTierList(
//        @AuthenticationPrincipal principal: OAuth2User?,
//        @RequestBody request: CreateTierListRequest
//    ): ResponseEntity<TierListResponse> {
//        // If principal is present, use Spotify ID. If null, Service handles it as Guest (creates
//        // temporary).
//        val userId = principal?.attributes?.get("id") as? String
//
//        val response = tierListService.createTierList(userId, request)
//        return ResponseEntity.created(URI.create("/api/tier-lists/${response.id}")).body(response)
//    }
//
//    @GetMapping("/{id}")
//    fun getTierList(@PathVariable id: Long): ResponseEntity<TierListResponse> {
//        val response = tierListService.getTierList(id)
//        return ResponseEntity.ok(response)
//    }
//}
