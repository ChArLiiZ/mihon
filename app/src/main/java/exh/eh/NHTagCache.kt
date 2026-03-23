package exh.eh

import android.content.Context
import android.content.SharedPreferences

/**
 * Dynamic tag cache for NHentai autocomplete suggestions.
 * Stores discovered tags in SharedPreferences, merged with the static [NHTags] seed list.
 *
 * Tags are stored as "namespace:name" strings (e.g. "tag:big breasts", "artist:shindol").
 * Only namespaces relevant to NHentai are cached: tag, artist, group, parody, character,
 * language, category.
 */
class NHTagCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val validNamespaces = setOf(
        "tag", "artist", "group", "parody", "character", "language", "category",
    )

    /**
     * Returns a combined list of namespace prefixes + static seed tags + dynamically cached tags.
     * Duplicates are removed and the result is sorted alphabetically within each namespace.
     */
    fun getAllTags(): List<String> {
        val namespaces = NHTags.getNamespaces().map { "$it:" }
        val staticTags = NHTags.getAllTags()
        val dynamicTags = loadCachedTags()
        val combined = (staticTags + dynamicTags).distinct().sorted()
        return namespaces + combined
    }

    /**
     * Add newly discovered tags to the cache.
     * @param tags list of pairs (type, name) from the NHentai JSON API response
     */
    fun addTags(tags: List<Pair<String, String>>) {
        if (tags.isEmpty()) return

        val existing = loadCachedTags().toMutableSet()
        var changed = false
        for ((type, name) in tags) {
            if (type !in validNamespaces || name.isBlank()) continue
            val formatted = "$type:$name"
            if (existing.add(formatted)) {
                changed = true
            }
        }
        if (changed) {
            saveCachedTags(existing)
        }
    }

    private fun loadCachedTags(): Set<String> {
        val raw = prefs.getString(KEY_TAGS, null) ?: return emptySet()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.toSet()
    }

    private fun saveCachedTags(tags: Set<String>) {
        prefs.edit()
            .putString(KEY_TAGS, tags.joinToString(SEPARATOR))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "nhentai_tag_cache"
        private const val KEY_TAGS = "cached_tags"
        private const val SEPARATOR = "\n"
    }
}
