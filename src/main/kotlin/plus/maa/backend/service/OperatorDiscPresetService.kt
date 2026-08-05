package plus.maa.backend.service

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import plus.maa.backend.controller.request.disc.OperatorDiscPresetCreateReq
import plus.maa.backend.controller.request.disc.OperatorDiscPresetUpdateReq
import plus.maa.backend.controller.response.disc.OperatorDiscPresetRes
import plus.maa.backend.repository.OperatorDiscPresetRepository
import plus.maa.backend.repository.entity.OperatorDiscPreset
import java.time.LocalDateTime
import kotlin.math.abs

@Service
class OperatorDiscPresetService(
    private val repository: OperatorDiscPresetRepository,
) {
    fun list(userId: String): List<OperatorDiscPresetRes> =
        repository.findAllByUserIdOrderByUpdateTimeDesc(userId).map(OperatorDiscPresetRes::from)

    fun create(req: OperatorDiscPresetCreateReq, userId: String): OperatorDiscPresetRes {
        val operatorId = normalizeOperatorId(req.operatorId)
        val label = normalizeLabel(req.label)
        val (selected, confirmed) = normalizeSlots(req.selected, req.confirmed)

        require(repository.countByUserId(userId) < MAX_PRESETS_PER_USER) {
            "每个用户最多只能保存${MAX_PRESETS_PER_USER}个命盘预设"
        }
        require(repository.countByUserIdAndOperatorId(userId, operatorId) < MAX_PRESETS_PER_OPERATOR) {
            "每个密探最多只能保存${MAX_PRESETS_PER_OPERATOR}个命盘预设"
        }
        require(!repository.existsByUserIdAndOperatorIdAndLabel(userId, operatorId, label)) {
            "同名命盘预设已存在"
        }

        val now = LocalDateTime.now()
        val entity = OperatorDiscPreset(
            userId = userId,
            operatorId = operatorId,
            label = label,
            selected = selected,
            confirmed = confirmed,
            createTime = now,
            updateTime = now,
        )
        return OperatorDiscPresetRes.from(insertOrThrowDuplicate(entity))
    }

    fun update(req: OperatorDiscPresetUpdateReq, userId: String): OperatorDiscPresetRes {
        require(req.label != null || req.selected != null || req.confirmed != null) {
            "至少需要更新预设名称或命盘槽位"
        }
        val entity = repository.findByIdAndUserId(req.id, userId)
            ?: throw IllegalArgumentException("命盘预设不存在或无权访问")

        req.label?.let { requestedLabel ->
            val label = normalizeLabel(requestedLabel)
            require(
                !repository.existsByUserIdAndOperatorIdAndLabelAndIdNot(
                    userId,
                    entity.operatorId,
                    label,
                    requireNotNull(entity.id),
                ),
            ) { "同名命盘预设已存在" }
            entity.label = label
        }
        if (req.selected != null || req.confirmed != null) {
            val (selected, confirmed) = normalizeSlots(
                req.selected ?: entity.selected,
                req.confirmed ?: entity.confirmed,
            )
            entity.selected = selected
            entity.confirmed = confirmed
        }
        entity.updateTime = LocalDateTime.now()

        return OperatorDiscPresetRes.from(saveOrThrowDuplicate(entity))
    }

    fun delete(id: String, userId: String) {
        val entity = repository.findByIdAndUserId(id, userId)
            ?: throw IllegalArgumentException("命盘预设不存在或无权访问")
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

    private fun normalizeSlots(selected: List<Int>, confirmed: List<Boolean>): Pair<List<Int>, List<Boolean>> {
        require(selected.size == SLOT_COUNT) { "命盘预设必须包含三个槽位" }
        require(confirmed.size == SLOT_COUNT) { "命盘确认状态必须包含三个槽位" }
        require(confirmed.any { it }) { "命盘预设至少需要包含一个已选择槽位" }
        require(selected.zip(confirmed).all { (value, isConfirmed) -> isConfirmed || value == 0 }) {
            "未确认的命盘槽位必须为空"
        }
        val discIds = selected.filter { it != 0 }.map { abs(it.toLong()) }
        require(discIds.distinct().size == discIds.size) { "同一预设不能重复选择命盘" }
        return selected.toList() to confirmed.toList()
    }

    private fun insertOrThrowDuplicate(entity: OperatorDiscPreset): OperatorDiscPreset = try {
        repository.insert(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("同名命盘预设已存在")
    }

    private fun saveOrThrowDuplicate(entity: OperatorDiscPreset): OperatorDiscPreset = try {
        repository.save(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("同名命盘预设已存在")
    }

    companion object {
        private const val SLOT_COUNT = 3
        private const val MAX_PRESETS_PER_USER = 200
        private const val MAX_PRESETS_PER_OPERATOR = 20
        private const val MAX_OPERATOR_ID_LENGTH = 64
        private const val MAX_LABEL_LENGTH = 32
    }
}
