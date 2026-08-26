package com.trungld.studyforielts.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AeroThemeTest {
    @Test
    fun testAeroColorsInitialized() {
        assertNotNull(PrimaryLight)
        assertNotNull(SecondaryLight)
        assertNotNull(BackgroundLight)
        val appColors = lightAppColors()
        assertEquals(CorrectGreenLight, appColors.correctGreen)
    }
}
