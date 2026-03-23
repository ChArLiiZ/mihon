package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewPage
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import exh.eh.NHTagCache
import exh.eh.NHTags
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.NamespaceSource
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.DelegatedHttpSource
import exh.source.ExhPreferences
import exh.util.trimOrNull
import exh.util.urlImportFetchSearchManga
import exh.util.urlImportFetchSearchMangaSuspend
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.CacheControl
import okhttp3.Response
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder

class NHentai(delegate: HttpSource, val context: Context) :
    DelegatedHttpSource(delegate),
    MetadataSource<NHentaiSearchMetadata, Response>,
    UrlImportableSource,
    NamespaceSource,
    PagePreviewSource {
    override val metaClass = NHentaiSearchMetadata::class
    override fun newMetaInstance() = NHentaiSearchMetadata()
    override val lang = delegate.lang

    private val sourcePreferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }

    private val exhPreferences: ExhPreferences by lazy {
        Injekt.get()
    }

    private val preferredTitle: Int
        get() {
            // Prefer centralized ExhPreferences, fall back to per-source pref
            val display = exhPreferences.nhentaiTitleDisplay().get()
                .takeIf { it.isNotEmpty() }
                ?: sourcePreferences.getString(TITLE_PREF, "full")
            return when (display) {
                "full" -> NHentaiSearchMetadata.TITLE_TYPE_ENGLISH
                else -> NHentaiSearchMetadata.TITLE_TYPE_SHORT
            }
        }

    // -- AutoComplete tag filter --

    private val tagCache by lazy { NHTagCache(context) }

    override fun getFilterList(): FilterList {
        val delegateFilters = super.getFilterList()
        // Inject AutoComplete filter at the beginning of the existing filter list
        val allFilters = mutableListOf<Filter<*>>(AutoCompleteTags(tagCache.getAllTags()))
        allFilters.addAll(delegateFilters.list)
        return FilterList(allFilters)
    }

    class AutoCompleteTags(values: List<String>) :
        Filter.AutoComplete(
            name = "Tags",
            hint = "Search tags (e.g. tag:big breasts, artist:name)",
            values = values,
            skipAutoFillTags = NHTags.getNamespaces().map { "$it:" },
            validPrefixes = listOf("-"),
            state = emptyList(),
        )

    /**
     * Extract tags from gallery JSON responses and feed them into the dynamic cache.
     */
    private fun cacheTagsFromGalleries(galleries: List<JsonResponse>) {
        val newTags = galleries.flatMap { gallery ->
            gallery.tags.mapNotNull { tag ->
                val type = tag.type ?: return@mapNotNull null
                val name = tag.name ?: return@mapNotNull null
                type to name
            }
        }
        tagCache.addTags(newTags)
    }

    /**
     * Extract tags from AutoComplete filter and combine into NHentai query format.
     * E.g. ["tag:big breasts", "-artist:name"] -> "tag:\"big breasts\" -artist:\"name\""
     */
    private fun combineAutoCompleteTags(filters: FilterList): String = buildString {
        filters.list.filterIsInstance<Filter.AutoComplete>().flatMap { it.state }.forEach { tag ->
            val exclude = tag.startsWith("-")
            val cleaned = tag.removePrefix("-").trim()
            val colonIdx = cleaned.indexOf(':')
            if (colonIdx > 0) {
                val namespace = cleaned.substring(0, colonIdx)
                val value = cleaned.substring(colonIdx + 1).trim()
                if (value.isNotEmpty()) {
                    if (exclude) append("-")
                    append("$namespace:\"$value\" ")
                }
            } else if (cleaned.isNotEmpty()) {
                if (exclude) append("-")
                append("\"$cleaned\" ")
            }
        }
    }.trim()

    // Support direct URL importing
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchManga"))
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) =
        urlImportFetchSearchManga(context, query) {
            @Suppress("DEPRECATION")
            super<DelegatedHttpSource>.fetchSearchManga(page, query, filters)
        }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        // When falling back to HTML, combine autocomplete tags into the text query
        // so the extension's combineQuery still works
        val autoCompletePart = combineAutoCompleteTags(filters)
        val combinedQuery = buildString {
            if (query.isNotBlank()) append(query)
            if (autoCompletePart.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(autoCompletePart)
            }
        }
        return urlImportFetchSearchMangaSuspend(context, combinedQuery) {
            try {
                fetchJsonSearchManga(page, query, filters)
            } catch (e: Exception) {
                logcat(LogPriority.WARN) {
                    "NHentai JSON API failed for search, falling back to HTML: ${e.message}"
                }
                super<DelegatedHttpSource>.getSearchManga(page, combinedQuery, filters)
            }
        }
    }

    /**
     * Use the nhentai JSON search API so results include page count & favorites.
     * Extracts the sort parameter from the extension's filter list if present.
     */
    private suspend fun fetchJsonSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        // Combine text query with AutoComplete tag selections
        val autoCompletePart = combineAutoCompleteTags(filters)

        // Build the search query, adding language filter automatically
        val apiQuery = buildString {
            if (query.isNotBlank()) {
                append(query)
            }
            if (autoCompletePart.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(autoCompletePart)
            }
            if (isEmpty()) append("\"\"")
            // Add language filter if not already in query
            val fullQuery = toString()
            if (nhLang.isNotEmpty() && !fullQuery.contains("language:")) {
                append(" language:$nhLang")
            }
        }

        // Try to extract sort from filters (extension uses a Sort or Select filter)
        val sort = extractSortFromFilters(filters)

        val url = buildString {
            append("$baseUrl/api/galleries/search")
            append("?query=${URLEncoder.encode(apiQuery, "UTF-8")}")
            append("&page=$page")
            if (sort.isNotEmpty()) append("&sort=$sort")
        }
        return fetchJsonMangasPage(url, page)
    }

    /**
     * Extract sort parameter from the extension's filter list.
     * The nhentai extension typically uses a Select filter for sort order.
     */
    private fun extractSortFromFilters(filters: FilterList): String {
        for (filter in filters.list) {
            // Check for Sort filter
            if (filter is eu.kanade.tachiyomi.source.model.Filter.Sort) {
                val state = filter.state ?: continue
                return when (filter.values.getOrNull(state.index)?.lowercase()) {
                    "popular" -> "popular"
                    "date", "recent" -> "date"
                    else -> ""
                }
            }
            // Check for Select filter that might be a sort selector
            if (filter is eu.kanade.tachiyomi.source.model.Filter.Select<*> &&
                filter.name.lowercase().contains("sort")
            ) {
                val selected = filter.values.getOrNull(filter.state)?.toString()?.lowercase() ?: ""
                return when {
                    selected.contains("popular") -> "popular"
                    selected.contains("date") || selected.contains("recent") -> "date"
                    else -> ""
                }
            }
        }
        return ""
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        return try {
            val query = buildString {
                append("\"\"")
                if (nhLang.isNotEmpty()) append(" language:$nhLang")
            }
            val url = "$baseUrl/api/galleries/search" +
                "?query=${URLEncoder.encode(query, "UTF-8")}" +
                "&sort=popular&page=$page"
            fetchJsonMangasPage(url, page)
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "NHentai JSON API failed for popular, falling back to HTML: ${e.message}" }
            super<DelegatedHttpSource>.getPopularManga(page)
        }
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return try {
            val url = if (nhLang.isEmpty()) {
                "$baseUrl/api/galleries/all?page=$page"
            } else {
                "$baseUrl/api/galleries/search" +
                    "?query=${URLEncoder.encode("language:$nhLang", "UTF-8")}" +
                    "&sort=date&page=$page"
            }
            fetchJsonMangasPage(url, page)
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "NHentai JSON API failed for latest, falling back to HTML: ${e.message}" }
            super<DelegatedHttpSource>.getLatestUpdates(page)
        }
    }

    // -- JSON API helpers --

    /** nhentai language name for the JSON API query filter */
    private val nhLang: String
        get() = when (lang) {
            "en" -> "english"
            "ja" -> "japanese"
            "zh" -> "chinese"
            else -> "" // "all" variant
        }

    private val shortenTitleRegex = Regex("""(\[[^]]*]|[({][^)}]*[)}])""")

    private suspend fun fetchJsonMangasPage(url: String, page: Int): MangasPage {
        val response = client.newCall(GET(url)).awaitSuccess()
        val body = response.body.string()
        val searchResult = jsonParser.decodeFromString<JsonSearchResponse>(body)
        // Dynamically cache tags from loaded galleries for autocomplete
        cacheTagsFromGalleries(searchResult.result)
        val mangas = searchResult.result.map { galleryToSManga(it) }
        val hasNextPage = searchResult.totalPages != null && page < searchResult.totalPages
        return MangasPage(mangas, hasNextPage)
    }

    private fun galleryToSManga(gallery: JsonResponse): SManga = SManga.create().apply {
        url = "/g/${gallery.id}/"

        title = when (preferredTitle) {
            NHentaiSearchMetadata.TITLE_TYPE_SHORT ->
                gallery.title?.pretty
                    ?: (gallery.title?.english ?: gallery.title?.japanese)
                        ?.replace(shortenTitleRegex, "")?.trim()
                    ?: ""
            else ->
                gallery.title?.english
                    ?: gallery.title?.japanese
                    ?: gallery.title?.pretty
                    ?: ""
        }

        thumbnail_url = gallery.mediaId?.let { mid ->
            gallery.images?.cover?.type?.let { t ->
                NHentaiSearchMetadata.typeToExtension(t)?.let { ext ->
                    "https://t1.nhentai.net/galleries/$mid/cover.$ext"
                }
            }
        }

        val pageCount = gallery.numPages ?: gallery.images?.pages?.size ?: 0
        val favCount = gallery.numFavorites ?: 0
        description = buildString {
            if (pageCount > 0) appendLine("Pages: $pageCount")
            if (favCount > 0) appendLine("Favorited by: $favCount")
        }.trim()

        artist = gallery.tags.filter { it.type == "artist" }
            .mapNotNull { it.name }
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
        author = gallery.tags.filter { it.type == "group" }
            .mapNotNull { it.name }
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
            ?: artist
        genre = gallery.tags.filter { it.type == "tag" || it.type == "category" }
            .mapNotNull { it.name }
            .joinToString(", ")
        status = SManga.COMPLETED
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        val response = client.newCall(mangaDetailsRequest(manga)).awaitSuccess()
        val result = parseToManga(manga, response)

        // 從詳情頁面提取頁數和收藏數，更新 description
        val body = response.body.string()
        val json = GALLERY_JSON_REGEX.find(body)?.let { match ->
            val jsonStr = match.groupValues[1].replace(UNICODE_ESCAPE_REGEX) {
                it.groupValues[1].toInt(radix = 16).toChar().toString()
            }
            try {
                jsonParser.decodeFromString<JsonResponse>(jsonStr)
            } catch (e: Exception) {
                null
            }
        }

        if (json != null) {
            // Cache tags from detail page for autocomplete
            cacheTagsFromGalleries(listOf(json))

            val description = buildString {
                append("Pages: ${json.images?.pages?.size ?: 0}\n")
                append("Favorited by: ${json.numFavorites ?: 0}\n")
            }
            result.description = description.trim()
        }

        return result
    }

    override suspend fun parseIntoMetadata(metadata: NHentaiSearchMetadata, input: Response) {
        val body = input.body.string()
        val server = MEDIA_SERVER_REGEX.find(body)?.groupValues?.get(1)?.toInt() ?: 1
        val json = GALLERY_JSON_REGEX.find(body)!!.groupValues[1].replace(
            UNICODE_ESCAPE_REGEX,
        ) { it.groupValues[1].toInt(radix = 16).toChar().toString() }
        val jsonResponse = jsonParser.decodeFromString<JsonResponse>(json)

        with(metadata) {
            nhId = jsonResponse.id

            uploadDate = jsonResponse.uploadDate

            favoritesCount = jsonResponse.numFavorites

            mediaId = jsonResponse.mediaId

            mediaServer = server

            jsonResponse.title?.let { title ->
                japaneseTitle = title.japanese
                shortTitle = title.pretty
                englishTitle = title.english
            }

            preferredTitle = this@NHentai.preferredTitle

            jsonResponse.images?.let { images ->
                coverImageType = images.cover?.type
                images.pages.mapNotNull {
                    it.type
                }.let {
                    pageImageTypes = it
                }
                thumbnailImageType = images.thumbnail?.type
            }

            scanlator = jsonResponse.scanlator?.trimOrNull()

            tags.clear()
            jsonResponse.tags.filter {
                it.type != null && it.name != null
            }.mapTo(tags) {
                RaisedTag(
                    it.type!!,
                    it.name!!,
                    if (it.type == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE) {
                        RaisedSearchMetadata.TAG_TYPE_VIRTUAL
                    } else {
                        NHentaiSearchMetadata.TAG_TYPE_DEFAULT
                    },
                )
            }
        }
    }

    @Serializable
    data class JsonSearchResponse(
        val result: List<JsonResponse> = emptyList(),
        @SerialName("num_pages")
        val totalPages: Int? = null,
        @SerialName("per_page")
        val perPage: Int? = null,
    )

    @Serializable
    data class JsonResponse(
        val id: Long,
        @SerialName("media_id")
        val mediaId: String? = null,
        val title: JsonTitle? = null,
        val images: JsonImages? = null,
        val scanlator: String? = null,
        @SerialName("upload_date")
        val uploadDate: Long? = null,
        val tags: List<JsonTag> = emptyList(),
        @SerialName("num_pages")
        val numPages: Int? = null,
        @SerialName("num_favorites")
        val numFavorites: Long? = null,
    )

    @Serializable
    data class JsonTitle(
        val english: String? = null,
        val japanese: String? = null,
        val pretty: String? = null,
    )

    @Serializable
    data class JsonImages(
        val pages: List<JsonPage> = emptyList(),
        val cover: JsonPage? = null,
        val thumbnail: JsonPage? = null,
    )

    @Serializable
    data class JsonPage(
        @SerialName("t")
        val type: String? = null,
        @SerialName("w")
        val width: Long? = null,
        @SerialName("h")
        val height: Long? = null,
    )

    @Serializable
    data class JsonTag(
        val id: Long? = null,
        val type: String? = null,
        val name: String? = null,
        val url: String? = null,
        val count: Long? = null,
    )

    override val matchingHosts = listOf(
        "nhentai.net",
    )

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? {
        if (uri.pathSegments.firstOrNull()?.lowercase() != "g") {
            return null
        }

        return "$baseUrl/g/${uri.pathSegments[1]}/"
    }

    override suspend fun getPagePreviewList(manga: SManga, chapters: List<SChapter>, page: Int): PagePreviewPage {
        val metadata = fetchOrLoadMetadata(manga.id()) {
            client.newCall(mangaDetailsRequest(manga)).awaitSuccess()
        }
        return PagePreviewPage(
            page,
            metadata.pageImageTypes.mapIndexed { index, s ->
                PagePreviewInfo(
                    index + 1,
                    imageUrl = thumbnailUrlFromType(metadata.mediaId!!, metadata.mediaServer ?: 1, index + 1, s)!!,
                )
            },
            false,
            1,
        )
    }

    private fun thumbnailUrlFromType(
        mediaId: String,
        mediaServer: Int,
        page: Int,
        t: String,
    ) = NHentaiSearchMetadata.typeToExtension(t)?.let {
        "https://t$mediaServer.nhentai.net/galleries/$mediaId/${page}t.$it"
    }

    override suspend fun fetchPreviewImage(page: PagePreviewInfo, cacheControl: CacheControl?): Response {
        return client.newCachelessCallWithProgress(
            if (cacheControl != null) {
                GET(page.imageUrl, cache = cacheControl)
            } else {
                GET(page.imageUrl)
            },
            page,
        ).awaitSuccess()
    }

    companion object {
        const val otherId = 7309872737163460316L

        private val jsonParser = Json {
            ignoreUnknownKeys = true
        }

        private val GALLERY_JSON_REGEX = Regex(".parse\\(\"(.*)\"\\);")
        private val MEDIA_SERVER_REGEX = Regex("media_server\\s*:\\s*(\\d+)")
        private val UNICODE_ESCAPE_REGEX = Regex("\\\\u([0-9a-fA-F]{4})")
        private const val TITLE_PREF = "Display manga title as:"
    }
}
