package plus.maa.backend.controller.response.starstone

import plus.maa.backend.repository.entity.OperatorStarStonePreset
import plus.maa.backend.service.model.StarStonePresetKind
import java.time.LocalDateTime

data class OperatorStarStonePresetRes(
    val id: String,
    val operatorId: String,
    val kind: StarStonePresetKind,
    val label: String,
    val values: List<String?>,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
) {
    companion object {
        fun from(entity: OperatorStarStonePreset) = OperatorStarStonePresetRes(
            id = requireNotNull(entity.id),
            operatorId = entity.operatorId,
            kind = entity.kind,
            label = entity.label,
            values = entity.values,
            createTime = entity.createTime,
            updateTime = entity.updateTime,
        )
    }
}
