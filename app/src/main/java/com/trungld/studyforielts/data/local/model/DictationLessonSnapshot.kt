package com.trungld.studyforielts.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.data.local.entity.ProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.data.local.entity.SentenceProgressEntity

data class DictationLessonSnapshot(
    @Embedded
    val lesson: LessonEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "lessonId",
    )
    val sentences: List<SentenceEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "lessonId",
    )
    val progressEntries: List<ProgressEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "lessonId",
    )
    val sentenceProgressEntries: List<SentenceProgressEntity>,
) {
    val progress: ProgressEntity?
        get() = progressEntries.firstOrNull()
}
