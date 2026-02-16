package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = Icons.Outlined.CollectionsBookmark,
        )
    }
}

@Composable
internal fun ReadLaterBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = Icons.Outlined.Schedule,
            color = MaterialTheme.colorScheme.tertiary,
            iconColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
internal fun NHentaiMetadataBadges(manga: Manga) {
    val description = manga.description ?: return

    // 從 description 解析頁數和收藏數
    val pageCountMatch = Regex("""Pages:\s*(\d+)""").find(description)
    val favoritesMatch = Regex("""Favorited by:\s*(\d+)""").find(description)

    if (pageCountMatch == null && favoritesMatch == null) return

    Row {
        pageCountMatch?.groupValues?.get(1)?.let { pageCount ->
            Badge(
                text = pageCount,
                imageVector = Icons.Outlined.FilterNone,
                color = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.onSecondary,
                iconColor = MaterialTheme.colorScheme.onSecondary,
            )
            if (favoritesMatch != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        favoritesMatch?.groupValues?.get(1)?.let { favorites ->
            Badge(
                text = formatFavorites(favorites.toLongOrNull() ?: 0),
                imageVector = Icons.Outlined.Favorite,
                color = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.onError,
                iconColor = MaterialTheme.colorScheme.onError,
            )
        }
    }
}

private fun formatFavorites(count: Long): String {
    return when {
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}
