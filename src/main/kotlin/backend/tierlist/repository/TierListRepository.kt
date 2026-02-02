package backend.tierlist.repository

import backend.tierlist.domain.TierList
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TierListRepository : JpaRepository<TierList, Long>
