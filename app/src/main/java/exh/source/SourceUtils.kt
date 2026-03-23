package exh.source

import tachiyomi.domain.source.model.Source
import eu.kanade.tachiyomi.source.Source as SourceApi

fun Source.isEhBasedSource(): Boolean = id == EH_SOURCE_ID || id == EXH_SOURCE_ID
fun SourceApi.isEhBasedSource(): Boolean = id == EH_SOURCE_ID || id == EXH_SOURCE_ID

fun Source.isNhentaiSource(): Boolean = id in nHentaiSourceIds
fun SourceApi.isNhentaiSource(): Boolean = id in nHentaiSourceIds

/** Returns true for any source that has rich metadata (EHentai, NHentai, etc.) */
fun Source.isMetadataSource(): Boolean = isEhBasedSource() || isNhentaiSource()
fun SourceApi.isMetadataSource(): Boolean = isEhBasedSource() || isNhentaiSource()
