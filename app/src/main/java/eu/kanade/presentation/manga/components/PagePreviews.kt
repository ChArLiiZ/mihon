package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import eu.kanade.domain.manga.model.PagePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.floor

/**
 * 頁面預覽的密封狀態類。
 */
sealed class PagePreviewState {
    data object Unused : PagePreviewState()
    data object Loading : PagePreviewState()
    data class Success(
        val pagePreviews: List<PagePreview>,
        val hasNextPage: Boolean = false,
        val pageCount: Int? = null,
    ) : PagePreviewState()
    data class Error(val error: Throwable) : PagePreviewState()
}

/**
 * 頁面預覽主要組件。
 */
@Composable
fun PagePreviews(
    pagePreviewState: PagePreviewState,
    onOpenPage: (Int) -> Unit,
    onMorePreviewsClicked: () -> Unit,
    rowCount: Int = 2,
) {
    Column(Modifier.fillMaxWidth()) {
        var maxWidth by remember { mutableStateOf(Dp.Hairline) }
        val density = LocalDensity.current

        when {
            pagePreviewState is PagePreviewState.Loading || maxWidth == Dp.Hairline -> {
                PagePreviewLoading(setMaxWidth = { maxWidth = it })
            }
            pagePreviewState is PagePreviewState.Success -> {
                val itemPerRowCount = floor(maxWidth / 120.dp).toInt().coerceAtLeast(1)
                pagePreviewState.pagePreviews
                    .take(rowCount * itemPerRowCount)
                    .chunked(itemPerRowCount)
                    .forEach { rowItems ->
                        PagePreviewRow(
                            onOpenPage = onOpenPage,
                            items = remember(rowItems) { rowItems.toImmutableList() },
                        )
                    }
                PagePreviewMore(onMorePreviewsClicked)
            }
            else -> {}
        }
    }
}

@Composable
private fun PagePreviewLoading(
    setMaxWidth: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .onSizeChanged { setMaxWidth(with(density) { it.width.toDp() }) },
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun PagePreviewRow(
    onOpenPage: (Int) -> Unit,
    items: ImmutableList<PagePreview>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { preview ->
            SubcomposeAsyncImage(
                model = preview,
                contentDescription = "Page ${preview.index + 1}",
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onOpenPage(preview.index) },
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                    )
                },
            )
        }
    }
}

@Composable
private fun PagePreviewMore(
    onMorePreviewsClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(onClick = onMorePreviewsClicked) {
            Text(
                text = "更多頁面預覽",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
