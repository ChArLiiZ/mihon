package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.source.PagePreviewPage
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okio.Source
import okio.buffer
import okio.sink
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

/**
 * 頁面預覽快取，使用 DiskLruCache 儲存頁面預覽列表和圖片。
 * 僅用於支援 PagePreviewSource 的來源（EHentai/NHentai）。
 */
class PagePreviewCache(private val context: Context) {

    companion object {
        const val PARAMETER_CACHE_DIRECTORY = "page_preview_disk_cache"
        const val PARAMETER_APP_VERSION = 2
        const val PARAMETER_VALUE_COUNT = 1
    }

    private val json: Json by injectLazy()

    private var diskCache = setupDiskCache(75)

    private val cacheDir: File
        get() = diskCache.directory

    private val realSize: Long
        get() = DiskUtil.getDirectorySize(cacheDir)

    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    private fun setupDiskCache(cacheSize: Long): DiskLruCache {
        return DiskLruCache.open(
            File(context.cacheDir, PARAMETER_CACHE_DIRECTORY),
            PARAMETER_APP_VERSION,
            PARAMETER_VALUE_COUNT,
            cacheSize * 1024 * 1024,
        )
    }

    fun getPageListFromCache(manga: Manga, chapterIds: List<Long>, page: Int): PagePreviewPage {
        val key = DiskUtil.hashKeyForDisk(getKey(manga, chapterIds, page))
        return diskCache.get(key).use {
            json.decodeFromString(it.getString(0))
        }
    }

    fun putPageListToCache(manga: Manga, chapterIds: List<Long>, pages: PagePreviewPage) {
        val cachedValue = json.encodeToString(pages)
        var editor: DiskLruCache.Editor? = null

        try {
            val key = DiskUtil.hashKeyForDisk(getKey(manga, chapterIds, pages.page))
            editor = diskCache.edit(key) ?: return

            editor.newOutputStream(0).sink().buffer().use {
                it.write(cachedValue.toByteArray())
                it.flush()
            }

            diskCache.flush()
            editor.commit()
            editor.abortUnlessCommitted()
        } catch (e: Exception) {
            // Ignore
        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    fun isImageInCache(imageUrl: String): Boolean {
        return try {
            diskCache.get(DiskUtil.hashKeyForDisk(imageUrl)) != null
        } catch (e: IOException) {
            false
        }
    }

    fun getImageFile(imageUrl: String): File {
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    @Throws(IOException::class)
    fun putImageToCache(imageUrl: String, source: Source) {
        var editor: DiskLruCache.Editor? = null

        try {
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(key) ?: throw IOException("Unable to edit key")

            source.buffer().use { bufferedSource ->
                editor.newOutputStream(0).sink().buffer().use { bufferedSink ->
                    bufferedSink.writeAll(bufferedSource)
                    bufferedSink.flush()
                }
            }

            diskCache.flush()
            editor.commit()
        } finally {
            source.close()
            editor?.abortUnlessCommitted()
        }
    }

    fun clear(): Int {
        var deletedFiles = 0
        cacheDir.listFiles()?.forEach {
            if (removeFileFromCache(it.name)) {
                deletedFiles++
            }
        }
        return deletedFiles
    }

    private fun removeFileFromCache(file: String): Boolean {
        if (file == "journal" || file.startsWith("journal.")) {
            return false
        }

        return try {
            val key = file.substringBeforeLast(".")
            diskCache.remove(key)
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to remove file from cache" }
            false
        }
    }

    private fun getKey(manga: Manga, chapterIds: List<Long>, page: Int): String {
        return "${manga.id}_${chapterIds.joinToString(separator = "-")}_$page"
    }
}
