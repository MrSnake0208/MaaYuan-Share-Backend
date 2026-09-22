package plus.maa.backend.service.level

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import plus.maa.backend.common.utils.converter.ArkLevelConverter
import plus.maa.backend.common.utils.converter.ArkLevelConverterV2
import plus.maa.backend.config.external.Levels
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.controller.response.copilot.ArkLevelInfoV2
import plus.maa.backend.repository.entity.ArkLevel

/**
 * 回归测试：关卡关键字解析不能把“具体关卡标识”当成模糊词，
 * 否则选择某一关会带出同前缀/同子串的其它关卡。
 */
class ArkLevelServiceKeywordTest {

    private val converter = mockk<ArkLevelConverter>()
    private val converterV2 = mockk<ArkLevelConverterV2>()

    private val service: ArkLevelService = run {
        every { converter.convert(any<List<ArkLevel>>()) } answers {
            firstArg<List<ArkLevel>>().map {
                ArkLevelInfo(
                    levelId = it.levelId!!,
                    stageId = it.stageId!!,
                    catOne = it.catOne ?: "",
                    catTwo = it.catTwo ?: "",
                    catThree = it.catThree ?: "",
                    name = it.name ?: "",
                )
            }
        }
        every { converterV2.convert(any<List<ArkLevel>>()) } answers {
            firstArg<List<ArkLevel>>().map {
                ArkLevelInfoV2(
                    levelId = it.levelId!!,
                    stageId = it.stageId!!,
                    catOne = it.catOne ?: "",
                    catTwo = it.catTwo ?: "",
                    catThree = it.catThree ?: "",
                    name = it.name ?: "",
                    endTime = it.endTime,
                )
            }
        }
        ArkLevelService(converter, converterV2, MaaCopilotProperties(levels = Levels(enableGithub = false)))
    }

    @Test
    fun `exact stageId returns only that level`() {
        val stageIds = service.queryLevelInfosByKeyword("yan_").map { it.stageId }
        check(stageIds == listOf("yan_")) { "expected only yan_, got $stageIds" }
    }

    @Test
    fun `stageId prefix does not leak child levels`() {
        val names = service.queryLevelInfosByKeyword("er_qi").map { it.name }
        check(names == listOf("二期")) { "expected only 二期, got $names" }
    }

    @Test
    fun `category keyword still returns the whole category`() {
        val stageIds = service.queryLevelInfosByKeyword("地宫").map { it.stageId }
        check(stageIds.size > 1) { "expected multiple 地宫 levels, got $stageIds" }
    }
}
