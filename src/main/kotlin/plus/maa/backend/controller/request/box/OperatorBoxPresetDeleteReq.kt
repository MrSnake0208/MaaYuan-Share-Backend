package plus.maa.backend.controller.request.box

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(title = "密探阵容预设删除请求")
data class OperatorBoxPresetDeleteReq(
    @field:NotBlank(message = "预设id不能为空")
    val id: String,
    @field:Min(value = 0, message = "预设修订号不能小于0")
    val expectedRevision: Long,
)
