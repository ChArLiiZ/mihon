package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FilterPreset(
    val id: String,
    val name: String,
    val filters: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
sealed class FilterData {
    abstract val name: String

    @Serializable
    data class Header(override val name: String) : FilterData()

    @Serializable
    data class Separator(override val name: String = "") : FilterData()

    @Serializable
    data class CheckBoxData(
        override val name: String,
        val state: Boolean,
    ) : FilterData()

    @Serializable
    data class TriStateData(
        override val name: String,
        val state: Int,
    ) : FilterData()

    @Serializable
    data class TextData(
        override val name: String,
        val state: String,
    ) : FilterData()

    @Serializable
    data class SelectData(
        override val name: String,
        val values: List<String>,
        val state: Int,
    ) : FilterData()

    @Serializable
    data class SortData(
        override val name: String,
        val values: List<String>,
        val selection: Int,
        val ascending: Boolean,
    ) : FilterData()

    @Serializable
    data class GroupData(
        override val name: String,
        val filters: List<FilterData>,
    ) : FilterData()

    companion object {
        fun fromFilter(filter: Filter<*>): FilterData {
            return when (filter) {
                is Filter.Header -> Header(filter.name)
                is Filter.Separator -> Separator()
                is Filter.CheckBox -> CheckBoxData(filter.name, filter.state)
                is Filter.TriState -> TriStateData(filter.name, filter.state)
                is Filter.Text -> TextData(filter.name, filter.state)
                is Filter.Select<*> -> SelectData(
                    name = filter.name,
                    values = filter.values.map { it.toString() },
                    state = filter.state,
                )
                is Filter.Sort -> SortData(
                    name = filter.name,
                    values = filter.values.toList(),
                    selection = filter.state?.index ?: 0,
                    ascending = filter.state?.ascending ?: true,
                )
                is Filter.Group<*> -> GroupData(
                    name = filter.name,
                    filters = filter.state.filterIsInstance<Filter<*>>().map { fromFilter(it) },
                )
                // SY -->
                is Filter.AutoComplete -> AutoCompleteData(
                    name = filter.name,
                    state = filter.state,
                )
                // SY <--
                else -> Header(filter.name)
            }
        }

        fun serializeFilterList(filterList: FilterList, json: Json): String {
            val data = filterList.list.map { fromFilter(it) }
            return json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(serializer()),
                data,
            )
        }

        /**
         * Apply saved filter states onto a fresh FilterList from the source.
         * This preserves the original Filter subclass types (including custom ones)
         * while restoring only the user-set state values.
         */
        fun applyToFilterList(
            savedJson: String,
            sourceFilters: FilterList,
            json: Json,
        ): FilterList {
            val saved = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(serializer()),
                savedJson,
            )
            applyStates(saved, sourceFilters.list)
            return sourceFilters
        }

        private fun applyStates(saved: List<FilterData>, targets: List<Filter<*>>) {
            for (i in targets.indices) {
                if (i >= saved.size) break
                val s = saved[i]
                val t = targets[i]
                when {
                    s is CheckBoxData && t is Filter.CheckBox -> t.state = s.state
                    s is TriStateData && t is Filter.TriState -> t.state = s.state
                    s is TextData && t is Filter.Text -> t.state = s.state
                    s is SelectData && t is Filter.Select<*> -> {
                        if (s.state < t.values.size) t.state = s.state
                    }
                    s is SortData && t is Filter.Sort -> {
                        t.state = Filter.Sort.Selection(s.selection, s.ascending)
                    }
                    s is GroupData && t is Filter.Group<*> -> {
                        applyStates(s.filters, t.state.filterIsInstance<Filter<*>>())
                    }
                    // SY -->
                    s is AutoCompleteData && t is Filter.AutoComplete -> t.state = s.state
                    // SY <--
                }
            }
        }
    }

    // SY -->
    @Serializable
    data class AutoCompleteData(
        override val name: String,
        val state: List<String>,
    ) : FilterData()
    // SY <--
}
