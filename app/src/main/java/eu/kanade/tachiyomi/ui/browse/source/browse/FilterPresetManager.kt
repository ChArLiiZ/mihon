package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.Context
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Manages filter presets for a specific source.
 * Presets are stored in SharedPreferences as JSON, keyed by source ID.
 */
class FilterPresetManager(
    context: Context,
    private val sourceId: Long,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getPresets(): List<FilterPreset> {
        val raw = prefs.getString(prefsKey(), null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(FilterPreset.serializer()), raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePreset(name: String, filterList: FilterList): String {
        val filtersJson = FilterData.serializeFilterList(filterList, json)
        val preset = FilterPreset(
            id = UUID.randomUUID().toString(),
            name = name,
            filters = filtersJson,
        )
        val presets = getPresets().toMutableList()
        presets.add(preset)
        persist(presets)
        return preset.id
    }

    fun updatePresetName(id: String, newName: String) {
        val presets = getPresets().toMutableList()
        val idx = presets.indexOfFirst { it.id == id }
        if (idx != -1) {
            presets[idx] = presets[idx].copy(
                name = newName,
                updatedAt = System.currentTimeMillis(),
            )
            persist(presets)
        }
    }

    fun overwritePreset(id: String, filterList: FilterList) {
        val filtersJson = FilterData.serializeFilterList(filterList, json)
        val presets = getPresets().toMutableList()
        val idx = presets.indexOfFirst { it.id == id }
        if (idx != -1) {
            presets[idx] = presets[idx].copy(
                filters = filtersJson,
                updatedAt = System.currentTimeMillis(),
            )
            persist(presets)
        }
    }

    fun deletePreset(id: String) {
        val presets = getPresets().toMutableList()
        presets.removeAll { it.id == id }
        persist(presets)
    }

    fun applyPreset(id: String, source: CatalogueSource): FilterList? {
        val preset = getPresets().find { it.id == id } ?: return null
        return try {
            val freshFilters = source.getFilterList()
            FilterData.applyToFilterList(preset.filters, freshFilters, json)
        } catch (_: Exception) {
            null
        }
    }

    private fun prefsKey() = "${KEY_PREFIX}$sourceId"

    private fun persist(presets: List<FilterPreset>) {
        val raw = json.encodeToString(ListSerializer(FilterPreset.serializer()), presets)
        prefs.edit().putString(prefsKey(), raw).apply()
    }

    companion object {
        private const val PREFS_NAME = "filter_presets"
        private const val KEY_PREFIX = "source_"
    }
}
