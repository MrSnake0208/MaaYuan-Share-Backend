package plus.maa.backend.service

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import plus.maa.backend.controller.request.OperatorTrainingConfigSaveReq
import plus.maa.backend.controller.request.box.OperatorBoxTrainingConfigBatchSaveReq
import plus.maa.backend.controller.request.box.OperatorBoxTrainingConfigSaveReq
import plus.maa.backend.controller.response.box.OperatorBoxTrainingConfigRes
import plus.maa.backend.repository.OperatorBoxPresetRepository
import plus.maa.backend.repository.OperatorBoxTrainingConfigRepository
import plus.maa.backend.repository.entity.OperatorBoxTrainingConfig
import plus.maa.backend.service.model.OperatorTrainingDiscConfig
import java.time.LocalDateTime

@Service
class OperatorBoxTrainingConfigService(
    private val repository: OperatorBoxTrainingConfigRepository,
    private val boxRepository: OperatorBoxPresetRepository,
) {
    fun list(boxId: String, userId: String): List<OperatorBoxTrainingConfigRes> {
        val ownedBoxId = requireOwnedBox(boxId, userId)
        return repository.findAllByUserIdAndBoxIdOrderByUpdateTimeDesc(userId, ownedBoxId)
            .map(OperatorBoxTrainingConfigRes::from)
    }

    fun save(req: OperatorBoxTrainingConfigSaveReq, userId: String): OperatorBoxTrainingConfigRes {
        val boxId = requireOwnedBox(req.boxId, userId)
        val normalized = normalize(req.config)
        val now = LocalDateTime.now()
        val entity = upsertEntity(boxId, normalized, userId, now)
        return OperatorBoxTrainingConfigRes.from(saveOrThrowDuplicate(entity))
    }

    fun saveBatch(req: OperatorBoxTrainingConfigBatchSaveReq, userId: String): List<OperatorBoxTrainingConfigRes> {
        val boxId = requireOwnedBox(req.boxId, userId)
        val normalized = req.configs.map(::normalize)
        require(normalized.map(OperatorTrainingConfigSaveReq::operatorId).distinct().size == normalized.size) {
            "同一批次不能重复保存密探练度配置"
        }

        val now = LocalDateTime.now()
        val existing = repository.findAllByUserIdAndBoxIdOrderByUpdateTimeDesc(userId, boxId)
            .associateBy(OperatorBoxTrainingConfig::operatorId)
        val entities = normalized.map { config ->
            toEntity(existing[config.operatorId], boxId, config, userId, now)
        }
        return try {
            repository.saveAll(entities).map(OperatorBoxTrainingConfigRes::from)
        } catch (_: DuplicateKeyException) {
            throw IllegalArgumentException("Box 密探练度配置保存冲突，请重试")
        }
    }

    private fun requireOwnedBox(boxId: String, userId: String): String = boxId.trim().also {
        require(it.isNotEmpty()) { "Box id不能为空" }
        require(boxRepository.findByIdAndUserId(it, userId) != null) { "阵容预设不存在或无权访问" }
    }

    private fun normalize(req: OperatorTrainingConfigSaveReq): OperatorTrainingConfigSaveReq {
        val operatorId = req.operatorId.trim()
        require(operatorId.isNotEmpty()) { "密探id不能为空" }
        validateElite(req.level, req.elite)
        return req.copy(operatorId = operatorId, discs = normalizeDiscs(req.discs))
    }

    private fun upsertEntity(
        boxId: String,
        req: OperatorTrainingConfigSaveReq,
        userId: String,
        now: LocalDateTime,
    ): OperatorBoxTrainingConfig = toEntity(
        repository.findByUserIdAndBoxIdAndOperatorId(userId, boxId, req.operatorId),
        boxId,
        req,
        userId,
        now,
    )

    private fun toEntity(
        existing: OperatorBoxTrainingConfig?,
        boxId: String,
        req: OperatorTrainingConfigSaveReq,
        userId: String,
        now: LocalDateTime,
    ): OperatorBoxTrainingConfig = existing?.apply {
        starLevel = req.starLevel
        level = req.level
        elite = req.elite
        skillLevel = req.skillLevel
        potentiality = req.potentiality
        module = req.module
        skill = req.skill
        attack = req.attack
        hp = req.hp
        discs = req.discs
        updateTime = now
    } ?: OperatorBoxTrainingConfig(
        userId = userId,
        boxId = boxId,
        operatorId = req.operatorId,
        starLevel = req.starLevel,
        level = req.level,
        elite = req.elite,
        skillLevel = req.skillLevel,
        potentiality = req.potentiality,
        module = req.module,
        skill = req.skill,
        attack = req.attack,
        hp = req.hp,
        discs = req.discs,
        createTime = now,
        updateTime = now,
    )

    private fun validateElite(level: Int?, elite: Int?) {
        if (level == null || elite == null) return
        val maxElite = ((level - 1) / 5).coerceIn(0, MAX_ELITE)
        require(elite <= maxElite) { "当前等级最高允许修为$maxElite" }
    }

    private fun normalizeDiscs(discs: OperatorTrainingDiscConfig?): OperatorTrainingDiscConfig? = discs?.let {
        require(it.selected.size == SLOT_COUNT) { "命盘必须包含三个槽位" }
        require(it.confirmed.size == SLOT_COUNT) { "命盘确认状态必须包含三个槽位" }
        val starStones = normalizeStarSlots(it.starStones, "主星")
        val assistStars = normalizeStarSlots(it.assistStars, "辅星")
        it.copy(starStones = starStones, assistStars = assistStars)
    }

    private fun normalizeStarSlots(values: List<String?>, label: String): List<String?> {
        require(values.size == SLOT_COUNT) { "${label}必须包含三个槽位" }
        return values.map { value -> value?.trim()?.takeIf(String::isNotEmpty) }.also { normalized ->
            require(normalized.filterNotNull().all { value -> value.length <= MAX_STAR_NAME_LENGTH }) {
                "${label}名称最长为${MAX_STAR_NAME_LENGTH}个字符"
            }
        }
    }

    private fun saveOrThrowDuplicate(entity: OperatorBoxTrainingConfig): OperatorBoxTrainingConfig = try {
        repository.save(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("Box 密探练度配置保存冲突，请重试")
    }

    companion object {
        private const val MAX_ELITE = 17
        private const val SLOT_COUNT = 3
        private const val MAX_STAR_NAME_LENGTH = 32
    }
}
