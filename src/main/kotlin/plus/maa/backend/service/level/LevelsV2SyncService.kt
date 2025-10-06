package plus.maa.backend.service.level

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.repository.GithubRepository
import plus.maa.backend.repository.RedisCache
import java.nio.file.Files
import java.nio.file.Path

@Service
class LevelsV2SyncService(
    private val properties: MaaCopilotProperties,
    private val githubRepo: GithubRepository,
    private val redisCache: RedisCache,
    private val cacheManager: CacheManager,
) {
    private val log = KotlinLogging.logger { }
    private val webClient: WebClient = WebClient.builder().build()

    suspend fun syncOnce(): Boolean {
        val cfg = properties.levels
        if (!cfg.enableGithub) {
            log.info { "levels v2 sync disabled; skip" }
            return false
        }
        val token = bearerOrEmpty(cfg.token)
        val dir = cfg.jsonDir.trim('/',' ').ifEmpty { "" }
        val contents = githubRepo.getContents(token, dir)
        val target = contents.firstOrNull { it.isFile && it.name == cfg.jsonFile }
            ?: run {
                log.warn { "levels v2 json not found in contents: dir=${cfg.jsonDir}, file=${cfg.jsonFile}" }
                return false
            }
        val shaKey = "levels:v2:sha"
        val oldSha = redisCache.getCache(shaKey, String::class.java)
        if (oldSha == target.sha) {
            log.info { "levels v2 already up-to-date (sha=$oldSha)" }
            return false
        }
        val downloadUrl = target.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            log.warn { "downloadUrl missing for ${target.name}" }
            return false
        }
        log.info { "downloading levels v2 from $downloadUrl (sha=${target.sha})" }
        val body = webClient.get().uri(downloadUrl).retrieve().bodyToMono(String::class.java).block()
        if (body.isNullOrBlank()) {
            log.warn { "downloaded empty body; abort" }
            return false
        }
        val path = Path.of(cfg.localCache)
        Files.createDirectories(path.parent)
        Files.writeString(path, body)
        // 更新缓存标记并失效 Caffeine 缓存
        redisCache.setData(shaKey, target.sha)
        cacheManager.getCache("arkLevelInfosV2")?.clear()
        log.info { "levels v2 updated -> ${cfg.localCache}; cache cleared" }
        return true
    }

    private fun bearerOrEmpty(token: String): String = if (token.isBlank()) "" else "Bearer $token"
}
