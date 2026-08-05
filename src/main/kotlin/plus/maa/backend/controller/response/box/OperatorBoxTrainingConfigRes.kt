package plus.maa.backend.controller.response.box

import plus.maa.backend.repository.entity.OperatorBoxTrainingConfig
import plus.maa.backend.service.model.OperatorTrainingDiscConfig
import java.time.LocalDateTime

data class OperatorBoxTrainingConfigRes(
    val boxId: String,
    val operatorId: String,
    val starLevel: Int?,
    val level: Int?,
    val elite: Int?,
    val skillLevel: Int?,
    val potentiality: Int?,
    val module: Int?,
    val skill: Int?,
    val attack: Int?,
    val hp: Int?,
    val discs: OperatorTrainingDiscConfig?,
    val updateTime: LocalDateTime,
) {
    companion object {
        fun from(entity: OperatorBoxTrainingConfig) = OperatorBoxTrainingConfigRes(
            boxId = entity.boxId,
            operatorId = entity.operatorId,
            starLevel = entity.starLevel,
            level = entity.level,
            elite = entity.elite,
            skillLevel = entity.skillLevel,
            potentiality = entity.potentiality,
            module = entity.module,
            skill = entity.skill,
            attack = entity.attack,
            hp = entity.hp,
            discs = entity.discs,
            updateTime = entity.updateTime,
        )
    }
}
