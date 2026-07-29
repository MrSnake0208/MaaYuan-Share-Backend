package plus.maa.backend.controller.request.starstone

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import plus.maa.backend.service.model.StarStonePresetKind

@Schema(title = "密探星石预设创建请求")
data class OperatorStarStonePresetCreateReq(
    @field:NotBlank(message = "密探id不能为空")
    @field:Size(max = 64, message = "密探id最长为64个字符")
    val operatorId: String,
    @field:NotNull(message = "预设类型不能为空")
    val kind: StarStonePresetKind,
    @field:NotBlank(message = "预设名称不能为空")
    @field:Size(max = 32, message = "预设名称最长为32个字符")
    val label: String,
    @field:Size(min = 3, max = 3, message = "星石预设必须包含三个槽位")
    val values: List<String?>,
)
