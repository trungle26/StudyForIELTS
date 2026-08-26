package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.domain.model.IeltsSkillType
import com.trungld.studyforielts.domain.model.StrategyGuide
import kotlinx.coroutines.flow.Flow

interface StrategyRepository {
    fun observeStrategies(skill: IeltsSkillType? = null): Flow<List<StrategyGuide>>
    fun observeSpotlight(): Flow<StrategyGuide?>
    suspend fun getStrategyById(id: String): StrategyGuide?
    suspend fun refreshStrategies(): Result<Unit>
}
