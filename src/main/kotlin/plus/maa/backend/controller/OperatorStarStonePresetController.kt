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
import plus.maa.backend.controller.request.starstone.OperatorStarStonePresetCreateReq
import plus.maa.backend.controller.request.starstone.OperatorStarStonePresetUpdateReq
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.starstone.OperatorStarStonePresetRes
import plus.maa.backend.service.OperatorStarStonePresetService

@Tag(name = "OperatorStarStonePreset", description = "密探星石预设相关接口")
@RequireJwt
@RequestMapping("/star-stone-preset")
@RestController
class OperatorStarStonePresetController(
    private val service: OperatorStarStonePresetService,
    private val helper: AuthenticationHelper,
) {
    @Operation(summary = "查询当前用户的密探星石预设")
    @GetMapping("/list")
    fun listPresets(): MaaResult<List<OperatorStarStonePresetRes>> = success(service.list(helper.requireUserId()))

    @Operation(summary = "创建密探星石预设")
    @PostMapping("/create")
    fun createPreset(@RequestBody req: @Valid OperatorStarStonePresetCreateReq): MaaResult<OperatorStarStonePresetRes> =
        success(service.create(req, helper.requireUserId()))

    @Operation(summary = "更新密探星石预设")
    @PostMapping("/update")
    fun updatePreset(@RequestBody req: @Valid OperatorStarStonePresetUpdateReq): MaaResult<OperatorStarStonePresetRes> =
        success(service.update(req, helper.requireUserId()))

    @Operation(summary = "删除密探星石预设")
    @PostMapping("/delete")
    fun deletePreset(@RequestBody req: @Valid CommonIdReq<String>): MaaResult<Unit> {
        service.delete(req.id, helper.requireUserId())
        return success()
    }
}
