package plus.maa.backend.controller.request.box

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import plus.maa.backend.controller.request.OperatorTrainingConfigSaveReq

@Schema(title = "Box 密探练度配置保存请求")
data class OperatorBoxTrainingConfigSaveReq(
    @field:NotBlank(message = "Box id不能为空")
    val boxId: String,
    @field:Valid
    val config: OperatorTrainingConfigSaveReq,
)
