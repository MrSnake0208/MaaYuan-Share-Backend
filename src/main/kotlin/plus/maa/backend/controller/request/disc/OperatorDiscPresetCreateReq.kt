package plus.maa.backend.controller.request.disc

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(title = "密探命盘预设创建请求")
data class OperatorDiscPresetCreateReq(
    @field:NotBlank(message = "密探id不能为空")
    @field:Size(max = 64, message = "密探id最长为64个字符")
    val operatorId: String,
    @field:NotBlank(message = "预设名称不能为空")
    @field:Size(max = 32, message = "预设名称最长为32个字符")
    val label: String,
    @field:Size(min = 3, max = 3, message = "命盘预设必须包含三个槽位")
    val selected: List<Int>,
    @field:Size(min = 3, max = 3, message = "命盘确认状态必须包含三个槽位")
    val confirmed: List<Boolean>,
)
