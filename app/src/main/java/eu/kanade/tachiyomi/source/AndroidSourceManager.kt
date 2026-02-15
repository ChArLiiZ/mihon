package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.log.xLogD
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.EnhancedHttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

class AndroidSourceManager(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val sourceRepository: StubSourceRepository,
) : SourceManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val downloadManager: DownloadManager by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, Source>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    override val catalogueSources: Flow<List<CatalogueSource>> = sourcesMapFlow.map {
        it.values.filterIsInstance<CatalogueSource>()
    }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = ConcurrentHashMap<Long, Source>(
                        mapOf(
                            LocalSource.ID to LocalSource(
                                context,
                                Injekt.get(),
                                Injekt.get(),
                            ),
                        ),
                    )

                    // EXH: Register built-in EHentai sources
                    val ehSource = EHentai(EH_SOURCE_ID, false, context)
                    val exhSource = EHentai(EXH_SOURCE_ID, true, context)
                    mutableMap[EH_SOURCE_ID] = ehSource
                    mutableMap[EXH_SOURCE_ID] = exhSource
                    registerStubSource(StubSource.from(ehSource))
                    registerStubSource(StubSource.from(exhSource))

                    extensions.forEach { extension ->
                        extension.sources.forEach {
                            val wrappedSource = it.toInternalSource()
                            if (wrappedSource != null) {
                                mutableMap[it.id] = wrappedSource
                            }
                            registerStubSource(StubSource.from(it))
                        }
                    }
                    sourcesMapFlow.value = mutableMap
                    _isInitialized.value = true
                }
        }

        scope.launch {
            sourceRepository.subscribeAll()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = it
                    }
                }
        }
    }

    /**
     * Wrap extension sources with built-in enhanced sources that add metadata support.
     * Based on TachiyomiSY's source delegation mechanism.
     */
    private fun Source.toInternalSource(): Source? {
        if (this !is HttpSource) return this

        val sourceQName = this::class.qualifiedName ?: return this

        // Check if this extension source should be delegated to an enhanced source
        val matchedDelegate = DELEGATED_SOURCES.entries.firstOrNull { (key, value) ->
            if (value.factory) {
                sourceQName.startsWith(key)
            } else {
                sourceQName == key
            }
        }?.value ?: return this

        xLogD("Delegating source: %s -> %s!", sourceQName, matchedDelegate.sourceName)
        val enhancedSource = matchedDelegate.createEnhancedSource(this, context)
        return EnhancedHttpSource(this, enhancedSource)
    }

    override fun get(sourceKey: Long): Source? {
        return sourcesMapFlow.value[sourceKey]
    }

    override fun getOrStub(sourceKey: Long): Source {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    override fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<HttpSource>()

    override fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<CatalogueSource>()

    override fun getStubSources(): List<StubSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    private fun registerStubSource(source: StubSource) {
        scope.launch {
            val dbSource = sourceRepository.getStubSource(source.id)
            if (dbSource == source) return@launch
            sourceRepository.upsertStubSource(source.id, source.lang, source.name)
            if (dbSource != null) {
                downloadManager.renameSource(dbSource, source)
            }
        }
    }

    private suspend fun createStubSource(id: Long): StubSource {
        sourceRepository.getStubSource(id)?.let {
            return it
        }
        extensionManager.getSourceData(id)?.let {
            registerStubSource(it)
            return it
        }
        return StubSource(id = id, lang = "", name = "")
    }

    companion object {
        private val DELEGATED_SOURCES = listOf(
            DelegatedSource(
                sourceName = "NHentai",
                originalSourceQualifiedClassName = "eu.kanade.tachiyomi.extension.all.nhentai.NHentai",
                factory = true,
                createEnhancedSource = { delegate, ctx -> NHentai(delegate, ctx) },
            ),
        ).associateBy { it.originalSourceQualifiedClassName }

        data class DelegatedSource(
            val sourceName: String,
            val originalSourceQualifiedClassName: String,
            val factory: Boolean = false,
            val createEnhancedSource: (HttpSource, Context) -> HttpSource,
        )
    }
}
