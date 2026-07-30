package plus.maa.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import plus.maa.backend.controller.request.copilot.CopilotShareImageConfigUpdateReq
import plus.maa.backend.repository.CopilotRepository
import plus.maa.backend.repository.CopilotShareImageConfigRepository
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.repository.entity.CopilotShareImageConfig
import java.time.LocalDateTime

class CopilotShareImageConfigServiceTest {
    private val repository = mockk<CopilotShareImageConfigRepository>()
    private val copilotRepository = mockk<CopilotRepository>()
    private val userService = mockk<UserService>()
    private val service = CopilotShareImageConfigService(repository, copilotRepository, userService, ObjectMapper())

    @Test
    fun `lists public configurations for an existing copilot`() {
        every { copilotRepository.findByCopilotIdAndDeleteIsFalse(100) } returns copilot()
        every { repository.findAllByCopilotIdOrderByCardKey(100) } returns listOf(config())

        val result = service.list(100)

        assertEquals(listOf("actions"), result.map { it.cardKey })
        assertEquals(2, result.single().revision)
    }

    @Test
    fun `creates a versioned configuration for the copilot owner`() {
        every { copilotRepository.findByCopilotIdAndDeleteIsFalse(100) } returns copilot()
        every { repository.findByCopilotIdAndCardKey(100, "deployed-operators") } returns null
        every { repository.insert(any<CopilotShareImageConfig>()) } answers {
            firstArg<CopilotShareImageConfig>().copy(id = "config-1", revision = 0)
        }

        val result = service.update(
            100,
            " deployed-operators ",
            CopilotShareImageConfigUpdateReq(
                schemaVersion = 1,
                expectedRevision = 0,
                payload = mapOf("requiredDiscs" to mapOf("1:1" to true)),
            ),
            "user-1",
        )

        assertEquals("deployed-operators", result.cardKey)
        assertEquals(0, result.revision)
        verify(exactly = 1) { repository.insert(any<CopilotShareImageConfig>()) }
    }

    @Test
    fun `allows an administrator to update an existing configuration`() {
        val config = config()
        every { copilotRepository.findByCopilotIdAndDeleteIsFalse(100) } returns copilot()
        every { userService.hasAdminPrivileges("admin-1") } returns true
        every { repository.findByCopilotIdAndCardKey(100, "actions") } returns config
        every { repository.save(config) } answers {
            firstArg<CopilotShareImageConfig>().copy(revision = 3)
        }

        val result = service.update(
            100,
            "actions",
            CopilotShareImageConfigUpdateReq(2, 2, mapOf("showNotes" to true)),
            "admin-1",
        )

        assertEquals(2, result.schemaVersion)
        assertEquals(3, result.revision)
    }

    @Test
    fun `rejects updates from users who do not own the copilot`() {
        every { copilotRepository.findByCopilotIdAndDeleteIsFalse(100) } returns copilot()
        every { userService.hasAdminPrivileges("user-2") } returns false

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.update(100, "actions", CopilotShareImageConfigUpdateReq(1, 0), "user-2")
        }

        assertEquals(403, exception.statusCode.value())
    }

    @Test
    fun `rejects stale revisions and schema downgrades`() {
        every { copilotRepository.findByCopilotIdAndDeleteIsFalse(100) } returns copilot()
        every { repository.findByCopilotIdAndCardKey(100, "actions") } returns config(schemaVersion = 2)

        val stale = assertThrows(ResponseStatusException::class.java) {
            service.update(100, "actions", CopilotShareImageConfigUpdateReq(2, 1), "user-1")
        }
        assertEquals(409, stale.statusCode.value())

        val downgrade = assertThrows(ResponseStatusException::class.java) {
            service.update(100, "actions", CopilotShareImageConfigUpdateReq(1, 2), "user-1")
        }
        assertEquals(409, downgrade.statusCode.value())
    }

    @Test
    fun `rejects unsafe payload field names`() {
        every { copilotRepository.findByCopilotIdAndDeleteIsFalse(100) } returns copilot()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.update(
                100,
                "actions",
                CopilotShareImageConfigUpdateReq(1, 0, mapOf("\$unsafe" to true)),
                "user-1",
            )
        }

        assertEquals("分享图配置的字段名不能以 $ 开头或包含点号", exception.message)
    }

    private fun copilot() = Copilot(copilotId = 100, uploaderId = "user-1")

    private fun config(schemaVersion: Int = 1) = CopilotShareImageConfig(
        id = "config-1",
        copilotId = 100,
        cardKey = "actions",
        schemaVersion = schemaVersion,
        payload = mapOf("showNotes" to false),
        createTime = LocalDateTime.now(),
        updateTime = LocalDateTime.now(),
        revision = 2,
    )
}
