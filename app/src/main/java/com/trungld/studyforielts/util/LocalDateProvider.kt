package com.trungld.studyforielts.util

import java.time.LocalDate

/** Resolves the current local calendar date. Wrapped for deterministic tests. */
fun interface LocalDateProvider {
    fun today(): LocalDate

    companion object {
        val System: LocalDateProvider = LocalDateProvider { LocalDate.now() }
    }
}
