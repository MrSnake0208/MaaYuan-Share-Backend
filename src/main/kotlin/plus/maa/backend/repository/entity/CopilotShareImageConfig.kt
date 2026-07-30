package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("maa_copilot_share_image_config")
@CompoundIndex(
    name = "uniq_copilot_card_key",
    def = "{'copilotId': 1, 'cardKey': 1}",
    unique = true,
)
data class CopilotShareImageConfig(
    @Id
    val id: String? = null,
    val copilotId: Long,
    val cardKey: String,
    var schemaVersion: Int,
    var payload: Map<String, Any?>,
    val createTime: LocalDateTime,
    var updateTime: LocalDateTime,
    @Version
    var revision: Long? = null,
)
