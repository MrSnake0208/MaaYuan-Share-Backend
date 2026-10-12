package plus.maa.backend.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.result.UpdateResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import plus.maa.backend.cache.InternalComposeCache
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.request.copilot.CopilotQueriesRequest
import plus.maa.backend.controller.request.copilot.CopilotRatingReq
import plus.maa.backend.repository.CopilotRepository
import plus.maa.backend.repository.RatingRepository
import plus.maa.backend.repository.RedisCache
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.repository.entity.MaaUser
import plus.maa.backend.repository.entity.Rating
import plus.maa.backend.service.model.CopilotSetStatus
import plus.maa.backend.service.segment.SegmentService
import java.time.LocalDateTime
import java.util.regex.Pattern

class CopilotInteractionTest {
    private val copilotRepository = mockk<CopilotRepository>()
    private val ratingRepository = mockk<RatingRepository>()
    private val mongoTemplate = mockk<MongoTemplate>()
    private val redisCache = mockk<RedisCache>(relaxed = true)
    private val userService = mockk<UserService>()
    private val segmentService = mockk<SegmentService>()
    private val service = CopilotService(
        copilotRepository,
        RatingService(ratingRepository),
        mongoTemplate,
        jacksonObjectMapper(),
        mockk(relaxed = true),
        redisCache,
        mockk(),
        userService,
        mockk(relaxed = true),
        MaaCopilotProperties(),
        mockk(),
        mockk(),
        segmentService,
        mockk(),
    )
    private val copilot = Copilot(
        copilotId = 25605,
        uploaderId = "author",
        uploadTime = LocalDateTime.now(),
        stageName = "test-stage",
        doc = Copilot.Doc(title = "伏击阵1-yuanassist用", details = "手机版可用"),
    )
    private var personalRating: Rating? = null

    @BeforeEach
    fun setUp() {
        clearCache()
        every { copilotRepository.findByCopilotIdAndDeleteIsFalse(25605) } answers { copilot.copy() }
        every { copilotRepository.existsCopilotsByCopilotId(25605) } returns true
        every { userService.findByUserIdOrDefaultInCache("author") } returns MaaUser.UNKNOWN
        every { ratingRepository.findByTypeAndKeyAndUserId(Rating.KeyType.COPILOT, "25605", "rater") } answers {
            personalRating?.copy()
        }
        every { ratingRepository.save(any<Rating>()) } answers {
            firstArg<Rating>().also { personalRating = it.copy() }
        }
        every { mongoTemplate.findOne(any<Query>(), Copilot::class.java) } answers { copilot.copy() }
        every { mongoTemplate.updateFirst(any<Query>(), any<Update>(), Copilot::class.java) } answers {
            val fields = secondArg<Update>().updateObject["\$set"] as Document
            copilot.likeCount = fields["likeCount"] as Long
            copilot.dislikeCount = fields["dislikeCount"] as Long
            copilot.ratingLevel = fields["ratingLevel"] as Int
            copilot.ratingRatio = fields["ratingRatio"] as Double
            UpdateResult.acknowledged(1, 1, null)
        }
        every { segmentService.getSegment(any<String>()) } returns emptyList()
    }

    @AfterEach
    fun clearCache() {
        InternalComposeCache.invalidateCopilotInfoByCid(25605)
        InternalComposeCache.invalidateCommentCountById(25605)
    }

    @Test
    fun `rating refreshes an already cached detail before returning`() {
        assertEquals(0L, service.getCopilotById("rater", 25605)!!.like)

        service.rates("rater", CopilotRatingReq(25605, "Like"))

        val detail = service.getCopilotById("rater", 25605)!!
        assertEquals(1L, detail.like)
        assertEquals(1, detail.ratingType)
    }

    @Test
    fun `rating clears affected home caches synchronously`() {
        every { redisCache.valueMemberInSet(any(), 25605L) } returns true

        service.rates("rater", CopilotRatingReq(25605, "Like"))

        verifyOrder {
            mongoTemplate.updateFirst(any<Query>(), any<Update>(), Copilot::class.java)
            redisCache.syncRemoveCacheByPattern("home:hot:*")
            redisCache.syncRemoveCacheByPattern("home:views:*")
            redisCache.syncRemoveCacheByPattern("home:id:*")
            redisCache.incZSet("rate:hot:copilotIds", "25605", 1.0, 100, 10800)
        }
        verify(exactly = 0) { redisCache.removeCacheByPattern(any()) }
    }

