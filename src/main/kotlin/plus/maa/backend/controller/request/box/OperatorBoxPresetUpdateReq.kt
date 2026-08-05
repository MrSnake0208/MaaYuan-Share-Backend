package plus.maa.backend.controller.request.box

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import plus.maa.backend.service.model.OperatorBoxMember

@Schema(title = "密探阵容预设更新请求")
data class OperatorBoxPresetUpdateReq(
    @field:NotBlank(message = "预设id不能为空")
    val id: String,
    @field:Min(value = 0, message = "预设修订号不能小于0")
    val expectedRevision: Long,
    @field:Size(min = 1, max = 32, message = "预设名称长度必须为1到32个字符")
    val label: String? = null,
    @field:Valid
    @field:Size(min = 1, max = 100, message = "阵容必须包含1到100名密探")
    val members: List<OperatorBoxMember>? = null,
)
