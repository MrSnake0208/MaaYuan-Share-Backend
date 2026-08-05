package plus.maa.backend.controller.response.box

import plus.maa.backend.repository.entity.OperatorBoxPreset
import plus.maa.backend.service.model.OperatorBoxMember
import java.time.LocalDateTime

data class OperatorBoxPresetRes(
    val id: String,
    val label: String,
    val members: List<OperatorBoxMember>,
    val schemaVersion: Int,
    val revision: Long,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
) {
    companion object {
        fun from(entity: OperatorBoxPreset) = OperatorBoxPresetRes(
            id = requireNotNull(entity.id),
            label = entity.label,
            members = entity.members,
            schemaVersion = entity.schemaVersion,
            revision = entity.revision ?: 0,
            createTime = entity.createTime,
            updateTime = entity.updateTime,
        )
    }
}
