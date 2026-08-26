package com.trungld.studyforielts.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trungld.studyforielts.data.remote.api.StrategyApi
import com.trungld.studyforielts.data.remote.model.StrategyGuideDto
import com.trungld.studyforielts.domain.model.IeltsSkillType
import com.trungld.studyforielts.domain.model.StrategyGuide
import com.trungld.studyforielts.domain.repository.StrategyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrategyRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val strategyApi: StrategyApi,
) : StrategyRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val _strategies = MutableStateFlow<List<StrategyGuide>>(emptyList())
    private val gson = Gson()

    init {
        // Load offline seed data immediately on repository creation
        loadSeedData()
    }

    private fun loadSeedData() {
        try {
            val jsonString = context.assets.open("seed_strategies.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<StrategyGuideDto>>() {}.type
            val dtos: List<StrategyGuideDto> = gson.fromJson(jsonString, type)
            _strategies.value = dtos.map { it.toDomain() }
        } catch (_: Exception) {
            // Asset load fallback
        }
    }

    override fun observeStrategies(skill: IeltsSkillType?): Flow<List<StrategyGuide>> {
        return _strategies.asStateFlow().map { list ->
            if (skill == null) list else list.filter { it.skill == skill }
        }
    }

    override fun observeSpotlight(): Flow<StrategyGuide?> {
        return _strategies.asStateFlow().map { list ->
            list.firstOrNull { it.isSpotlight } ?: list.firstOrNull()
        }
    }

    override suspend fun getStrategyById(id: String): StrategyGuide? = withContext(ioDispatcher) {
        // Check in-memory list first
        val local = _strategies.value.firstOrNull { it.id == id }
        if (local != null) return@withContext local

        try {
            val remote = strategyApi.getStrategyDetail(id).toDomain()
            // Update cache
            _strategies.value = _strategies.value.filterNot { it.id == id } + remote
            remote
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun refreshStrategies(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = strategyApi.listStrategies()
            val remoteItems = response.items.map { it.toDomain() }
            if (remoteItems.isNotEmpty()) {
                _strategies.value = remoteItems
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
