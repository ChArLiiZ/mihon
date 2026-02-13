package eu.kanade.tachiyomi.ui.readlater

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class ReadLaterScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { ReadLaterScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.read_later),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = {},
                )
            },
        ) { paddingValues ->
            when (val s = state) {
                is ReadLaterScreenModel.State.Loading -> LoadingScreen()
                is ReadLaterScreenModel.State.Success -> {
                    if (s.manga.isEmpty()) {
                        EmptyScreen(
                            stringRes = MR.strings.read_later_empty,
                            modifier = Modifier.padding(paddingValues),
                        )
                    } else {
                        ReadLaterContent(
                            manga = s.manga,
                            contentPadding = paddingValues,
                            onMangaClick = { navigator.push(MangaScreen(it.id)) },
                            onRemoveClick = { screenModel.removeFromReadLater(it.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadLaterContent(
    manga: List<Manga>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onRemoveClick: (Manga) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        items(
            items = manga,
            key = { it.id },
        ) { item ->
            MangaComfortableGridItem(
                isSelected = false,
                title = item.title,
                titleMaxLines = 2,
                coverData = MangaCover(
                    mangaId = item.id,
                    sourceId = item.source,
                    isMangaFavorite = item.favorite,
                    url = item.thumbnailUrl,
                    lastModified = item.coverLastModified,
                ),
                coverAlpha = 1f,
                coverBadgeStart = {},
                coverBadgeEnd = {
                    IconButton(
                        onClick = { onRemoveClick(item) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                onLongClick = { onRemoveClick(item) },
                onClick = { onMangaClick(item) },
            )
        }
    }
}
