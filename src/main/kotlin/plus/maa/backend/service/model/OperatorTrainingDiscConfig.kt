package plus.maa.backend.service.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "密探的三个命盘与星石槽位")
data class OperatorTrainingDiscConfig(
    @field:Size(min = 3, max = 3)
    val selected: List<Int>,
    @field:Size(min = 3, max = 3)
    val confirmed: List<Boolean>,
    @field:Size(min = 3, max = 3)
    val starStones: List<String?>,
    @field:Size(min = 3, max = 3)
    val assistStars: List<String?>,
)
