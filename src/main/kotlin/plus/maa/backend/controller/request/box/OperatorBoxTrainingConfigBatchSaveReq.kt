package plus.maa.backend.controller.request.box

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import plus.maa.backend.controller.request.OperatorTrainingConfigSaveReq

@Schema(title = "Box 密探练度配置批量保存请求")
data class OperatorBoxTrainingConfigBatchSaveReq(
    @field:NotBlank(message = "Box id不能为空")
    val boxId: String,
    @field:Valid
    @field:Size(min = 1, max = 100, message = "每次必须保存1到100份密探练度配置")
    val configs: List<OperatorTrainingConfigSaveReq>,
)
