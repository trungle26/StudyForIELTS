package com.trungld.studyforielts.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.trungld.studyforielts.data.local.entity.YoutubeSentenceEntity
import com.trungld.studyforielts.data.local.entity.YoutubeVideoEntity

data class YoutubeVideoWithSentences(
    @Embedded
    val video: YoutubeVideoEntity,
    @Relation(
        parentColumn = "videoId",
        entityColumn = "videoId",
    )
    val sentences: List<YoutubeSentenceEntity>,
)
