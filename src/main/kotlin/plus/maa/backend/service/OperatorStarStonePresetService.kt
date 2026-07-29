package plus.maa.backend.service

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import plus.maa.backend.controller.request.starstone.OperatorStarStonePresetCreateReq
import plus.maa.backend.controller.request.starstone.OperatorStarStonePresetUpdateReq
import plus.maa.backend.controller.response.starstone.OperatorStarStonePresetRes
import plus.maa.backend.repository.OperatorStarStonePresetRepository
import plus.maa.backend.repository.entity.OperatorStarStonePreset
import java.time.LocalDateTime

@Service
class OperatorStarStonePresetService(
    private val repository: OperatorStarStonePresetRepository,
) {
    fun list(userId: String): List<OperatorStarStonePresetRes> =
        repository.findAllByUserIdOrderByUpdateTimeDesc(userId).map(OperatorStarStonePresetRes::from)

    fun create(req: OperatorStarStonePresetCreateReq, userId: String): OperatorStarStonePresetRes {
        val operatorId = normalizeOperatorId(req.operatorId)
        val label = normalizeLabel(req.label)
        val values = normalizeValues(req.values)

        require(repository.countByUserId(userId) < MAX_PRESETS_PER_USER) {
            "每个用户最多只能保存${MAX_PRESETS_PER_USER}个星石预设"
        }
        require(
            repository.countByUserIdAndOperatorIdAndKind(userId, operatorId, req.kind) < MAX_PRESETS_PER_OPERATOR_KIND,
        ) { "每个密探的同类星石预设最多只能保存${MAX_PRESETS_PER_OPERATOR_KIND}个" }
        require(!repository.existsByUserIdAndOperatorIdAndKindAndLabel(userId, operatorId, req.kind, label)) {
            "同名星石预设已存在"
        }

        val now = LocalDateTime.now()
        val entity = OperatorStarStonePreset(
            userId = userId,
            operatorId = operatorId,
            kind = req.kind,
            label = label,
            values = values,
            createTime = now,
            updateTime = now,
        )
        return OperatorStarStonePresetRes.from(insertOrThrowDuplicate(entity))
    }

    fun update(req: OperatorStarStonePresetUpdateReq, userId: String): OperatorStarStonePresetRes {
        require(req.label != null || req.values != null) { "至少需要更新预设名称或星石槽位" }
        val entity = repository.findByIdAndUserId(req.id, userId)
            ?: throw IllegalArgumentException("星石预设不存在或无权访问")

        req.label?.let { requestedLabel ->
            val label = normalizeLabel(requestedLabel)
            require(
                !repository.existsByUserIdAndOperatorIdAndKindAndLabelAndIdNot(
                    userId,
                    entity.operatorId,
                    entity.kind,
                    label,
                    requireNotNull(entity.id),
                ),
            ) { "同名星石预设已存在" }
            entity.label = label
        }
        req.values?.let { entity.values = normalizeValues(it) }
        entity.updateTime = LocalDateTime.now()

        return OperatorStarStonePresetRes.from(saveOrThrowDuplicate(entity))
    }

    fun delete(id: String, userId: String) {
        val entity = repository.findByIdAndUserId(id, userId)
            ?: throw IllegalArgumentException("星石预设不存在或无权访问")
        repository.delete(entity)
    }

    private fun normalizeOperatorId(value: String): String = value.trim().also {
        require(it.isNotEmpty()) { "密探id不能为空" }
        require(it.length <= MAX_OPERATOR_ID_LENGTH) { "密探id最长为${MAX_OPERATOR_ID_LENGTH}个字符" }
    }

    private fun normalizeLabel(value: String): String = value.trim().also {
        require(it.isNotEmpty()) { "预设名称不能为空" }
        require(it.length <= MAX_LABEL_LENGTH) { "预设名称最长为${MAX_LABEL_LENGTH}个字符" }
    }

    private fun normalizeValues(values: List<String?>): List<String?> {
        require(values.size == SLOT_COUNT) { "星石预设必须包含三个槽位" }
        val normalized = values.map { value -> value?.trim()?.takeIf(String::isNotEmpty) }
        require(normalized.any { it != null }) { "星石预设至少需要包含一个星石" }
        require(normalized.filterNotNull().all { it.length <= MAX_STAR_NAME_LENGTH }) {
            "星石名称最长为${MAX_STAR_NAME_LENGTH}个字符"
        }
        val uniqueValues = normalized.filterNotNull().filterNot { it == ANY_STAR_VALUE }
        require(uniqueValues.distinct().size == uniqueValues.size) { "同一预设不能重复选择星石" }
        return normalized
    }

    private fun insertOrThrowDuplicate(entity: OperatorStarStonePreset): OperatorStarStonePreset = try {
        repository.insert(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("同名星石预设已存在")
    }

    private fun saveOrThrowDuplicate(entity: OperatorStarStonePreset): OperatorStarStonePreset = try {
        repository.save(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("同名星石预设已存在")
    }

    companion object {
        private const val SLOT_COUNT = 3
        private const val MAX_PRESETS_PER_USER = 200
        private const val MAX_PRESETS_PER_OPERATOR_KIND = 20
        private const val MAX_OPERATOR_ID_LENGTH = 64
        private const val MAX_LABEL_LENGTH = 32
        private const val MAX_STAR_NAME_LENGTH = 32
        private const val ANY_STAR_VALUE = "任意"
    }
}
