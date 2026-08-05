package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.request.OperatorTrainingConfigSaveReq
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.OperatorTrainingConfigRes
import plus.maa.backend.service.OperatorTrainingConfigService

@Tag(name = "OperatorTrainingConfig", description = "密探练度配置相关接口")
@RequireJwt
@RequestMapping("/operator-training-config")
@RestController
class OperatorTrainingConfigController(
    private val service: OperatorTrainingConfigService,
    private val helper: AuthenticationHelper,
) {
    @Operation(summary = "查询当前用户的密探练度配置")
    @GetMapping("/list")
    fun listConfigs(): MaaResult<List<OperatorTrainingConfigRes>> = success(service.list(helper.requireUserId()))

    @Operation(summary = "保存当前用户的密探练度配置")
    @PostMapping("/save")
    fun saveConfig(@RequestBody req: @Valid OperatorTrainingConfigSaveReq): MaaResult<OperatorTrainingConfigRes> =
        success(service.save(req, helper.requireUserId()))
}
