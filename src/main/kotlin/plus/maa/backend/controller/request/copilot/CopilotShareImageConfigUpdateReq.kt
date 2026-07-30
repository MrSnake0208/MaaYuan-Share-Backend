package plus.maa.backend.controller.request.copilot

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

data class CopilotShareImageConfigUpdateReq(
    @field:Min(value = 1, message = "配置版本必须大于 0")
    val schemaVersion: Int,
    @field:Min(value = 0, message = "配置修订号不能小于 0")
    val expectedRevision: Long,
    @field:Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
    val payload: Map<String, Any?> = emptyMap(),
)
