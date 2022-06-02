package com.pcp.bentotw

data class FileStruct(
    val correct: Int,
    val lineNumber: Int,
    val content: String,
    val parse: List<String>,
)