package plus.maa.backend.controller.response.disc

import plus.maa.backend.repository.entity.OperatorDiscPreset
import java.time.LocalDateTime

data class OperatorDiscPresetRes(
    val id: String,
    val operatorId: String,
    val label: String,
    val selected: List<Int>,
    val confirmed: List<Boolean>,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
) {
    companion object {
        fun from(entity: OperatorDiscPreset) = OperatorDiscPresetRes(
            id = requireNotNull(entity.id),
            operatorId = entity.operatorId,
            label = entity.label,
            selected = entity.selected,
            confirmed = entity.confirmed,
            createTime = entity.createTime,
            updateTime = entity.updateTime,
        )
    }
}