    @Test
    fun `repeating a like does not increase its count`() {
        repeat(2) { service.rates("rater", CopilotRatingReq(25605, "Like")) }

        assertEquals(1L, copilot.likeCount)
        verify(exactly = 1) { ratingRepository.save(any<Rating>()) }
    }

    @Test
    fun `cancelling a like refreshes the count and personal state`() {
        service.rates("rater", CopilotRatingReq(25605, "Like"))
        service.getCopilotById("rater", 25605)

        service.rates("rater", CopilotRatingReq(25605, "None"))

        val detail = service.getCopilotById("rater", 25605)!!
        assertEquals(0L, detail.like)
        assertEquals(0, detail.ratingType)
    }

    @Test
    fun `an English prefix searches title and description ignoring case`() {
        every { segmentService.getSegment("yuana") } returns listOf("yuana")
        every { segmentService.fetchIndexInfo("yuana") } returns emptySet()
        val query = searchQuery(" yuana ")
        val alternatives = query.getList("\$or", Document::class.java)

        val titlePattern = alternatives.first { it.containsKey("doc.title") }["doc.title"] as Pattern
        val detailsPattern = alternatives.first { it.containsKey("doc.details") }["doc.details"] as Pattern
        assertTrue(titlePattern.matcher(copilot.doc!!.title!!).find())
        assertTrue(detailsPattern.matcher("YUANASSIST 手机版可用").find())
        assertFalse(titlePattern.matcher("其他作业").find())
        val filters = query.getList("\$and", Document::class.java)
        assertTrue(filters.contains(Document("delete", false)))
        assertTrue(filters.contains(Document("status", CopilotSetStatus.PUBLIC)))
    }

    @Test
    fun `substring searches preserve uploader tag and operator filters`() {
        val query = searchQuery(
            CopilotQueriesRequest(document = "yuana", uploaderId = "author", tags = listOf("如鸢"), operator = "密探"),
        )
        val filters = query.getList("\$and", Document::class.java)

        assertTrue(filters.contains(Document("uploaderId", "author")))
        assertTrue(filters.contains(Document("tags", Document("\$all", listOf("如鸢")))))
        assertTrue(filters.contains(Document("opers.name", "密探")))
        assertTrue(query.containsKey("\$or"))
    }

    @Test
    fun `search treats regular expression characters literally`() {
        val query = searchQuery("yuan.*")
        val alternatives = query.getList("\$or", Document::class.java)
        val pattern = alternatives.first { it.containsKey("doc.title") }["doc.title"] as Pattern

        assertFalse(pattern.matcher("yuanassist").find())
        assertTrue(pattern.matcher("脚本 yuan.* 示例").find())
    }

    @Test
    fun `search preserves existing segmented matches alongside substring matches`() {
        every { segmentService.getSegment("伏击 手机版") } returns listOf("伏击", "手机版")
        every { segmentService.fetchIndexInfo("伏击") } returns setOf(25605, 36880)
        every { segmentService.fetchIndexInfo("手机版") } returns setOf(25605)

        val query = searchQuery("伏击 手机版")
        val alternatives = query.getList("\$or", Document::class.java)
        val ids = alternatives.first { it.containsKey("copilotId") }["copilotId"] as Document

        assertEquals(setOf(25605L), (ids["\$in"] as Collection<*>).toSet())
    }

    @Test
    fun `single character searches are filtered instead of returning all copilots`() {
        val query = searchQuery("鸢")
        assertTrue(query.containsKey("\$or"))
    }

    @Test
    fun `blank searches do not add keyword filters`() {
        val query = searchQuery("  ")
        assertFalse(query.containsKey("\$or"))
    }

    private fun searchQuery(keyword: String): Document = searchQuery(CopilotQueriesRequest(document = keyword))

    private fun searchQuery(request: CopilotQueriesRequest): Document {
        val query = slot<Query>()
        every { mongoTemplate.find(capture(query), Copilot::class.java) } returns emptyList()

        service.queriesCopilot(null, request)

        assertTrue(query.isCaptured, "Search must reach MongoDB even when the segment index has no matching token")
        return query.captured.queryObject
    }
}
