package com.trungld.studyforielts.presentation.vocabulary

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun buildContextLookupUrl(word: String): String {
    val encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.toString())
    return "https://www.google.com/search?q=how+to+use+$encodedWord+in+a+sentence"
}

internal fun buildImageLookupUrl(word: String): String {
    val encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.toString())
    return "https://www.google.com/search?tbm=isch&q=$encodedWord"
}