package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val CoverPlaceholderColor = androidx.compose.ui.graphics.Color(0x1F888888)

@Composable
fun SimilarMangaSection(
    manga: List<Manga>,
    isLoading: Boolean,
    noPinnedSources: Boolean,
    onExpand: () -> Unit,
    onMangaClick: (Manga) -> Unit,
    getSourceName: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    if (expanded && manga.isEmpty() && !isLoading) {
                        onExpand()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.similar_manga),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            when {
                isLoading && manga.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                noPinnedSources -> {
                    Text(
                        text = stringResource(MR.strings.no_pinned_sources_for_similar),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                manga.isEmpty() && !isLoading -> {
                    Text(
                        text = stringResource(MR.strings.no_similar_manga),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = manga,
                            key = { it.id },
                        ) { item ->
                            SimilarMangaItem(
                                manga = item,
                                sourceName = getSourceName(item.source),
                                onClick = { onMangaClick(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarMangaItem(
    manga: Manga,
    sourceName: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = MangaCover(
                mangaId = manga.id,
                sourceId = manga.source,
                isMangaFavorite = manga.favorite,
                url = manga.thumbnailUrl,
                lastModified = manga.coverLastModified,
            ),
            contentDescription = manga.title,
            placeholder = ColorPainter(CoverPlaceholderColor),
            modifier = Modifier
                .size(width = 100.dp, height = 140.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = manga.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = sourceName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
