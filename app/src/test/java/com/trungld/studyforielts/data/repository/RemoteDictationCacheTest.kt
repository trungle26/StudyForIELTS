package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.domain.model.CacheStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours

/**
 * Cache-first semantics for [RemoteDictationRepositoryImpl]: freshness windows, audio-path preservation
 * across replacement, and graceful degradation on offline.
 *
 * `ponytail:` no Robolectric/MockK — we exercise pure extension functions only.
 * Add a Room-in-Memory test when DAO interaction needs verification.
 */
class RemoteDictationCacheTest {

    private val ttlMillis = RemoteDictationRepositoryImpl.CACHE_TTL.inWholeMilliseconds

    private fun entity(
        serverId: String = "lesson-1",
        cachedAt: Long = 0L,
        localAudioPath: String? = null,
        localAudioBytes: Long = 0L,
    ) = RemoteDictationLessonEntity(
        serverId = serverId,
        title = "Sample",
        level = "B1",
        source = "BBC",
        audioUrl = "https://example.com/audio.mp3",
        durationSeconds = 120,
        updatedAt = "2026-01-01",
        cachedAt = cachedAt,
        localAudioPath = localAudioPath,
        localAudioBytes = localAudioBytes,
    )

    /** Cache under TTL → FRESH regardless of local audio state. */
    @Test
    fun `toCached returns FRESH when under TTL`() {
        val now = 1_000_000L
        val e = entity(cachedAt = now - 1.hours.inWholeMilliseconds)
        val cached = e.toCached(now)
        assertEquals(CacheStatus.FRESH, cached.cacheStatus)
        assertFalse(cached.hasLocalAudio)
    }

    /** Cache past TTL → STALE; UI still shows row but flags it for background refresh. */
    @Test
    fun `toCached returns STALE when over TTL`() {
        val now = 1_000_000L
        val e = entity(cachedAt = now - 25.hours.inWholeMilliseconds)
        val cached = e.toCached(now)
        assertEquals(CacheStatus.STALE, cached.cacheStatus)
    }

    /** Boundary: exactly TTL is the first STALE point (we use `age < TTL`). */
    @Test
    fun `toCached boundary at TTL is STALE`() {
        val now = 1_000_000L
        val e = entity(cachedAt = now - ttlMillis)
        val cached = e.toCached(now)
        assertEquals(CacheStatus.STALE, cached.cacheStatus)
    }

    /** Future-dated cachedAt (clock skew) must not blow up — treated as FRESH. */
    @Test
    fun `toCached with future cachedAt is FRESH`() {
        val now = 1_000_000L
        val e = entity(cachedAt = now + 60_000L)
        val cached = e.toCached(now)
        assertEquals(CacheStatus.FRESH, cached.cacheStatus)
    }

    /** hasLocalAudio mirrors localAudioPath non-blank check. */
    @Test
    fun `toCached hasLocalAudio reflects localAudioPath`() {
        val now = 1_000_000L
        assertFalse(entity(localAudioPath = null).toCached(now).hasLocalAudio)
        assertFalse(entity(localAudioPath = "").toCached(now).hasLocalAudio)
        assertTrue(
            entity(localAudioPath = "/data/audio/lesson-1.audio", localAudioBytes = 4096)
                .toCached(now)
                .hasLocalAudio
        )
    }

    /** localAudioBytes propagates so the UI can show a size label. */
    @Test
    fun `toCached exposes localAudioBytes`() {
        val now = 1_000_000L
        val cached = entity(localAudioPath = "/x", localAudioBytes = 2_000_000L).toCached(now)
        assertEquals(2_000_000L, cached.localAudioBytes)
    }
}
