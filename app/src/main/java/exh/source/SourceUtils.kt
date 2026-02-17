package exh.source

import tachiyomi.domain.source.model.Source
import eu.kanade.tachiyomi.source.Source as SourceApi

fun Source.isEhBasedSource(): Boolean = id == EH_SOURCE_ID || id == EXH_SOURCE_ID
fun SourceApi.isEhBasedSource(): Boolean = id == EH_SOURCE_ID || id == EXH_SOURCE_ID
