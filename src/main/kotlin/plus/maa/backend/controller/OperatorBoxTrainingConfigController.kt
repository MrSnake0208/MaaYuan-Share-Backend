package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.request.box.OperatorBoxTrainingConfigBatchSaveReq
import plus.maa.backend.controller.request.box.OperatorBoxTrainingConfigSaveReq
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.box.OperatorBoxTrainingConfigRes
import plus.maa.backend.service.OperatorBoxTrainingConfigService

@Tag(name = "OperatorBoxTrainingConfig", description = "Box 密探练度配置相关接口")
@RequireJwt
@RequestMapping("/operator-box-training-config")
@RestController
class OperatorBoxTrainingConfigController(
    private val service: OperatorBoxTrainingConfigService,
    private val helper: AuthenticationHelper,
) {
    @Operation(summary = "查询当前用户指定 Box 的密探练度配置")
    @GetMapping("/list")
    fun listOperatorBoxTrainingConfigs(@RequestParam boxId: String): MaaResult<List<OperatorBoxTrainingConfigRes>> =
        success(service.list(boxId, helper.requireUserId()))

    @Operation(summary = "保存当前用户指定 Box 的单个密探练度配置")
    @PostMapping("/save")
    fun saveOperatorBoxTrainingConfig(
        @RequestBody req: @Valid OperatorBoxTrainingConfigSaveReq,
    ): MaaResult<OperatorBoxTrainingConfigRes> =
        success(service.save(req, helper.requireUserId()))

    @Operation(summary = "批量保存当前用户指定 Box 的密探练度配置")
    @PostMapping("/save-batch")
    fun saveOperatorBoxTrainingConfigs(
        @RequestBody req: @Valid OperatorBoxTrainingConfigBatchSaveReq,
    ): MaaResult<List<OperatorBoxTrainingConfigRes>> = success(service.saveBatch(req, helper.requireUserId()))
}
