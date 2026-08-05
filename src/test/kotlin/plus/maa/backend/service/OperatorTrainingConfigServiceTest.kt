package plus.maa.backend.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import plus.maa.backend.controller.request.OperatorTrainingConfigSaveReq
import plus.maa.backend.repository.OperatorTrainingConfigRepository
import plus.maa.backend.repository.entity.OperatorTrainingConfig
import plus.maa.backend.service.model.OperatorTrainingDiscConfig
import java.time.LocalDateTime

class OperatorTrainingConfigServiceTest {
    private val repository = mockk<OperatorTrainingConfigRepository>()
    private val service = OperatorTrainingConfigService(repository)

    @Test
    fun `creates a normalized config for the authenticated user`() {
        every { repository.findByUserIdAndOperatorId("user-1", "char_001") } returns null
        every { repository.save(any<OperatorTrainingConfig>()) } answers {
            firstArg<OperatorTrainingConfig>().copy(id = "config-1")
        }

        val result = service.save(configReq(operatorId = " char_001 "), "user-1")

        assertEquals("char_001", result.operatorId)
        assertEquals(5, result.starLevel)
        assertEquals(60, result.level)
        assertEquals(10, result.elite)
        assertEquals(listOf(1, 0, 3), result.discs?.selected)
        assertEquals(listOf("天府", null, "巨门"), result.discs?.starStones)
    }

    @Test
    fun `updates only the config belonging to the authenticated user`() {
        val existing = config(userId = "user-1")
        every { repository.findByUserIdAndOperatorId("user-1", "char_001") } returns existing
        every { repository.save(existing) } returns existing

        val result = service.save(configReq(level = 70, elite = 12), "user-1")

        assertEquals(70, result.level)
        assertEquals(12, result.elite)
        verify(exactly = 1) { repository.findByUserIdAndOperatorId("user-1", "char_001") }
    }

    @Test
    fun `lists only configs belonging to the authenticated user`() {
        every { repository.findAllByUserIdOrderByUpdateTimeDesc("user-1") } returns listOf(config())

        val result = service.list("user-1")

        assertEquals(listOf("char_001"), result.map { it.operatorId })
        verify(exactly = 1) { repository.findAllByUserIdOrderByUpdateTimeDesc("user-1") }
    }

    @Test
    fun `rejects elite above the level cap`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.save(configReq(level = 5, elite = 1), "user-1")
        }

        assertEquals("当前等级最高允许修为0", exception.message)
    }

    @Test
    fun `rejects an incomplete disc slot list`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.save(
                configReq().copy(discs = configReq().discs?.copy(selected = listOf(1, 2))),
                "user-1",
            )
        }

        assertEquals("命盘必须包含三个槽位", exception.message)
    }

    private fun configReq(
        operatorId: String = "char_001",
        level: Int = 60,
        elite: Int = 10,
    ) = OperatorTrainingConfigSaveReq(
        operatorId = operatorId,
        starLevel = 5,
        level = level,
        elite = elite,
        skillLevel = 10,
        potentiality = 6,
        module = 1,
        skill = 2,
        attack = 900,
        hp = 3000,
        discs = OperatorTrainingDiscConfig(
            selected = listOf(1, 0, 3),
            confirmed = listOf(true, true, true),
            starStones = listOf(" 天府 ", "", "巨门"),
            assistStars = listOf("红鸾", null, "天魁"),
        ),
    )

    private fun config(userId: String = "user-1") = OperatorTrainingConfig(
        id = "config-1",
        userId = userId,
        operatorId = "char_001",
        starLevel = 5,
        level = 60,
        elite = 10,
        skillLevel = 10,
        potentiality = 6,
        module = 1,
        skill = 2,
        attack = 900,
        hp = 3000,
        discs = OperatorTrainingDiscConfig(
            selected = listOf(1, 0, 3),
            confirmed = listOf(true, true, true),
            starStones = listOf("天府", null, "巨门"),
            assistStars = listOf("红鸾", null, "天魁"),
        ),
        createTime = LocalDateTime.now(),
        updateTime = LocalDateTime.now(),
    )
}
