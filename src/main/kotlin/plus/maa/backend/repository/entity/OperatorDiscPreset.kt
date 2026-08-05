package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("maa_operator_disc_preset")
@CompoundIndex(
    name = "uniq_user_operator_label",
    def = "{'userId': 1, 'operatorId': 1, 'label': 1}",
    unique = true,
)
data class OperatorDiscPreset(
    @Id
    val id: String? = null,
    val userId: String,
    val operatorId: String,
    var label: String,
    var selected: List<Int>,
    var confirmed: List<Boolean>,
    val createTime: LocalDateTime,
    var updateTime: LocalDateTime,
)
