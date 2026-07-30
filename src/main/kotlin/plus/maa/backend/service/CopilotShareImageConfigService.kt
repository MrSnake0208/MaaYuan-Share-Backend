package plus.maa.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import plus.maa.backend.controller.request.copilot.CopilotShareImageConfigUpdateReq
import plus.maa.backend.controller.response.copilot.CopilotShareImageConfigRes
import plus.maa.backend.repository.CopilotRepository
import plus.maa.backend.repository.CopilotShareImageConfigRepository
import plus.maa.backend.repository.entity.CopilotShareImageConfig
import java.time.LocalDateTime

@Service
class CopilotShareImageConfigService(
    private val repository: CopilotShareImageConfigRepository,
    private val copilotRepository: CopilotRepository,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) {
    fun list(copilotId: Long): List<CopilotShareImageConfigRes> {
        requireCopilot(copilotId)
        return repository.findAllByCopilotIdOrderByCardKey(copilotId).map(CopilotShareImageConfigRes::from)
    }

    fun update(copilotId: Long, rawCardKey: String, req: CopilotShareImageConfigUpdateReq, userId: String): CopilotShareImageConfigRes {
        val copilot = requireCopilot(copilotId)
        if (copilot.uploaderId != userId && !userService.hasAdminPrivileges(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "您没有权限修改此作业的分享图配置")
        }

        val cardKey = normalizeCardKey(rawCardKey)
        validatePayload(req.payload)
        val current = repository.findByCopilotIdAndCardKey(copilotId, cardKey)
        val currentRevision = current?.revision ?: 0
        if (req.expectedRevision != currentRevision) {
            throw conflict("分享图配置已被更新，请刷新后重试")
        }
        if (current != null && req.schemaVersion < current.schemaVersion) {
            throw conflict("不能使用旧版本配置覆盖新版本配置")
        }

        val now = LocalDateTime.now()
        val entity = current?.apply {
            schemaVersion = req.schemaVersion
            payload = req.payload
            updateTime = now
        } ?: CopilotShareImageConfig(
            copilotId = copilotId,
            cardKey = cardKey,
            schemaVersion = req.schemaVersion,
            payload = req.payload,
            createTime = now,
            updateTime = now,
        )

        return try {
            val saved = if (current == null) repository.insert(entity) else repository.save(entity)
            CopilotShareImageConfigRes.from(saved)
        } catch (_: DuplicateKeyException) {
            throw conflict("分享图配置已被创建，请刷新后重试")
        } catch (_: OptimisticLockingFailureException) {
            throw conflict("分享图配置已被更新，请刷新后重试")
        }
    }

    private fun requireCopilot(copilotId: Long) = copilotRepository.findByCopilotIdAndDeleteIsFalse(copilotId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "作业不存在")

    private fun normalizeCardKey(value: String): String = value.trim().lowercase().also {
        require(CARD_KEY_PATTERN.matches(it)) {
            "图片类型只能包含小写字母、数字和连字符，长度不能超过 $MAX_CARD_KEY_LENGTH"
        }
    }

    private fun validatePayload(payload: Map<String, Any?>) {
        require(objectMapper.writeValueAsBytes(payload).size <= MAX_PAYLOAD_BYTES) {
            "单张分享图配置不能超过 ${MAX_PAYLOAD_BYTES / 1024} KB"
        }

        var entryCount = 0
        fun visit(value: Any?, depth: Int) {
            require(depth <= MAX_PAYLOAD_DEPTH) { "分享图配置嵌套层级不能超过 $MAX_PAYLOAD_DEPTH 层" }
            when (value) {
                null, is Number, is Boolean -> Unit
                is String -> require(value.length <= MAX_STRING_LENGTH) {
                    "分享图配置中的字符串不能超过 $MAX_STRING_LENGTH 个字符"
                }
                is Map<*, *> -> value.forEach { (key, nestedValue) ->
                    require(key is String) { "分享图配置的字段名必须是字符串" }
                    require(key.isNotEmpty() && key.length <= MAX_FIELD_NAME_LENGTH) {
                        "分享图配置的字段名长度必须在 1 到 $MAX_FIELD_NAME_LENGTH 之间"
                    }
                    require(!key.startsWith('$') && !key.contains('.')) {
                        "分享图配置的字段名不能以 $ 开头或包含点号"
                    }
                    entryCount += 1
                    require(entryCount <= MAX_PAYLOAD_ENTRIES) {
                        "分享图配置最多包含 $MAX_PAYLOAD_ENTRIES 个字段"
                    }
                    visit(nestedValue, depth + 1)
                }
                is Iterable<*> -> value.forEach { visit(it, depth + 1) }
                else -> throw IllegalArgumentException("分享图配置包含不支持的数据类型")
            }
        }

        visit(payload, 1)
    }

    private fun conflict(message: String) = ResponseStatusException(HttpStatus.CONFLICT, message)

    companion object {
        private const val MAX_CARD_KEY_LENGTH = 64
        private const val MAX_PAYLOAD_BYTES = 64 * 1024
        private const val MAX_PAYLOAD_DEPTH = 12
        private const val MAX_PAYLOAD_ENTRIES = 2_000
        private const val MAX_FIELD_NAME_LENGTH = 128
        private const val MAX_STRING_LENGTH = 4_096
        private val CARD_KEY_PATTERN = Regex("^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$")
    }
}
