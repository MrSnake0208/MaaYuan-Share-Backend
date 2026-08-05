package plus.maa.backend.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.Valid
import plus.maa.backend.service.model.OperatorTrainingDiscConfig

@Schema(description = "密探练度配置保存请求")
data class OperatorTrainingConfigSaveReq(
    @field:NotBlank
    @field:Size(max = 64)
    @field:Schema(description = "密探唯一标识")
    val operatorId: String,
    @field:Min(0)
    @field:Max(6)
    @field:Schema(description = "星级；0 表示未设置")
    val starLevel: Int? = null,
    @field:Min(1)
    @field:Max(100)
    @field:Schema(description = "等级")
    val level: Int? = null,
    @field:Min(0)
    @field:Max(17)
    @field:Schema(description = "修为")
    val elite: Int? = null,
    @field:Min(0)
    @field:Max(10)
    @field:Schema(description = "技能等级")
    val skillLevel: Int? = null,
    @field:Min(0)
    @field:Max(6)
    @field:Schema(description = "命潜")
    val potentiality: Int? = null,
    @field:Min(-1)
    @field:Max(4)
    @field:Schema(description = "模组编号")
    val module: Int? = null,
    @field:Min(1)
    @field:Max(3)
    @field:Schema(description = "技能编号")
    val skill: Int? = null,
    @field:Min(0)
    @field:Schema(description = "攻击")
    val attack: Int? = null,
    @field:Min(0)
    @field:Schema(description = "生命")
    val hp: Int? = null,
    @field:Valid
    @field:Schema(description = "三个命盘槽位、主星和辅星")
    val discs: OperatorTrainingDiscConfig? = null,
)
