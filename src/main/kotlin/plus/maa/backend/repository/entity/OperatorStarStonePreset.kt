package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import plus.maa.backend.service.model.StarStonePresetKind
import java.time.LocalDateTime

@Document("maa_operator_star_stone_preset")
@CompoundIndex(
    name = "uniq_user_operator_kind_label",
    def = "{'userId': 1, 'operatorId': 1, 'kind': 1, 'label': 1}",
    unique = true,
)
data class OperatorStarStonePreset(
    @Id
    val id: String? = null,
    val userId: String,
    val operatorId: String,
    val kind: StarStonePresetKind,
    var label: String,
    var values: List<String?>,
    val createTime: LocalDateTime,
    var updateTime: LocalDateTime,
)
