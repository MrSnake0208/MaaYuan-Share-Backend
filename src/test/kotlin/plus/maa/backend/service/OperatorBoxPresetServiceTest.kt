package plus.maa.backend.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import plus.maa.backend.controller.request.box.OperatorBoxPresetCreateReq
import plus.maa.backend.controller.request.box.OperatorBoxPresetUpdateReq
import plus.maa.backend.repository.OperatorBoxPresetRepository
import plus.maa.backend.repository.entity.OperatorBoxPreset
import plus.maa.backend.service.model.OperatorBoxMember
import java.time.LocalDateTime

class OperatorBoxPresetServiceTest {
    private val repository = mockk<OperatorBoxPresetRepository>()
    private val service = OperatorBoxPresetService(repository)

    @Test
    fun `creates a normalized versioned box for the authenticated user`() {
        every { repository.countByUserId("user-1") } returns 1
        every { repository.existsByUserIdAndLabel("user-1", "深塔主力队") } returns false
        every { repository.insert(any<OperatorBoxPreset>()) } answers {
            firstArg<OperatorBoxPreset>().copy(id = "box-2", revision = 0)
        }

        val result = service.create(
            OperatorBoxPresetCreateReq(
                label = " 深塔主力队 ",
                members = listOf(
                    OperatorBoxMember(" 密探乙 ", 9),
                    OperatorBoxMember("密探甲", 2),
                ),
            ),
            "user-1",
        )

        assertEquals("box-2", result.id)
        assertEquals("深塔主力队", result.label)
        assertEquals(listOf("密探甲", "密探乙"), result.members.map { it.operatorKey })
        assertEquals(listOf(0, 1), result.members.map { it.order })
        assertEquals(1, result.schemaVersion)
        assertEquals(0, result.revision)
    }

    @Test
    fun `lists multiple boxes for the current user`() {
        every { repository.findAllByUserIdOrderByUpdateTimeDesc("user-1") } returns listOf(
            box(id = "box-2", label = "第二队"),
            box(id = "box-1", label = "第一队"),
        )

        val result = service.list("user-1")

        assertEquals(listOf("box-2", "box-1"), result.map { it.id })
        verify(exactly = 1) { repository.findAllByUserIdOrderByUpdateTimeDesc("user-1") }
    }

    @Test
    fun `rejects duplicate operators in one box`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.create(
                OperatorBoxPresetCreateReq(
                    label = "重复阵容",
                    members = listOf(
                        OperatorBoxMember("密探甲", 0),
                        OperatorBoxMember(" 密探甲 ", 1),
                    ),
                ),
                "user-1",
            )
        }

        assertEquals("同一阵容不能重复包含密探", exception.message)
    }

    @Test
    fun `updates an owned box with optimistic concurrency`() {
        val existing = box()
        every { repository.findByIdAndUserId("box-1", "user-1") } returns existing
        every { repository.existsByUserIdAndLabelAndIdNot("user-1", "新名称", "box-1") } returns false
        every { repository.save(existing) } answers {
            firstArg<OperatorBoxPreset>().copy(revision = 3)
        }

        val result = service.update(
            OperatorBoxPresetUpdateReq(
                id = "box-1",
                expectedRevision = 2,
                label = " 新名称 ",
            ),
            "user-1",
        )

        assertEquals("新名称", result.label)
        assertEquals(3, result.revision)
    }

    @Test
    fun `rejects stale updates`() {
        every { repository.findByIdAndUserId("box-1", "user-1") } returns box()

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.update(
                OperatorBoxPresetUpdateReq(
                    id = "box-1",
                    expectedRevision = 1,
                    label = "过期更新",
                ),
                "user-1",
            )
        }

        assertEquals(409, exception.statusCode.value())
    }

    @Test
    fun `deletes only the owned current revision`() {
        val existing = box()
        every { repository.findByIdAndUserId("box-1", "user-1") } returns existing
        every { repository.delete(existing) } just runs

        service.delete("box-1", 2, "user-1")

        verify(exactly = 1) { repository.delete(existing) }
    }

    private fun box(
        id: String = "box-1",
        label: String = "常用阵容",
    ) = OperatorBoxPreset(
        id = id,
        userId = "user-1",
        label = label,
        members = listOf(OperatorBoxMember("密探甲", 0)),
        schemaVersion = 1,
        createTime = LocalDateTime.now(),
        updateTime = LocalDateTime.now(),
        revision = 2,
    )
}
