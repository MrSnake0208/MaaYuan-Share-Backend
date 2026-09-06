package plus.maa.backend.service.level

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import plus.maa.backend.controller.response.copilot.ArkLevelInfoV2
import plus.maa.backend.repository.entity.ArkLevel

class ArkLevelInfoV2Test {
    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    @Test
    fun `end time survives V2 JSON round trip`() {
        val endTime = "2026-09-01T00:00:00.000+08:00"
        val level: ArkLevel = mapper.readValue(
            """
            {
              "level_id": "level-1",
              "stage_id": "stage-1",
              "cat_one": "活动",
              "cat_two": "活动名称",
              "cat_three": "无",
              "name": "活动名称",
              "end_time": "$endTime"
            }
            """.trimIndent(),
        )

        val info = ArkLevelInfoV2(
            levelId = level.levelId!!,
            stageId = level.stageId!!,
            catOne = level.catOne!!,
            catTwo = level.catTwo!!,
            catThree = level.catThree!!,
            name = level.name!!,
            endTime = level.endTime,
        )

        val roundTripped: ArkLevelInfoV2 = mapper.readValue(mapper.writeValueAsString(info))
        assertEquals(endTime, roundTripped.endTime)
    }
}
