package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.OperatorTrainingConfig

interface OperatorTrainingConfigRepository : MongoRepository<OperatorTrainingConfig, String> {
    fun findAllByUserIdOrderByUpdateTimeDesc(userId: String): List<OperatorTrainingConfig>

    fun findByUserIdAndOperatorId(userId: String, operatorId: String): OperatorTrainingConfig?
}
