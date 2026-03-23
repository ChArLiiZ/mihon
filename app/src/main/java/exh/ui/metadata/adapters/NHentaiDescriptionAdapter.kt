package exh.ui.metadata.adapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import exh.metadata.MetadataUtil
import exh.metadata.metadata.NHentaiSearchMetadata
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NHentaiDescription(
    state: State.Success,
    openMetadataViewer: () -> Unit,
    search: (String) -> Unit,
) {
    val meta = try {
        state.flatMetadata?.raise<NHentaiSearchMetadata>()
    } catch (e: Exception) {
        null
    }

    if (meta == null) return

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Category badge
        val category = meta.tags
            .filter { it.namespace == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE }
            .joinToString { it.name }
            .takeIf { it.isNotEmpty() }
        if (category != null) {
            val genreInfo = MetadataUIUtil.getGenreAndColour(context, category)
            if (genreInfo != null) {
                val (color, label) = genreInfo
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                )
            } else {
                Text(
                    text = category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                )
            }
        }

        // Upload date
        meta.uploadDate?.let { timestamp ->
            val formattedDate = MetadataUtil.EX_DATE_FORMAT.format(
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()),
            )
            MetadataRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = formattedDate,
            )
        }

        // Uploader/scanlator (clickable to search)
        meta.scanlator?.takeIf { it.isNotBlank() }?.let { uploader ->
            MetadataRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = uploader,
                modifier = Modifier.clickable { search("\"$uploader\"") },
            )
        }

        // Stats row: favorites + pages
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Favorites
            meta.favoritesCount?.let { favorites ->
                MetadataRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    text = favorites.toString(),
                )
            }

            // Page count
            val pageCount = meta.pageImageTypes.size
            if (pageCount > 0) {
                MetadataRow(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    text = context.pluralStringResource(SYMR.plurals.num_pages, pageCount, pageCount),
                )
            }
        }

        // Language (from tags)
        val language = meta.tags
            .filter { it.namespace == "language" }
            .joinToString { it.name.replaceFirstChar { c -> c.uppercase() } }
            .takeIf { it.isNotEmpty() }
        if (language != null) {
            Text(
                text = language,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // More info button
        TextButton(onClick = openMetadataViewer) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(SYMR.strings.more_info))
        }
    }
}

@Composable
private fun MetadataRow(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
