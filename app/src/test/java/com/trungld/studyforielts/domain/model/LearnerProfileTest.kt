package com.trungld.studyforielts.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LearnerProfileTest {
    @Test fun unknownLevelUsesDiagnostic() {
        assertEquals(RecommendationTitle.DIAGNOSTIC, recommend(LearnerProfile()).title)
    }

    @Test fun foundationLevelsUseFoundationRecommendation() {
        assertEquals(RecommendationTitle.FOUNDATION, recommend(LearnerProfile(currentCefrLevel = CefrLevel.A2)).title)
    }

    @Test fun targetBandDoesNotReplaceCefr() {
        val profile = LearnerProfile(currentCefrLevel = CefrLevel.B2, targetBand = TargetBand.B7_0)
        assertEquals(RecommendationTitle.IELTS_SKILL, recommend(profile).title)
        assertEquals(CefrLevel.B2, profile.currentCefrLevel)
        assertEquals(TargetBand.B7_0, profile.targetBand)
    }

    @Test fun a1AndUnknownUseDailyRoutinesPath() {
        assertEquals(true, shouldRecommendDailyRoutines(LearnerProfile(currentCefrLevel = CefrLevel.A1)))
        assertEquals(true, shouldRecommendDailyRoutines(LearnerProfile()))
        assertEquals(false, shouldRecommendDailyRoutines(LearnerProfile(currentCefrLevel = CefrLevel.B2)))
    }

    @Test fun dailyRoutinesPathKeepsMetadataSeparate() {
        val path = DailyRoutinesPath()
        assertEquals(DAILY_ROUTINES_TOPIC, path.topic)
        assertEquals(DAILY_ROUTINES_STAGE, path.stage)
        assertEquals(DAILY_ROUTINES_CEFR, path.cefrLevel)
        assertEquals(listOf(DailyRoutinesActivity.VOCABULARY, DailyRoutinesActivity.GRAMMAR, DailyRoutinesActivity.DICTATION, DailyRoutinesActivity.WRITING, DailyRoutinesActivity.REVIEW), path.activities)
    }
}
