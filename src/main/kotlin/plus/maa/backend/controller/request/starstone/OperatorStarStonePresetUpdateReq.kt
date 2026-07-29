package plus.maa.backend.controller.request.starstone

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(title = "密探星石预设更新请求")
data class OperatorStarStonePresetUpdateReq(
    @field:NotBlank(message = "预设id不能为空")
    val id: String,
    @field:Size(min = 1, max = 32, message = "预设名称长度必须为1到32个字符")
    val label: String? = null,
    @field:Size(min = 3, max = 3, message = "星石预设必须包含三个槽位")
    val values: List<String?>? = null,
)
