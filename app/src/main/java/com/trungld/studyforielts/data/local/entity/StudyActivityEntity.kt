package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per local calendar day in which the user performed a meaningful
 * dictation action (sentence completed or intentionally skipped). The date key
 * is stored as ISO-8601 (yyyy-MM-dd) in the app's local zone; the streak is
 * computed from this column.
 */
@Entity(tableName = "study_activity")
data class StudyActivityEntity(
    @PrimaryKey
    val activityDate: String,
    val recordedAt: Long = System.currentTimeMillis(),
)
