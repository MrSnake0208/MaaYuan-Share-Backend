package plus.maa.backend.controller.response.copilot

import io.swagger.v3.oas.annotations.media.Schema
import plus.maa.backend.repository.entity.CopilotShareImageConfig
import java.time.LocalDateTime

data class CopilotShareImageConfigRes(
    val cardKey: String,
    val schemaVersion: Int,
    val revision: Long,
    @field:Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
    val payload: Map<String, Any?>,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(entity: CopilotShareImageConfig) = CopilotShareImageConfigRes(
            cardKey = entity.cardKey,
            schemaVersion = entity.schemaVersion,
            revision = entity.revision ?: 0,
            payload = entity.payload,
            updatedAt = entity.updateTime,
        )
    }
}
