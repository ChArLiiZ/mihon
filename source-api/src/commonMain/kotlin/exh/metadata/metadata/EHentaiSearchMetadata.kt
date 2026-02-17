package exh.metadata.metadata

import android.content.Context
import androidx.core.net.toUri
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.metadata.MetadataUtil
import exh.pref.DelegateSourcePreferences
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale

@Serializable
class EHentaiSearchMetadata : RaisedSearchMetadata() {
    var gId: String?
        get() = indexedExtra
        set(value) {
            indexedExtra = value
        }

    var gToken: String? = null
    var exh: Boolean? = null
    var thumbnailUrl: String? = null

    var title by titleDelegate(TITLE_TYPE_TITLE)
    var altTitle by titleDelegate(TITLE_TYPE_ALT_TITLE)

    var genre: String? = null

    var datePosted: Long? = null
    var parent: String? = null

    var visible: String? = null // Not a boolean
    var language: String? = null
    var translated: Boolean? = null
    var size: Long? = null
    var length: Int? = null
    var favorites: Int? = null
    var ratingCount: Int? = null
    var averageRating: Double? = null

    var aged: Boolean = false
    var lastUpdateCheck: Long = 0

    override fun createMangaInfo(manga: SManga): SManga {
        val key = gId?.let { gId ->
            gToken?.let { gToken ->
                idAndTokenToUrl(gId, gToken)
            }
        }
        val cover = thumbnailUrl

        // No title bug?
        val title = altTitle
            ?.takeIf { Injekt.get<DelegateSourcePreferences>().useJapaneseTitle().get() } // todo
            ?: title



        // KMK -->
        val description = StringBuilder()
        description.append("id: $gId")
        description.append("\n")
        description.append("token: $gToken")
        description.append("\n")
        description.append("isExh: $exh")
        description.append("\n")
        description.append("pages: $length")
        description.append("\n")
        description.append("size: ${size?.let { MetadataUtil.humanReadableByteCount(it, true) }}")
        description.append("\n")
        description.append("posted: ${datePosted?.let { MetadataUtil.EX_DATE_FORMAT.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())) }}")
        description.append("\n")
        description.append("visible: $visible")
        description.append("\n")
        description.append("language: $language")
        description.append("\n")
        description.append("translated: $translated")
        description.append("\n")
        description.append("ratingCount: $ratingCount")
        description.append("\n")
        description.append("averageRating: $averageRating")
        description.append("\n")
        description.append("aged: $aged")
        description.append("\n")
        description.append("lastUpdateCheck: $lastUpdateCheck")
        // KMK <--

        val artist = tags.ofNamespace(EH_ARTIST_NAMESPACE).joinToString { it.name }

        val genre = tags.ofNamespace(EH_GENRE_NAMESPACE).firstOrNull()?.name?.let { genre ->
            genre.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        val authors = tags.ofNamespace(EH_WRITER_NAMESPACE).joinToString { it.name }

        // Try to automatically identify if it is ongoing, we try not to be too lenient here to avoid making mistakes
        // We default to completed
        var status = SManga.COMPLETED
        title?.let { t ->
            MetadataUtil.ONGOING_SUFFIX.find {
                t.endsWith(it, ignoreCase = true)
            }?.let {
                status = SManga.ONGOING
            }
        }

        return manga.copy(
            title = title ?: manga.title,
            thumbnail_url = cover,
            artist = artist,
            author = authors,
            description = description.toString(),
            genre = genre,
            status = status,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(gId) { stringResource(SYMR.strings.id) },
                getItem(gToken) { stringResource(SYMR.strings.token) },
                getItem(exh) { stringResource(SYMR.strings.is_exhentai_gallery) },
                getItem(thumbnailUrl) { stringResource(SYMR.strings.thumbnail_url) },
                getItem(title) { stringResource(MR.strings.title) },
                getItem(altTitle) { stringResource(SYMR.strings.alt_title) },
                getItem(genre) { stringResource(SYMR.strings.genre) },
                getItem(
                    datePosted,
                    {
                        MetadataUtil.EX_DATE_FORMAT
                            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                    },
                ) {
                    stringResource(SYMR.strings.date_posted)
                },
                getItem(parent) { stringResource(SYMR.strings.parent) },
                getItem(visible) { stringResource(SYMR.strings.visible) },
                getItem(language) { stringResource(SYMR.strings.language) },
                getItem(translated) { stringResource(SYMR.strings.translated) },
                getItem(size, { MetadataUtil.humanReadableByteCount(it, true) }) {
                    stringResource(SYMR.strings.gallery_size)
                },
                getItem(length) { stringResource(SYMR.strings.page_count) },
                getItem(favorites) { stringResource(SYMR.strings.total_favorites) },
                getItem(ratingCount) { stringResource(SYMR.strings.total_ratings) },
                getItem(averageRating) { stringResource(SYMR.strings.average_rating) },
                getItem(aged) { stringResource(SYMR.strings.aged) },
                getItem(
                    lastUpdateCheck,
                    {
                        MetadataUtil.EX_DATE_FORMAT
                            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                    },
                ) { stringResource(SYMR.strings.last_update_check) },
            )
        }
    }

    companion object {
        private const val TITLE_TYPE_TITLE = 0
        private const val TITLE_TYPE_ALT_TITLE = 1

        const val TAG_TYPE_NORMAL = 0
        const val TAG_TYPE_LIGHT = 1
        const val TAG_TYPE_WEAK = 2

        const val EH_GENRE_NAMESPACE = "genre"
        private const val EH_ARTIST_NAMESPACE = "artist"
        private const val EH_WRITER_NAMESPACE = "writer" // KMK: Added missing constant
        private const val EH_GROUP_NAMESPACE = "group"
        const val EH_LANGUAGE_NAMESPACE = "language"
        const val EH_META_NAMESPACE = "meta"
        const val EH_UPLOADER_NAMESPACE = "uploader"
        const val EH_VISIBILITY_NAMESPACE = "visibility"

        private fun splitGalleryUrl(url: String) =
            url.let {
                // Only parse URL if is full URL
                val pathSegments = if (it.startsWith("http")) {
                    it.toUri().pathSegments
                } else {
                    it.split('/')
                }
                pathSegments.filterNot(String::isNullOrBlank)
            }

        fun galleryId(url: String): String = splitGalleryUrl(url)[1]

        fun galleryToken(url: String): String =
            splitGalleryUrl(url)[2]

        fun normalizeUrl(url: String) =
            idAndTokenToUrl(galleryId(url), galleryToken(url))

        fun idAndTokenToUrl(id: String, token: String) =
            "/g/$id/$token/?nw=always"
    }
}
