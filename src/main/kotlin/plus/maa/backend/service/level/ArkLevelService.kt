package plus.maa.backend.service.level

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import plus.maa.backend.common.utils.converter.ArkLevelConverter
import plus.maa.backend.common.utils.converter.ArkLevelConverterV2
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.controller.response.copilot.ArkLevelInfoV2
import plus.maa.backend.repository.entity.ArkLevel
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.charset.StandardCharsets

/**
 * 使用手工维护的关卡列表，取代远程同步的实现。
 */
@Service
class ArkLevelService(
    private val arkLevelConverter: ArkLevelConverter,
    private val arkLevelConverterV2: ArkLevelConverterV2,
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

    // ---------------- v2: 从 JSON 加载四层级（含 game） ----------------
    private val objectMapper = jacksonObjectMapper()

    private fun loadLevelsFromJson(): List<ArkLevel> {
        return try {
            val resource = javaClass.classLoader.getResourceAsStream("levels/arknights-levels.v2.json")
            if (resource != null) {
                resource.use { input ->
                    val json = input.readBytes().toString(StandardCharsets.UTF_8)
                    val items: List<ArkLevel> = objectMapper.readValue(json)
                    // 默认 game 填充为 “明日方舟”
                    items.onEach { if (it.game.isNullOrBlank()) it.game = "明日方舟" }
                }
            } else {
                log.warn { "levels/arknights-levels.v2.json not found in classpath, fallback to manualLevels" }
                manualLevels.onEach { if (it.game.isNullOrBlank()) it.game = "明日方舟" }
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to load levels from JSON, fallback to manualLevels" }
            manualLevels.onEach { if (it.game.isNullOrBlank()) it.game = "明日方舟" }
        }
    }

    @get:Cacheable("arkLevelInfosV2")
    val arkLevelInfosV2: List<ArkLevelInfoV2>
        get() = arkLevelConverterV2.convert(loadLevelsFromJson())

    fun findLevelInfoV2ByKeyword(keyword: String?): ArkLevelInfoV2? {
        if (keyword.isNullOrBlank()) return null
        val k = keyword.trim()
        val list = arkLevelInfosV2
        // 优先 stageId
        list.firstOrNull { it.stageId.equals(k, ignoreCase = true) }?.let { return it }
        // 其次 levelId
        list.firstOrNull { it.levelId.equals(k, ignoreCase = true) }?.let { return it }
        // 再次 catThree / name（用于诸如 M-1、SW-EV-1 等）
        list.firstOrNull { it.catThree.equals(k, ignoreCase = true) }?.let { return it }
        list.firstOrNull { it.name.equals(k, ignoreCase = true) }?.let { return it }
        return null
    }
}
