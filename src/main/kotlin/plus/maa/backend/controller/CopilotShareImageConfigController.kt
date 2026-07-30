package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.request.copilot.CopilotShareImageConfigUpdateReq
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.copilot.CopilotShareImageConfigRes
import plus.maa.backend.service.CopilotShareImageConfigService

@Tag(name = "CopilotShareImageConfig", description = "作业分享图配置接口")
@RequestMapping("/copilot/{copilotId}/share-image-config")
@RestController
class CopilotShareImageConfigController(
    private val service: CopilotShareImageConfigService,
    private val helper: AuthenticationHelper,
) {
    @Operation(summary = "获取作业的作者分享图配置")
    @GetMapping
    fun listConfigs(@PathVariable copilotId: Long): MaaResult<List<CopilotShareImageConfigRes>> = success(service.list(copilotId))

    @Operation(summary = "保存作业的作者分享图配置")
    @RequireJwt
    @PostMapping("/{cardKey}")
    fun updateConfig(
        @PathVariable copilotId: Long,
        @PathVariable cardKey: String,
        @RequestBody req: @Valid CopilotShareImageConfigUpdateReq,
    ): MaaResult<CopilotShareImageConfigRes> = success(service.update(copilotId, cardKey, req, helper.requireUserId()))
}
