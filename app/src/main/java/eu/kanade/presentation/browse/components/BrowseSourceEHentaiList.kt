package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.util.SourceTagsUtil
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import java.time.Instant
import java.time.ZoneId

@Composable
fun BrowseSourceEHentaiList(
    pagingItems: LazyPagingItems<StateFlow<Manga>>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.value.id },
            contentType = pagingItems.itemContentType { "manga" },
        ) { index ->
            val item by pagingItems[index]?.collectAsState() ?: return@items
            BrowseSourceEHentaiListItem(
                manga = item,
                metadata = null, // TODO: Pass metadata when available
                onClick = { onMangaClick(item) },
                onLongClick = { onMangaLongClick(item) },
                modifier = Modifier.animateItemFastScroll(),
            )
        }
    }
}

@Composable
fun BrowseSourceEHentaiListItem(
    manga: Manga,
    metadata: EHentaiSearchMetadata?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier.height(120.dp).padding(8.dp),
        ) {
            MangaCover.Book(
                data = manga,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Title and Uploader
                Column {
                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val authors = manga.author?.takeUnless { it.isEmpty() } ?: manga.artist
                    if (!authors.isNullOrEmpty()) {
                        Text(
                            text = authors,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Tags/Genre and Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val genre = metadata?.genre ?: manga.genre?.firstOrNull()
                    if (genre != null) {
                        val color = SourceTagsUtil.getGenreColor(genre)?.color
                        Text(
                            text = genre.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (color != null) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    color = if (color != null) Color(color).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Rating if available (hard without metadata, maybe parsing description?)
                    if (metadata != null && metadata.averageRating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format("%.2f", metadata.averageRating), // Simple formatting
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                
                // Date posted if available
                // val date = metadata?.datePosted ?: manga.dateAdded
                // if (date > 0) {
                     // Display date...
                // }
            }
        }
    }
}
