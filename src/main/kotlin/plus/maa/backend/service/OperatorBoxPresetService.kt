package plus.maa.backend.service

import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import plus.maa.backend.controller.request.box.OperatorBoxPresetCreateReq
import plus.maa.backend.controller.request.box.OperatorBoxPresetUpdateReq
import plus.maa.backend.controller.response.box.OperatorBoxPresetRes
import plus.maa.backend.repository.OperatorBoxPresetRepository
import plus.maa.backend.repository.entity.OperatorBoxPreset
import plus.maa.backend.service.model.OperatorBoxMember
import java.time.LocalDateTime

@Service
class OperatorBoxPresetService(
    private val repository: OperatorBoxPresetRepository,
) {
    fun list(userId: String): List<OperatorBoxPresetRes> =
        repository.findAllByUserIdOrderByUpdateTimeDesc(userId).map(OperatorBoxPresetRes::from)

    fun create(req: OperatorBoxPresetCreateReq, userId: String): OperatorBoxPresetRes {
        val label = normalizeLabel(req.label)
        val members = normalizeMembers(req.members)

        require(repository.countByUserId(userId) < MAX_PRESETS_PER_USER) {
            "每个用户最多只能保存${MAX_PRESETS_PER_USER}个阵容预设"
        }
        require(!repository.existsByUserIdAndLabel(userId, label)) {
            "同名阵容预设已存在"
        }

        val now = LocalDateTime.now()
        val entity = OperatorBoxPreset(
            userId = userId,
            label = label,
            members = members,
            schemaVersion = CURRENT_SCHEMA_VERSION,
            createTime = now,
            updateTime = now,
        )
        return OperatorBoxPresetRes.from(insertOrThrowDuplicate(entity))
    }

    fun update(req: OperatorBoxPresetUpdateReq, userId: String): OperatorBoxPresetRes {
        require(req.label != null || req.members != null) {
            "至少需要更新预设名称或阵容成员"
        }
        val entity = repository.findByIdAndUserId(req.id, userId)
            ?: throw IllegalArgumentException("阵容预设不存在或无权访问")
        requireCurrentRevision(entity, req.expectedRevision)

        req.label?.let { requestedLabel ->
            val label = normalizeLabel(requestedLabel)
            require(!repository.existsByUserIdAndLabelAndIdNot(userId, label, requireNotNull(entity.id))) {
                "同名阵容预设已存在"
            }
            entity.label = label
        }
        req.members?.let { entity.members = normalizeMembers(it) }
        entity.updateTime = LocalDateTime.now()

        return OperatorBoxPresetRes.from(saveOrThrowConflict(entity))
    }

    fun delete(id: String, expectedRevision: Long, userId: String) {
        val entity = repository.findByIdAndUserId(id, userId)
            ?: throw IllegalArgumentException("阵容预设不存在或无权访问")
        requireCurrentRevision(entity, expectedRevision)
        try {
            repository.delete(entity)
        } catch (_: OptimisticLockingFailureException) {
            throw conflict("阵容预设已被更新，请刷新后重试")
        }
    }

    private fun normalizeLabel(value: String): String = value.trim().also {
        require(it.isNotEmpty()) { "预设名称不能为空" }
        require(it.length <= MAX_LABEL_LENGTH) { "预设名称最长为${MAX_LABEL_LENGTH}个字符" }
    }

    private fun normalizeMembers(values: List<OperatorBoxMember>): List<OperatorBoxMember> {
        require(values.isNotEmpty()) { "阵容至少需要包含一名密探" }
        require(values.size <= MAX_MEMBERS_PER_PRESET) {
            "每个阵容最多只能包含${MAX_MEMBERS_PER_PRESET}名密探"
        }
        val normalizedKeys = values
            .sortedBy(OperatorBoxMember::order)
            .map { member ->
                member.operatorKey.trim().also { key ->
                    require(key.isNotEmpty()) { "密探标识不能为空" }
                    require(key.length <= MAX_OPERATOR_KEY_LENGTH) {
                        "密探标识最长为${MAX_OPERATOR_KEY_LENGTH}个字符"
                    }
                }
            }
        require(normalizedKeys.distinct().size == normalizedKeys.size) {
            "同一阵容不能重复包含密探"
        }
        return normalizedKeys.mapIndexed { index, key -> OperatorBoxMember(key, index) }
    }

    private fun requireCurrentRevision(entity: OperatorBoxPreset, expectedRevision: Long) {
        if (expectedRevision != (entity.revision ?: 0)) {
            throw conflict("阵容预设已被更新，请刷新后重试")
        }
    }

    private fun insertOrThrowDuplicate(entity: OperatorBoxPreset): OperatorBoxPreset = try {
        repository.insert(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("同名阵容预设已存在")
    }

    private fun saveOrThrowConflict(entity: OperatorBoxPreset): OperatorBoxPreset = try {
        repository.save(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("同名阵容预设已存在")
    } catch (_: OptimisticLockingFailureException) {
        throw conflict("阵容预设已被更新，请刷新后重试")
    }

    private fun conflict(message: String) = ResponseStatusException(HttpStatus.CONFLICT, message)

    companion object {
        private const val CURRENT_SCHEMA_VERSION = 1
        private const val MAX_PRESETS_PER_USER = 100
        private const val MAX_MEMBERS_PER_PRESET = 100
        private const val MAX_OPERATOR_KEY_LENGTH = 64
        private const val MAX_LABEL_LENGTH = 32
    }
}
