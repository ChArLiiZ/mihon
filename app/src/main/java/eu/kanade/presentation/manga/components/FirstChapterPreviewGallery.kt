package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.manga.PagePreview
import java.io.File
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

private val CoverPlaceholderColor = androidx.compose.ui.graphics.Color(0x1F888888)

@Composable
fun FirstChapterPreviewGallery(
    pages: List<PagePreview>,
    totalPageCount: Int,
    visibleCount: Int,
    isLoading: Boolean,
    error: String?,
    onExpand: () -> Unit,
    onLoadMore: () -> Unit,
    onPageClick: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()),
    ) {
        // Header row - clickable to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    if (expanded && pages.isEmpty() && !isLoading) {
                        onExpand()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.first_chapter_preview),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Expanded content
        if (expanded) {
            when {
                // Loading state (initial load)
                isLoading && pages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // Error state
                error != null && pages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(MR.strings.first_chapter_preview_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onExpand) {
                            Text(text = stringResource(MR.strings.action_retry))
                        }
                    }
                }
                // Gallery content
                pages.isNotEmpty() -> {
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val columns = 3
                    val horizontalPadding = 16.dp
                    val spacing = MaterialTheme.padding.small
                    val itemWidth = (screenWidth - horizontalPadding * 2 - spacing * (columns - 1)) / columns

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        pages.forEach { page ->
                            Column(
                                modifier = Modifier.width(itemWidth),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val imageModel = page.imageUrl.let { url ->
                                    val file = File(url)
                                    if (file.exists()) file else url
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = stringResource(
                                        MR.strings.chapter_progress,
                                        page.pageIndex + 1,
                                    ),
                                    placeholder = ColorPainter(CoverPlaceholderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .clickable { onPageClick(page.pageIndex) },
                                    contentScale = ContentScale.Crop,
                                )
                                Text(
                                    text = "${page.pageIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }

                    // Load more / loading indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                            visibleCount < totalPageCount -> {
                                OutlinedButton(onClick = onLoadMore) {
                                    Text(
                                        text = stringResource(MR.strings.load_more) +
                                            " (${visibleCount}/${totalPageCount})",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
