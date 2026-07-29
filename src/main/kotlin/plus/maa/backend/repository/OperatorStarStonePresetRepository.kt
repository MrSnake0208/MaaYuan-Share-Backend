package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.OperatorStarStonePreset
import plus.maa.backend.service.model.StarStonePresetKind

interface OperatorStarStonePresetRepository : MongoRepository<OperatorStarStonePreset, String> {
    fun findAllByUserIdOrderByUpdateTimeDesc(userId: String): List<OperatorStarStonePreset>

    fun findByIdAndUserId(id: String, userId: String): OperatorStarStonePreset?

    fun countByUserId(userId: String): Long

    fun countByUserIdAndOperatorIdAndKind(userId: String, operatorId: String, kind: StarStonePresetKind): Long

    fun existsByUserIdAndOperatorIdAndKindAndLabel(userId: String, operatorId: String, kind: StarStonePresetKind, label: String): Boolean

    fun existsByUserIdAndOperatorIdAndKindAndLabelAndIdNot(
        userId: String,
        operatorId: String,
        kind: StarStonePresetKind,
        label: String,
        id: String,
    ): Boolean
}
