package com.trungld.studyforielts.data.local.database

import androidx.room.TypeConverter
import com.trungld.studyforielts.data.local.entity.SentenceStatus

class RoomConverters {

    @TypeConverter
    fun fromSentenceStatus(value: SentenceStatus): String = value.name

    @TypeConverter
    fun toSentenceStatus(value: String): SentenceStatus = SentenceStatus.valueOf(value)
}
