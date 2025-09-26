package plus.maa.backend.service.level

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import plus.maa.backend.common.utils.converter.ArkLevelConverter
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.repository.entity.ArkLevel

/**
 * 使用手工维护的关卡列表，取代远程同步的实现。
 */
@Service
class ArkLevelService(
    private val arkLevelConverter: ArkLevelConverter,
) {
    private val log = KotlinLogging.logger { }
    private val manualLevels: List<ArkLevel> = ManualArkLevels.levels
    private val manualLevelInfos: List<ArkLevelInfo> by lazy {
        arkLevelConverter.convert(manualLevels)
    }

    @get:Cacheable("arkLevelInfos")
    val arkLevelInfos: List<ArkLevelInfo>
        get() = manualLevelInfos

    @Cacheable("arkLevel")
    fun findByLevelIdFuzzy(levelId: String): ArkLevel? {
        if (levelId.isBlank()) return null
        return manualLevels.firstOrNull { level -> level.matchesKeyword(levelId) }
    }

    fun queryLevelInfosByKeyword(keyword: String): List<ArkLevelInfo> {
        if (keyword.isBlank()) return emptyList()
        return manualLevelInfos.filter { info ->
            listOf(
                info.levelId,
                info.stageId,
                info.catOne,
                info.catTwo,
                info.catThree,
                info.name,
            ).any { it.containsIgnoreCase(keyword) }
        }
    }

    suspend fun syncLevelData() {
        log.info { "Hand-crafted level set enabled, skip remote synchronization." }
    }

    suspend fun updateActivitiesOpenStatus() {
        log.info { "Hand-crafted level set enabled, skip activities open status update." }
    }

    suspend fun updateCrisisV2OpenStatus() {
        log.info { "Hand-crafted level set enabled, skip crisis open status update." }
    }

    private fun ArkLevel.matchesKeyword(keyword: String): Boolean {
        val targets = listOfNotNull(
            stageId,
            levelId,
            catOne,
            catTwo,
            catThree,
            name,
        )
        return targets.any { it.contains(keyword, ignoreCase = true) }
    }

    private fun String?.containsIgnoreCase(other: String): Boolean = this?.contains(other, ignoreCase = true) ?: false
}
