package backend.tierlist.repository

import backend.tierlist.domain.TierGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TierGroupRepository : JpaRepository<TierGroup, Long> {
    fun findByTierListIdOrderBySortOrderAsc(tierListId: Long): List<TierGroup>
}
