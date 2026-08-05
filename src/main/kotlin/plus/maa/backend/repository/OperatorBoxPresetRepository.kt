package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.OperatorBoxPreset

interface OperatorBoxPresetRepository : MongoRepository<OperatorBoxPreset, String> {
    fun findAllByUserIdOrderByUpdateTimeDesc(userId: String): List<OperatorBoxPreset>

    fun findByIdAndUserId(id: String, userId: String): OperatorBoxPreset?

    fun countByUserId(userId: String): Long

    fun existsByUserIdAndLabel(userId: String, label: String): Boolean

    fun existsByUserIdAndLabelAndIdNot(userId: String, label: String, id: String): Boolean
}
