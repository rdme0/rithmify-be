package backend.tierlist.service

import backend.auth.domain.User
import backend.auth.domain.UserRole
import backend.auth.exception.UserNotFoundException
import backend.auth.repository.UserRepository
import backend.common.exception.server.InternalServerException
import backend.tierlist.domain.TierGroup
import backend.tierlist.domain.TierItem
import backend.tierlist.domain.TierList
import backend.tierlist.dto.request.CreateTierListRequest
import backend.tierlist.dto.response.TierGroupResponse
import backend.tierlist.dto.response.TierItemResponse
import backend.tierlist.dto.response.TierListResponse
import backend.tierlist.dto.response.TierListWriterResponse
import backend.tierlist.exception.TierListNotFoundException
import backend.tierlist.repository.TierGroupRepository
import backend.tierlist.repository.TierItemRepository
import backend.tierlist.repository.TierListRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class TierListService(
    private val tierListRepository: TierListRepository,
    private val tierGroupRepository: TierGroupRepository,
    private val tierItemRepository: TierItemRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createTierList(userId: String?, request: CreateTierListRequest): TierListResponse {
        val user = resolveUser(userId)

        // 1. Save TierList
        val tierList = TierList(
            user = user,
            title = request.title,
            description = request.description,
            category = request.category,
            isPublic = request.isPublic
        )
        val savedTierList = tierListRepository.save(tierList)

        // 2. Save Groups and Items
        request.groups.forEach { groupReq ->
            val tierGroup = TierGroup(
                tierList = savedTierList,
                label = groupReq.label,
                colorHex = groupReq.colorHex,
                sortOrder = groupReq.sortOrder
            )
            val savedGroup = tierGroupRepository.save(tierGroup)

            groupReq.items.forEach { itemReq ->
                val tierItem = TierItem(
                    tierGroup = savedGroup,
                    spotifyId = itemReq.spotifyId,
                    name = itemReq.name,
                    imageUrl = itemReq.imageUrl,
                    previewUrl = itemReq.previewUrl,
                    sortOrder = itemReq.sortOrder
                )
                tierItemRepository.save(tierItem)
            }
        }

        return getTierList(savedTierList.id!!)
    }

    fun getTierList(id: Long): TierListResponse {
        val tierList = tierListRepository.findByIdOrNull(id) ?: throw TierListNotFoundException()

        // 1. Fetch all groups
        val groups = tierGroupRepository.findByTierListIdOrderBySortOrderAsc(id)

        // 2. Fetch all items for these groups (Batch Fetch)
        val groupIds = groups.mapNotNull { it.id }
        val allItems = if (groupIds.isNotEmpty()) {
            tierItemRepository.findByTierGroupIdInOrderBySortOrderAsc(groupIds)
        } else {
            emptyList()
        }

        // 3. Group items by groupId in memory
        val itemsByGroupId = allItems.groupBy { it.tierGroup?.id }

        val groupResponses = groups.map { group ->
            val items = itemsByGroupId[group.id] ?: emptyList()

            TierGroupResponse(
                id = group.id!!,
                label = group.label,
                colorHex = group.colorHex,
                items =
                    items.map { item ->
                        TierItemResponse(
                            id = item.id!!,
                            spotifyId = item.spotifyId,
                            name = item.name,
                            imageUrl = item.imageUrl,
                            previewUrl = item.previewUrl
                        )
                    }
            )
        }

        return TierListResponse(
            id = tierList.id!!,
            title = tierList.title,
            description = tierList.description,
            category = tierList.category,
            isPublic = tierList.isPublic,
            writer =
                TierListWriterResponse(
                    id = tierList.user.id
                        ?: throw InternalServerException(
                            IllegalStateException("User ID missing")
                        ),
                    displayName = tierList.user.displayName
                ),
            groups = groupResponses
        )
    }

    private fun resolveUser(userId: String?): User {
        return if (userId != null) {
            userRepository.findByProviderId(userId) ?: throw UserNotFoundException()
        } else {
            val guestId = "guest-${UUID.randomUUID()}"
            val guestUser = User(providerId = guestId, role = UserRole.GUEST)
            userRepository.save(guestUser)
        }
    }
}
