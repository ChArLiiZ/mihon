package eu.kanade.tachiyomi.data.backup.restore

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupDecoder
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionRepos
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.restore.restorers.CategoriesRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.ExtensionRepoRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.MangaRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.PreferenceRestorer
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
    private val isSync: Boolean,

    private val categoriesRestorer: CategoriesRestorer = CategoriesRestorer(),
    private val preferenceRestorer: PreferenceRestorer = PreferenceRestorer(context),
    private val extensionRepoRestorer: ExtensionRepoRestorer = ExtensionRepoRestorer(),
    private val mangaRestorer: MangaRestorer = MangaRestorer(),
) {

    private val restoreAmount = AtomicInteger(0)
    private val restoreProgress = AtomicInteger(0)
    private val errors = Collections.synchronizedList(mutableListOf<Pair<Date, String>>())

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()

    suspend fun restore(uri: Uri, options: RestoreOptions) {
        restoreAmount.set(0)
        restoreProgress.set(0)
        synchronized(errors) {
            errors.clear()
        }

        val startTime = System.currentTimeMillis()

        restoreFromFile(uri, options)

        val time = System.currentTimeMillis() - startTime
        val errorCount = synchronized(errors) { errors.size }

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(
            time,
            errorCount,
            logFile.parent,
            logFile.name,
            isSync,
        )
    }

    private suspend fun restoreFromFile(uri: Uri, options: RestoreOptions) {
        val backup = BackupDecoder(context).decode(uri)

        // Store source mapping for error messages
        val backupMaps = backup.backupSources
        sourceMapping = backupMaps.associate { it.sourceId to it.name }

        val amount = buildRestoreAmount(backup, options)
        restoreAmount.set(amount)

        coroutineScope {
            // Categories must be restored first as manga restoration depends on them existing in DB
            if (options.categories) {
                ensureActive()
                categoriesRestorer(backup.backupCategories)
                showProgress(
                    context.stringResource(MR.strings.categories),
                )
            }

            if (options.appSettings) {
                restoreAppPreferences(backup.backupPreferences, backup.backupCategories.takeIf { options.categories })
            }
            if (options.sourceSettings) {
                restoreSourcePreferences(backup.backupSourcePreferences)
            }
            if (options.libraryEntries) {
                restoreManga(backup.backupManga, if (options.categories) backup.backupCategories else emptyList())
            }
            if (options.extensionRepoSettings) {
                restoreExtensionRepos(backup.backupExtensionRepo)
            }

            // TODO: optionally trigger online library + tracker update
        }
    }

    private fun CoroutineScope.restoreManga(
        backupMangas: List<BackupManga>,
        backupCategories: List<BackupCategory>,
    ) = launch {
        mangaRestorer.sortByNew(backupMangas)
            .forEach {
                ensureActive()

                try {
                    mangaRestorer.restore(it, backupCategories)
                } catch (e: Exception) {
                    val sourceName = sourceMapping[it.source] ?: it.source.toString()
                    errors.add(Date() to "${it.title} [$sourceName]: ${e.message}")
                }

                showProgress(it.title)
            }
    }

    private fun CoroutineScope.restoreAppPreferences(
        preferences: List<BackupPreference>,
        categories: List<BackupCategory>?,
    ) = launch {
        ensureActive()
        preferenceRestorer.restoreApp(
            preferences,
            categories,
        )

        showProgress(
            context.stringResource(MR.strings.app_settings),
        )
    }

    private fun CoroutineScope.restoreSourcePreferences(preferences: List<BackupSourcePreferences>) = launch {
        ensureActive()
        preferenceRestorer.restoreSource(preferences)

        showProgress(
            context.stringResource(MR.strings.source_settings),
        )
    }

    private fun CoroutineScope.restoreExtensionRepos(
        backupExtensionRepo: List<BackupExtensionRepos>,
    ) = launch {
        backupExtensionRepo
            .forEach {
                ensureActive()

                try {
                    extensionRepoRestorer(it)
                } catch (e: Exception) {
                    errors.add(Date() to "Error Adding Repo: ${it.name} : ${e.message}")
                }

                showProgress(
                    context.stringResource(MR.strings.extensionRepo_settings),
                )
            }
    }

    private fun writeErrorLog(): File {
        try {
            val snapshot = synchronized(errors) { errors.toList() }
            if (snapshot.isNotEmpty()) {
                val file = context.createFileInCacheDir("mihon_restore_error.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { out ->
                    snapshot.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }

    private fun buildRestoreAmount(
        backup: Backup,
        options: RestoreOptions,
    ): Int {
        var amount = 0
        if (options.libraryEntries) amount += backup.backupManga.size
        if (options.categories) amount += 1
        if (options.appSettings) amount += 1
        if (options.extensionRepoSettings) amount += backup.backupExtensionRepo.size
        if (options.sourceSettings) amount += 1
        return amount
    }

    private fun showProgress(content: String) {
        val progress = restoreProgress.incrementAndGet()
        notifier.showRestoreProgress(content, progress, restoreAmount.get(), isSync)
    }
}
