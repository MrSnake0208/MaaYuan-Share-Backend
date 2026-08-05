package plus.maa.backend.controller.request.box

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import plus.maa.backend.service.model.OperatorBoxMember

@Schema(title = "密探阵容预设创建请求")
data class OperatorBoxPresetCreateReq(
    @field:NotBlank(message = "预设名称不能为空")
    @field:Size(max = 32, message = "预设名称最长为32个字符")
    val label: String,
    @field:Valid
    @field:Size(min = 1, max = 100, message = "阵容必须包含1到100名密探")
    val members: List<OperatorBoxMember>,
)
