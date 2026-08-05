package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.OperatorDiscPreset

interface OperatorDiscPresetRepository : MongoRepository<OperatorDiscPreset, String> {
    fun findAllByUserIdOrderByUpdateTimeDesc(userId: String): List<OperatorDiscPreset>

    fun findByIdAndUserId(id: String, userId: String): OperatorDiscPreset?

    fun countByUserId(userId: String): Long

    fun countByUserIdAndOperatorId(userId: String, operatorId: String): Long

    fun existsByUserIdAndOperatorIdAndLabel(userId: String, operatorId: String, label: String): Boolean

    fun existsByUserIdAndOperatorIdAndLabelAndIdNot(
        userId: String,
        operatorId: String,
        label: String,
        id: String,
    ): Boolean
}
