package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shape.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tachiyomi.domain.manga.model.Manga

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        IconBadge(
            imageVector = Icons.Outlined.CollectionsBookmark,
        )
    }
}

@Composable
internal fun ReadLaterBadge(enabled: Boolean) {
    if (enabled) {
        IconBadge(
            imageVector = Icons.Outlined.Schedule,
            color = MaterialTheme.colorScheme.tertiary,
            iconColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
private fun IconBadge(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    iconColor: Color = MaterialTheme.colorScheme.onSecondary,
) {
    Row(
        modifier = modifier
            .clip(RectangleShape)
            .background(color)
            .padding(horizontal = 3.dp, vertical = 1.dp),
    ) {
        Icon(
            imageVector = imageVector,
            tint = iconColor,
            contentDescription = null,
        )
    }
}

@Composable
internal fun NHentaiMetadataBadges(manga: Manga) {
    val description = manga.description

    if (description == null) return

    // 从 description 解析页数和收藏数
    val pageCountMatch = Regex("""Pages:\s*(\d+)""").find(description)
    val favoritesMatch = Regex("""Favorited by:\s*(\d+)""").find(description)

    if (pageCountMatch == null && favoritesMatch == null) return

    Row {
        pageCountMatch?.groupValues?.get(1)?.let { pageCount ->
            TextBadge(
                text = pageCount,
                icon = Icons.Outlined.FilterNone,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (favoritesMatch != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        favoritesMatch?.groupValues?.get(1)?.let { favorites ->
            TextBadge(
                text = formatFavorites(favorites.toLongOrNull() ?: 0),
                icon = Icons.Outlined.Favorite,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TextBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RectangleShape)
            .background(color)
            .padding(horizontal = 3.dp, vertical = 1.dp),
    ) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colorScheme.onSecondary,
            contentDescription = null,
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatFavorites(count: Long): String {
    return when {
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}
