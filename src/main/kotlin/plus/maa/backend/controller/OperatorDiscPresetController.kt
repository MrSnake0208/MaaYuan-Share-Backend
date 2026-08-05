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
import plus.maa.backend.controller.request.CommonIdReq
import plus.maa.backend.controller.request.disc.OperatorDiscPresetCreateReq
import plus.maa.backend.controller.request.disc.OperatorDiscPresetUpdateReq
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.disc.OperatorDiscPresetRes
import plus.maa.backend.service.OperatorDiscPresetService

@Tag(name = "OperatorDiscPreset", description = "密探命盘预设相关接口")
@RequireJwt
@RequestMapping("/disc-preset")
@RestController
class OperatorDiscPresetController(
    private val service: OperatorDiscPresetService,
    private val helper: AuthenticationHelper,
) {
    @Operation(summary = "查询当前用户的密探命盘预设")
    @GetMapping("/list")
    fun listDiscPresets(): MaaResult<List<OperatorDiscPresetRes>> = success(service.list(helper.requireUserId()))

    @Operation(summary = "创建密探命盘预设")
    @PostMapping("/create")
    fun createDiscPreset(@RequestBody req: @Valid OperatorDiscPresetCreateReq): MaaResult<OperatorDiscPresetRes> =
        success(service.create(req, helper.requireUserId()))

    @Operation(summary = "更新密探命盘预设")
    @PostMapping("/update")
    fun updateDiscPreset(@RequestBody req: @Valid OperatorDiscPresetUpdateReq): MaaResult<OperatorDiscPresetRes> =
        success(service.update(req, helper.requireUserId()))

    @Operation(summary = "删除密探命盘预设")
    @PostMapping("/delete")
    fun deleteDiscPreset(@RequestBody req: @Valid CommonIdReq<String>): MaaResult<Unit> {
        service.delete(req.id, helper.requireUserId())
        return success()
    }
}
