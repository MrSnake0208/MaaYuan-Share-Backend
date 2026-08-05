package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import plus.maa.backend.service.model.OperatorTrainingDiscConfig
import java.time.LocalDateTime

@Document("maa_operator_box_training_config")
@CompoundIndexes(
    CompoundIndex(
        name = "uniq_user_box_operator_training_config",
        def = "{'userId': 1, 'boxId': 1, 'operatorId': 1}",
        unique = true,
    ),
    CompoundIndex(
        name = "idx_user_box_training_update_time",
        def = "{'userId': 1, 'boxId': 1, 'updateTime': -1}",
    ),
)
data class OperatorBoxTrainingConfig(
    @Id
    val id: String? = null,
    val userId: String,
    val boxId: String,
    val operatorId: String,
    var starLevel: Int?,
    var level: Int?,
    var elite: Int?,
    var skillLevel: Int?,
    var potentiality: Int?,
    var module: Int?,
    var skill: Int?,
    var attack: Int?,
    var hp: Int?,
    var discs: OperatorTrainingDiscConfig?,
    val createTime: LocalDateTime,
    var updateTime: LocalDateTime,
)
