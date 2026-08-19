package com.trungld.studyforielts.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationProgressEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceProgressEntity

data class RemoteDictationLessonSnapshot(
    @Embedded
    val lesson: RemoteDictationLessonEntity,
    @Relation(
        parentColumn = "serverId",
        entityColumn = "lessonServerId",
    )
    val sentences: List<RemoteDictationSentenceEntity>,
    @Relation(
        parentColumn = "serverId",
        entityColumn = "lessonServerId",
    )
    val progressEntries: List<RemoteDictationProgressEntity>,
    @Relation(
        parentColumn = "serverId",
        entityColumn = "lessonServerId",
    )
    val sentenceProgressEntries: List<RemoteDictationSentenceProgressEntity>,
) {
    val progress: RemoteDictationProgressEntity?
        get() = progressEntries.firstOrNull()
}