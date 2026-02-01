package backend.tierlist.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TierListRepository : JpaRepository<TierList, Long> {
    fun findByUserId(userId: String): List<TierList>
}
