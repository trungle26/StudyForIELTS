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
}
