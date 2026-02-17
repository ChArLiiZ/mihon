package exh.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.PURURIN_SOURCE_ID
import exh.source.TSUMINO_SOURCE_ID
import exh.source.lanraragiSourceIds
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import java.util.Locale
import tachiyomi.i18n.MR

object SourceTagsUtil {
    fun getWrappedTag(
        sourceId: Long?,
        namespace: String? = null,
        tag: String? = null,
        fullTag: String? = null,
    ): String? {
        return if (
            sourceId == EXH_SOURCE_ID ||
            sourceId == EH_SOURCE_ID ||
            sourceId in nHentaiSourceIds ||
            sourceId in mangaDexSourceIds ||
            sourceId == PURURIN_SOURCE_ID ||
            sourceId == TSUMINO_SOURCE_ID ||
            sourceId in lanraragiSourceIds
        ) {
            val parsed = when {
                fullTag != null -> parseTag(fullTag)
                namespace != null && tag != null -> RaisedTag(namespace, tag, TAG_TYPE_DEFAULT)
                else -> null
            }
            if (parsed?.namespace != null) {
                when (sourceId) {
                    in nHentaiSourceIds -> wrapTagNHentai(parsed.namespace!!, parsed.name.substringBefore('|').trim())
                    in mangaDexSourceIds -> parsed.name
                    PURURIN_SOURCE_ID -> parsed.name.substringBefore('|').trim()
                    TSUMINO_SOURCE_ID -> wrapTagTsumino(parsed.namespace!!, parsed.name.substringBefore('|').trim())
                    else -> wrapTag(parsed.namespace!!, parsed.name.substringBefore('|').trim())
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun wrapTag(namespace: String, tag: String) = if (tag.contains(spaceRegex)) {
        "$namespace:\"$tag$\""
    } else {
        "$namespace:$tag$"
    }

    private fun wrapTagNHentai(namespace: String, tag: String) = if (tag.contains(spaceRegex)) {
        if (namespace == "tag") {
            """"$tag""""
        } else {
            """$namespace:"$tag""""
        }
    } else {
        "$namespace:$tag"
    }

    private fun wrapTagTsumino(namespace: String, tag: String) = if (tag.contains(spaceRegex)) {
        if (namespace == "tags") {
            "\"${tag.replace(spaceRegex, "_")}\""
        } else {
            "\"$namespace: ${tag.replace(spaceRegex, "_")}\""
        }
    } else {
        if (namespace == "tags") {
            tag
        } else {
            "$namespace:$tag"
        }
    }

    fun parseTag(tag: String) = RaisedTag(
        if (tag.startsWith("-")) {
            tag.substringAfter("-")
        } else {
            tag
        }.substringBefore(':', missingDelimiterValue = "").trimOrNull(),
        tag.substringAfter(':', missingDelimiterValue = tag).trim(),
        if (tag.startsWith("-")) TAG_TYPE_EXCLUDE else TAG_TYPE_DEFAULT,
    )

    private const val TAG_TYPE_EXCLUDE = 69 // why not

    enum class GenreColor(val color: Int) {
        DOUJINSHI_COLOR(0xFFf44336.toInt()),
        MANGA_COLOR(0xFFff9800.toInt()),
        ARTIST_CG_COLOR(0xFFfbc02d.toInt()),
        GAME_CG_COLOR(0xFF4caf50.toInt()),
        WESTERN_COLOR(0xFF8bc34a.toInt()),
        NON_H_COLOR(0xFF2196f3.toInt()),
        IMAGE_SET_COLOR(0xFF3f51b5.toInt()),
        COSPLAY_COLOR(0xFF9c27b0.toInt()),
        ASIAN_PORN_COLOR(0xFF9575cd.toInt()),
        MISC_COLOR(0xFFf06292.toInt()),
        ;
    }

    fun getLocaleSourceUtil(language: String?) = when (language) {
        "english", "eng" -> Locale.forLanguageTag("en")
        "chinese" -> Locale.forLanguageTag("zh")
        "spanish" -> Locale.forLanguageTag("es")
        "korean" -> Locale.forLanguageTag("ko")
        "russian" -> Locale.forLanguageTag("ru")
        "french" -> Locale.forLanguageTag("fr")
        "portuguese" -> Locale.forLanguageTag("pt")
        "thai" -> Locale.forLanguageTag("th")
        "german" -> Locale.forLanguageTag("de")
        "italian" -> Locale.forLanguageTag("it")
        "vietnamese" -> Locale.forLanguageTag("vi")
        "polish" -> Locale.forLanguageTag("pl")
        "hungarian" -> Locale.forLanguageTag("hu")
        "dutch" -> Locale.forLanguageTag("nl")
        else -> null
    }

    fun getGenreColor(genre: String) = when (genre) {
        "doujinshi" -> GenreColor.DOUJINSHI_COLOR
        "manga" -> GenreColor.MANGA_COLOR
        "artistcg" -> GenreColor.ARTIST_CG_COLOR
        "gamecg" -> GenreColor.GAME_CG_COLOR
        "western" -> GenreColor.WESTERN_COLOR
        "non-h" -> GenreColor.NON_H_COLOR
        "imageset" -> GenreColor.IMAGE_SET_COLOR
        "cosplay" -> GenreColor.COSPLAY_COLOR
        "asianporn" -> GenreColor.ASIAN_PORN_COLOR
        "misc" -> GenreColor.MISC_COLOR
        else -> null
    }

    private const val TAG_TYPE_DEFAULT = 1

    private val spaceRegex = "\\s".toRegex()
}
