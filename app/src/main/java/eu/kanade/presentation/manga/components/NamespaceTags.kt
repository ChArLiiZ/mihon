package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchTag

/**
 * 從 FlatMetadata 建立 namespace 分組的 tag 顯示組件。
 */
@Composable
fun SearchMetadataChips(
    flatMetadata: FlatMetadata?,
    modifier: Modifier = Modifier,
    onTagClick: ((SearchTag) -> Unit)? = null,
) {
    if (flatMetadata == null) return
    val tags = flatMetadata.tags
    if (tags.isEmpty()) return

    NamespaceTags(
        tags = tags,
        modifier = modifier,
        onTagClick = onTagClick,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NamespaceTags(
    tags: List<SearchTag>,
    modifier: Modifier = Modifier,
    onTagClick: ((SearchTag) -> Unit)? = null,
) {
    val grouped = tags.groupBy { it.namespace.orEmpty() }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (namespace, namespaceTags) ->
            if (namespace.isNotBlank()) {
                Text(
                    text = namespace,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                namespaceTags.forEach { tag ->
                    TagsChip(
                        tag = tag,
                        onClick = onTagClick?.let { { it(tag) } },
                    )
                }
            }
        }
    }
}

@Composable
fun TagsChip(
    tag: SearchTag,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    SuggestionChip(
        modifier = modifier,
        onClick = onClick ?: {},
        label = {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
