package com.nosferatu.launcher.parser

import java.io.File

interface ParserStrategy {
    suspend fun parse(file: File): RawMetadata
}

data class RawMetadata(val title: String?, val author: String?)