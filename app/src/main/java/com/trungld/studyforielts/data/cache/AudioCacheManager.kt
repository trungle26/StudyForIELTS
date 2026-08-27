package com.trungld.studyforielts.data.cache

import android.content.Context
import com.trungld.studyforielts.data.local.dao.RemoteDictationDao
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Auto-downloading audio cache for remote dictation lessons.
 *
 * Behavior:
 *  - [ensureLocalAudio] is idempotent. If a local file already exists for the lesson,
 *    the call returns immediately. Otherwise it kicks off a background download.
 *  - Successful downloads write the file under `filesDir/audio/<serverId>.audio`,
 *    then update Room via [RemoteDictationRepository.updateLocalAudio] so the lesson
 *    is marked cached.
 *  - On failure the file is deleted; the next call can retry.
 *  - [evictStale] removes downloads whose underlying file is gone (defensive cleanup)
 *    and enforces a soft storage cap by deleting the least-recently-accessed downloads
 *    once the total exceeds [MAX_STORAGE_BYTES].
 *
 * `ponytail:` ceiling = no resumable downloads, no parallelism, no foreground service.
 * Upgrade path: replace with WorkManager + Range headers once auth + WorkManager land,
 * and add a real MIME sniffer so we keep file extensions instead of `.audio`.
 */
@Singleton
class AudioCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val repository: RemoteDictationRepository,
    private val remoteDictationDao: RemoteDictationDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow<Map<String, AudioDownloadState>>(emptyMap())
    val state: StateFlow<Map<String, AudioDownloadState>> = _state.asStateFlow()

    /** Returns the local file for a lesson if it exists on disk, otherwise null. */
    fun localFile(serverId: String): File? {
        val file = audioFile(serverId)
        return if (file.exists() && file.length() > 0L) file else null
    }

    /**
     * Idempotent. If the lesson already has a local file, returns immediately. Otherwise
     * starts a background download and updates Room on completion.
     */
    fun ensureLocalAudio(serverId: String, audioUrl: String) {
        if (audioUrl.isBlank()) return
        if (localFile(serverId) != null) {
            _state.update { it + (serverId to AudioDownloadState.READY) }
            return
        }
        scope.launch {
            // Coalesce concurrent requests for the same lesson.
            mutex.withLock {
                if (localFile(serverId) != null) return@withLock
                _state.update { it + (serverId to AudioDownloadState.DOWNLOADING) }
                runCatching {
                    download(serverId, audioUrl)
                }.onFailure {
                    _state.update { it + (serverId to AudioDownloadState.FAILED) }
                }
            }
        }
    }

    /** Removes the downloaded audio for a single lesson. Safe to call when nothing is cached. */
    suspend fun remove(serverId: String) = withContext(NonCancellable + Dispatchers.IO) {
        mutex.withLock {
            val file = audioFile(serverId)
            if (file.exists()) file.delete()
            repository.updateLocalAudio(serverId = serverId, path = null, bytes = 0L)
            _state.update { it - serverId }
        }
    }

    /** Removes all cached audio (e.g. on user-requested "Clear downloads"). */
    suspend fun removeAll() = withContext(NonCancellable + Dispatchers.IO) {
        mutex.withLock {
            audioDir().listFiles()?.forEach { it.delete() }
            // Reset the DB-side audio columns for any lesson that still claims a path.
            remoteDictationDao.observeDownloadedLessonsByAccessTime().forEach { lesson ->
                repository.updateLocalAudio(serverId = lesson.serverId, path = null, bytes = 0L)
            }
            _state.value = emptyMap()
        }
    }

    /**
     * Cleans up orphans (DB row says downloaded, but file is gone) and trims the cache
     * down to [MAX_STORAGE_BYTES] by removing least-recently-accessed lessons first.
     */
    suspend fun evictStale() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cached = remoteDictationDao.observeDownloadedLessonsByAccessTime()
            var totalBytes = 0L
            val toDelete = mutableListOf<Pair<String, File>>()

            for (lesson in cached) {
                val path = lesson.localAudioPath ?: continue
                val file = File(path)
                if (!file.exists() || file.length() == 0L) {
                    // Orphan DB row; clear the columns but don't add to totalBytes.
                    repository.updateLocalAudio(serverId = lesson.serverId, path = null, bytes = 0L)
                    continue
                }
                totalBytes += file.length()
                if (totalBytes > MAX_STORAGE_BYTES) {
                    // Already past the cap (sorted oldest-first), so everything from here on gets evicted.
                    toDelete += lesson.serverId to file
                }
            }

            toDelete.forEach { (serverId, file) ->
                file.delete()
                repository.updateLocalAudio(serverId = serverId, path = null, bytes = 0L)
            }
        }
    }

    /** Total bytes currently on disk across all cached audio files. */
    fun totalBytesOnDisk(): Long {
        val dir = audioDir()
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private suspend fun download(serverId: String, audioUrl: String) = withContext(Dispatchers.IO) {
        val target = audioFile(serverId)
        target.parentFile?.mkdirs()

        val request = Request.Builder().url(audioUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }

        val bytes = target.length()
        if (bytes == 0L) {
            target.delete()
            throw IOException("Downloaded 0 bytes")
        }

        repository.updateLocalAudio(
            serverId = serverId,
            path = target.absolutePath,
            bytes = bytes,
        )
        _state.update { it + (serverId to AudioDownloadState.READY) }
    }

    private fun audioFile(serverId: String): File = File(audioDir(), "$serverId.audio")

    private fun audioDir(): File = File(context.filesDir, "audio").apply { mkdirs() }

    companion object {
        /**
         * Soft cap on total cached audio size. Once exceeded, least-recently-accessed files
         * are evicted in [evictStale].
         *
         * `ponytail:` ceiling = 200MB is conservative for short IELTS tracks. Upgrade path:
         * read from settings / server-driven config once we have per-user preferences.
         */
        const val MAX_STORAGE_BYTES: Long = 200L * 1024L * 1024L
    }
}

enum class AudioDownloadState { IDLE, DOWNLOADING, READY, FAILED }
