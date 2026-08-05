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
import plus.maa.backend.controller.request.box.OperatorBoxPresetCreateReq
import plus.maa.backend.controller.request.box.OperatorBoxPresetDeleteReq
import plus.maa.backend.controller.request.box.OperatorBoxPresetUpdateReq
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.box.OperatorBoxPresetRes
import plus.maa.backend.service.OperatorBoxPresetService

@Tag(name = "OperatorBoxPreset", description = "密探阵容预设相关接口")
@RequireJwt
@RequestMapping("/operator-box-preset")
@RestController
class OperatorBoxPresetController(
    private val service: OperatorBoxPresetService,
    private val helper: AuthenticationHelper,
) {
    @Operation(summary = "查询当前用户的密探阵容预设")
    @GetMapping("/list")
    fun listOperatorBoxPresets(): MaaResult<List<OperatorBoxPresetRes>> = success(service.list(helper.requireUserId()))

    @Operation(summary = "创建密探阵容预设")
    @PostMapping("/create")
    fun createOperatorBoxPreset(@RequestBody req: @Valid OperatorBoxPresetCreateReq): MaaResult<OperatorBoxPresetRes> =
        success(service.create(req, helper.requireUserId()))

    @Operation(summary = "更新密探阵容预设")
    @PostMapping("/update")
    fun updateOperatorBoxPreset(@RequestBody req: @Valid OperatorBoxPresetUpdateReq): MaaResult<OperatorBoxPresetRes> =
        success(service.update(req, helper.requireUserId()))

    @Operation(summary = "删除密探阵容预设")
    @PostMapping("/delete")
    fun deleteOperatorBoxPreset(@RequestBody req: @Valid OperatorBoxPresetDeleteReq): MaaResult<Unit> {
        service.delete(req.id, req.expectedRevision, helper.requireUserId())
        return success()
    }
}
