package plus.maa.backend.service

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import plus.maa.backend.controller.request.OperatorTrainingConfigSaveReq
import plus.maa.backend.controller.response.OperatorTrainingConfigRes
import plus.maa.backend.repository.OperatorTrainingConfigRepository
import plus.maa.backend.repository.entity.OperatorTrainingConfig
import plus.maa.backend.service.model.OperatorTrainingDiscConfig
import java.time.LocalDateTime

@Service
class OperatorTrainingConfigService(
    private val repository: OperatorTrainingConfigRepository,
) {
    fun list(userId: String): List<OperatorTrainingConfigRes> =
        repository.findAllByUserIdOrderByUpdateTimeDesc(userId).map(OperatorTrainingConfigRes::from)

    fun save(req: OperatorTrainingConfigSaveReq, userId: String): OperatorTrainingConfigRes {
        val operatorId = req.operatorId.trim()
        require(operatorId.isNotEmpty()) { "密探id不能为空" }
        validateElite(req.level, req.elite)
        val discs = normalizeDiscs(req.discs)

        val now = LocalDateTime.now()
        val entity = repository.findByUserIdAndOperatorId(userId, operatorId)?.apply {
            starLevel = req.starLevel
            level = req.level
            elite = req.elite
            skillLevel = req.skillLevel
            potentiality = req.potentiality
            module = req.module
            skill = req.skill
            attack = req.attack
            hp = req.hp
            this.discs = discs
            updateTime = now
        } ?: OperatorTrainingConfig(
            userId = userId,
            operatorId = operatorId,
            starLevel = req.starLevel,
            level = req.level,
            elite = req.elite,
            skillLevel = req.skillLevel,
            potentiality = req.potentiality,
            module = req.module,
            skill = req.skill,
            attack = req.attack,
            hp = req.hp,
            discs = discs,
            createTime = now,
            updateTime = now,
        )

        return OperatorTrainingConfigRes.from(saveOrThrowDuplicate(entity))
    }

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

    private fun saveOrThrowDuplicate(entity: OperatorTrainingConfig): OperatorTrainingConfig = try {
        repository.save(entity)
    } catch (_: DuplicateKeyException) {
        throw IllegalArgumentException("密探练度配置保存冲突，请重试")
    }

    companion object {
        private const val MAX_ELITE = 17
        private const val SLOT_COUNT = 3
        private const val MAX_STAR_NAME_LENGTH = 32
    }
}
