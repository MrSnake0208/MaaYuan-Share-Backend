package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.OperatorBoxTrainingConfig

interface OperatorBoxTrainingConfigRepository : MongoRepository<OperatorBoxTrainingConfig, String> {
    fun findAllByUserIdAndBoxIdOrderByUpdateTimeDesc(userId: String, boxId: String): List<OperatorBoxTrainingConfig>

    fun findByUserIdAndBoxIdAndOperatorId(userId: String, boxId: String, operatorId: String): OperatorBoxTrainingConfig?

    fun deleteAllByUserIdAndBoxId(userId: String, boxId: String): Long
}
