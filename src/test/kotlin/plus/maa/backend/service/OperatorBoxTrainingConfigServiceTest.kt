package plus.maa.backend.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import plus.maa.backend.controller.request.OperatorTrainingConfigSaveReq
import plus.maa.backend.controller.request.box.OperatorBoxTrainingConfigBatchSaveReq
import plus.maa.backend.controller.request.box.OperatorBoxTrainingConfigSaveReq
import plus.maa.backend.repository.OperatorBoxPresetRepository
import plus.maa.backend.repository.OperatorBoxTrainingConfigRepository
import plus.maa.backend.repository.entity.OperatorBoxPreset
import plus.maa.backend.repository.entity.OperatorBoxTrainingConfig
import plus.maa.backend.service.model.OperatorBoxMember
import java.time.LocalDateTime

class OperatorBoxTrainingConfigServiceTest {
    private val repository = mockk<OperatorBoxTrainingConfigRepository>()
    private val boxRepository = mockk<OperatorBoxPresetRepository>()
    private val service = OperatorBoxTrainingConfigService(repository, boxRepository)

    @Test
    fun `stores the same operator independently in different owned boxes`() {
        every { boxRepository.findByIdAndUserId(any(), "user-1") } answers { box(firstArg()) }
        every { repository.findByUserIdAndBoxIdAndOperatorId("user-1", any(), "char-1") } returns null
        every { repository.save(any<OperatorBoxTrainingConfig>()) } answers {
            firstArg<OperatorBoxTrainingConfig>().copy(id = "saved-${firstArg<OperatorBoxTrainingConfig>().boxId}")
        }

        val first = service.save(saveReq("box-1", level = 60), "user-1")
        val second = service.save(saveReq("box-2", level = 80), "user-1")

        assertEquals("box-1", first.boxId)
        assertEquals(60, first.level)
        assertEquals("box-2", second.boxId)
        assertEquals(80, second.level)
        verify(exactly = 1) {
            repository.findByUserIdAndBoxIdAndOperatorId("user-1", "box-1", "char-1")
        }
        verify(exactly = 1) {
            repository.findByUserIdAndBoxIdAndOperatorId("user-1", "box-2", "char-1")
        }
    }

    @Test
    fun `rejects access to a box not owned by the authenticated user`() {
        every { boxRepository.findByIdAndUserId("box-2", "user-1") } returns null

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.save(saveReq("box-2"), "user-1")
        }

        assertEquals("阵容预设不存在或无权访问", exception.message)
    }

    @Test
    fun `rejects duplicate operators in a batch`() {
        every { boxRepository.findByIdAndUserId("box-1", "user-1") } returns box("box-1")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.saveBatch(
                OperatorBoxTrainingConfigBatchSaveReq(
                    boxId = "box-1",
                    configs = listOf(configReq(), configReq(operatorId = " char-1 ")),
                ),
                "user-1",
            )
        }

        assertEquals("同一批次不能重复保存密探练度配置", exception.message)
    }

    private fun saveReq(boxId: String, level: Int = 60) = OperatorBoxTrainingConfigSaveReq(
        boxId = boxId,
        config = configReq(level = level),
    )

    private fun configReq(
        operatorId: String = "char-1",
        level: Int = 60,
    ) = OperatorTrainingConfigSaveReq(
        operatorId = operatorId,
        level = level,
        elite = 10,
    )

    private fun box(id: String) = OperatorBoxPreset(
        id = id,
        userId = "user-1",
        label = id,
        members = listOf(OperatorBoxMember("char-1", 0)),
        schemaVersion = 1,
        createTime = LocalDateTime.now(),
        updateTime = LocalDateTime.now(),
        revision = 0,
    )
}
