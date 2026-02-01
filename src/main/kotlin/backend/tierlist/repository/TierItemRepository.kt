package backend.tierlist.repository

import backend.tierlist.domain.TierItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TierItemRepository : JpaRepository<TierItem, Long> {
    fun findByTierGroupIdOrderBySortOrderAsc(tierGroupId: Long): List<TierItem>

    // N+1 Solution: Fetch items for multiple groups in one query and order by sortOrder
    fun findByTierGroupIdInOrderBySortOrderAsc(tierGroupIds: List<Long>): List<TierItem>
}
