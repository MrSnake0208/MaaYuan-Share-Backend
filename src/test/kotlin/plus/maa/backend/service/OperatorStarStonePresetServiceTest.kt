package plus.maa.backend.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import plus.maa.backend.controller.request.starstone.OperatorStarStonePresetCreateReq
import plus.maa.backend.controller.request.starstone.OperatorStarStonePresetUpdateReq
import plus.maa.backend.repository.OperatorStarStonePresetRepository
import plus.maa.backend.repository.entity.OperatorStarStonePreset
import plus.maa.backend.service.model.StarStonePresetKind
import java.time.LocalDateTime

class OperatorStarStonePresetServiceTest {
    private val repository = mockk<OperatorStarStonePresetRepository>()
    private val service = OperatorStarStonePresetService(repository)

    @Test
    fun `creates a normalized preset for the authenticated user`() {
        every { repository.countByUserId("user-1") } returns 0
        every {
            repository.countByUserIdAndOperatorIdAndKind("user-1", "char_001", StarStonePresetKind.MAIN)
        } returns 0
        every {
            repository.existsByUserIdAndOperatorIdAndKindAndLabel(
                "user-1",
                "char_001",
                StarStonePresetKind.MAIN,
                "常用主星",
            )
        } returns false
        every { repository.insert(any<OperatorStarStonePreset>()) } answers {
            firstArg<OperatorStarStonePreset>().copy(id = "preset-1")
        }

        val result = service.create(
            OperatorStarStonePresetCreateReq(
                operatorId = " char_001 ",
                kind = StarStonePresetKind.MAIN,
                label = " 常用主星 ",
                values = listOf(" 天府 ", "", null),
            ),
            "user-1",
        )

        assertEquals("preset-1", result.id)
        assertEquals("char_001", result.operatorId)
        assertEquals("常用主星", result.label)
        assertEquals(listOf("天府", null, null), result.values)
    }

    @Test
    fun `lists only presets belonging to the authenticated user`() {
        every { repository.findAllByUserIdOrderByUpdateTimeDesc("user-1") } returns listOf(preset())

        val result = service.list("user-1")

        assertEquals(listOf("preset-1"), result.map { it.id })
        verify(exactly = 1) { repository.findAllByUserIdOrderByUpdateTimeDesc("user-1") }
    }

    @Test
    fun `rejects updates when preset is not owned by the user`() {
        every { repository.findByIdAndUserId("preset-1", "user-2") } returns null

        assertThrows(IllegalArgumentException::class.java) {
            service.update(
                OperatorStarStonePresetUpdateReq(id = "preset-1", label = "新名称"),
                "user-2",
            )
        }
    }

    @Test
    fun `rejects duplicate non-any star values`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.create(
                OperatorStarStonePresetCreateReq(
                    operatorId = "char_001",
                    kind = StarStonePresetKind.ASSIST,
                    label = "重复辅星",
                    values = listOf("文昌", "文昌", null),
                ),
                "user-1",
            )
        }

        assertEquals("同一预设不能重复选择星石", exception.message)
    }

    @Test
    fun `rejects creation when the user preset limit is reached`() {
        every { repository.countByUserId("user-1") } returns 200

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.create(
                OperatorStarStonePresetCreateReq(
                    operatorId = "char_001",
                    kind = StarStonePresetKind.MAIN,
                    label = "超限预设",
                    values = listOf("天府", null, null),
                ),
                "user-1",
            )
        }

        assertEquals("每个用户最多只能保存200个星石预设", exception.message)
    }

    @Test
    fun `deletes only a preset owned by the user`() {
        val preset = preset()
        every { repository.findByIdAndUserId("preset-1", "user-1") } returns preset
        every { repository.delete(preset) } just runs

        service.delete("preset-1", "user-1")

        verify(exactly = 1) { repository.delete(preset) }
    }

    private fun preset() = OperatorStarStonePreset(
        id = "preset-1",
        userId = "user-1",
        operatorId = "char_001",
        kind = StarStonePresetKind.MAIN,
        label = "常用主星",
        values = listOf("天府", null, null),
        createTime = LocalDateTime.now(),
        updateTime = LocalDateTime.now(),
    )
}
