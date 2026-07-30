package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.CopilotShareImageConfig

interface CopilotShareImageConfigRepository : MongoRepository<CopilotShareImageConfig, String> {
    fun findAllByCopilotIdOrderByCardKey(copilotId: Long): List<CopilotShareImageConfig>

    fun findByCopilotIdAndCardKey(copilotId: Long, cardKey: String): CopilotShareImageConfig?
}
