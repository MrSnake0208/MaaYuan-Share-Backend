package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import plus.maa.backend.service.model.OperatorBoxMember
import java.time.LocalDateTime

@Document("maa_operator_box_preset")
@CompoundIndexes(
    CompoundIndex(
        name = "uniq_user_label",
        def = "{'userId': 1, 'label': 1}",
        unique = true,
    ),
    CompoundIndex(
        name = "idx_user_update_time",
        def = "{'userId': 1, 'updateTime': -1}",
    ),
)
data class OperatorBoxPreset(
    @Id
    val id: String? = null,
    val userId: String,
    var label: String,
    var members: List<OperatorBoxMember>,
    val schemaVersion: Int,
    val createTime: LocalDateTime,
    var updateTime: LocalDateTime,
    @Version
    var revision: Long? = null,
)
