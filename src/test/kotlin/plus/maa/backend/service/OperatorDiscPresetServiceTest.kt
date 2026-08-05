package plus.maa.backend.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import plus.maa.backend.controller.request.disc.OperatorDiscPresetCreateReq
import plus.maa.backend.controller.request.disc.OperatorDiscPresetUpdateReq
import plus.maa.backend.repository.OperatorDiscPresetRepository
import plus.maa.backend.repository.entity.OperatorDiscPreset
import java.time.LocalDateTime

class OperatorDiscPresetServiceTest {
    private val repository = mockk<OperatorDiscPresetRepository>()
    private val service = OperatorDiscPresetService(repository)

    @Test
    fun `creates a normalized preset for the authenticated user`() {
        every { repository.countByUserId("user-1") } returns 0
        every { repository.countByUserIdAndOperatorId("user-1", "char_001") } returns 0
        every { repository.existsByUserIdAndOperatorIdAndLabel("user-1", "char_001", "常用命盘") } returns false
        every { repository.insert(any<OperatorDiscPreset>()) } answers {
            firstArg<OperatorDiscPreset>().copy(id = "preset-1")
        }

        val result = service.create(
            OperatorDiscPresetCreateReq(
                operatorId = " char_001 ",
                label = " 常用命盘 ",
                selected = listOf(1, 0, -3),
                confirmed = listOf(true, true, true),
            ),
            "user-1",
        )

        assertEquals("preset-1", result.id)
        assertEquals("char_001", result.operatorId)
        assertEquals("常用命盘", result.label)
        assertEquals(listOf(1, 0, -3), result.selected)
        assertEquals(listOf(true, true, true), result.confirmed)
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
            service.update(OperatorDiscPresetUpdateReq(id = "preset-1", label = "新名称"), "user-2")
        }
    }

    @Test
    fun `rejects duplicate disc values regardless of forbidden state`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.create(
                OperatorDiscPresetCreateReq(
                    operatorId = "char_001",
                    label = "重复命盘",
                    selected = listOf(2, -2, 0),
                    confirmed = listOf(true, true, false),
                ),
                "user-1",
            )
        }

        assertEquals("同一预设不能重复选择命盘", exception.message)
    }

    @Test
    fun `rejects a selected disc in an unconfirmed slot`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.create(
                OperatorDiscPresetCreateReq(
                    operatorId = "char_001",
                    label = "非法命盘",
                    selected = listOf(1, 0, 0),
                    confirmed = listOf(false, false, false),
                ),
                "user-1",
            )
        }

        assertEquals("命盘预设至少需要包含一个已选择槽位", exception.message)
    }

    @Test
    fun `rejects creation when the user preset limit is reached`() {
        every { repository.countByUserId("user-1") } returns 200

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.create(
                OperatorDiscPresetCreateReq(
                    operatorId = "char_001",
                    label = "超限预设",
                    selected = listOf(1, 0, 0),
                    confirmed = listOf(true, false, false),
                ),
                "user-1",
            )
        }

        assertEquals("每个用户最多只能保存200个命盘预设", exception.message)
    }

    @Test
    fun `deletes only a preset owned by the user`() {
        val preset = preset()
        every { repository.findByIdAndUserId("preset-1", "user-1") } returns preset
        every { repository.delete(preset) } just runs

        service.delete("preset-1", "user-1")

        verify(exactly = 1) { repository.delete(preset) }
    }

    private fun preset() = OperatorDiscPreset(
        id = "preset-1",
        userId = "user-1",
        operatorId = "char_001",
        label = "常用命盘",
        selected = listOf(1, 0, -3),
        confirmed = listOf(true, true, true),
        createTime = LocalDateTime.now(),
        updateTime = LocalDateTime.now(),
    )
}
