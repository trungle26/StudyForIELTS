package com.trungld.studyforielts.domain.model

/**
 * Per-lesson cache freshness. Drives the UI badge and refresh policy.
 *
 *  - [FRESH]: cached within the TTL window; no background refresh needed.
 *  - [STALE]: cached but older than the TTL; UI shows cached data while a background refresh runs.
 *  - [MISSING]: not in cache at all; only happens before the first refresh completes.
 */
enum class CacheStatus { FRESH, STALE, MISSING }

data class CachedRemoteDictationLesson(
    val lesson: RemoteDictationLesson,
    val cacheStatus: CacheStatus,
    val hasLocalAudio: Boolean,
    val hasVocabulary: Boolean,
    val localAudioBytes: Long,
) {
    val isFullyCached: Boolean
        get() = hasLocalAudio && hasVocabulary
}
