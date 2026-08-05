package plus.maa.backend.service.model

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class OperatorBoxMember(
    @field:NotBlank(message = "密探标识不能为空")
    @field:Size(max = 64, message = "密探标识最长为64个字符")
    val operatorKey: String,
    @field:Min(value = 0, message = "密探顺序不能小于0")
    val order: Int,
)
