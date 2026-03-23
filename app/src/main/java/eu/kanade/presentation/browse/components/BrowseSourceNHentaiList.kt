package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.animateItemFastScroll
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.util.SourceTagsUtil
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.sy.SYMR

@Composable
fun BrowseSourceNHentaiList(
    pagingItems: LazyPagingItems<StateFlow<Pair<Manga, RaisedSearchMetadata?>>>,
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
            key = pagingItems.itemKey { it.value.first.id },
            contentType = pagingItems.itemContentType { "manga" },
        ) { index ->
            val pair by pagingItems[index]?.collectAsState() ?: return@items
            val manga = pair.first
            val metadata = pair.second as? NHentaiSearchMetadata
            BrowseSourceNHentaiListItem(
                manga = manga,
                metadata = metadata,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
                modifier = Modifier.animateItemFastScroll(),
            )
        }
    }
}

@Composable
fun BrowseSourceNHentaiListItem(
    manga: Manga,
    metadata: NHentaiSearchMetadata?,
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
            Box {
                val colorFilter = remember(manga.favorite, manga.readLater) {
                    if (manga.favorite || manga.readLater) {
                        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                            androidx.compose.ui.graphics.ColorMatrix().apply {
                                setToSaturation(0f)
                            },
                        )
                    } else {
                        null
                    }
                }

                MangaCover.Book(
                    data = manga,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    colorFilter = colorFilter,
                )
                Column(modifier = Modifier.padding(4.dp)) {
                    InLibraryBadge(enabled = manga.favorite)
                    ReadLaterBadge(enabled = manga.readLater && !manga.favorite)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Title and Artist
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

                // Parse fallback values from manga.description when metadata is unavailable
                // Extension's mangaDetailsParse sets description like "Pages: 25\nFavorited by: 123\n..."
                val descPageCount = remember(manga.description) {
                    manga.description?.let {
                        Regex("""Pages:\s*(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull()
                    }
                }
                val descFavorites = remember(manga.description) {
                    manga.description?.let {
                        Regex("""Favorited by:\s*(\d+)""").find(it)?.groupValues?.get(1)?.toLongOrNull()
                    }
                }

                // Category, Pages and Favorites
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Category badge
                        val category = metadata?.tags
                            ?.firstOrNull {
                                it.namespace == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE
                            }
                            ?.name
                            ?: manga.genre?.firstOrNull()
                        if (category != null) {
                            val color = SourceTagsUtil.getGenreColor(category)?.color
                            Text(
                                text = category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (color != null) {
                                    Color(color)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .background(
                                        color = if (color != null) {
                                            Color(color).copy(alpha = 0.2f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }

                        // Page count: prefer metadata, fallback to description
                        val pageCount = metadata?.pageImageTypes?.size
                            ?: descPageCount
                            ?: 0
                        if (pageCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = LocalContext.current.pluralStringResource(
                                        SYMR.plurals.num_pages,
                                        pageCount,
                                        pageCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Favorites count: prefer metadata, fallback to description
                    val favorites = metadata?.favoritesCount
                        ?: descFavorites
                    if (favorites != null && favorites > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = favorites.toString(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
