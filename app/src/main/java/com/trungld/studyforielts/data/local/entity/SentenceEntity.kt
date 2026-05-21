package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sentences",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["lessonId"]),
    ],
)
data class SentenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val lessonId: Long,
    val orderIndex: Int,
    val correctText: String,
    val startTime: Long,
    val endTime: Long,
)
