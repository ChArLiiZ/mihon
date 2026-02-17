package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
    iconColor: Color = MaterialTheme.colorScheme.secondary,
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

/**
 * Unified metadata badges that parse description for page count, favorites, and rating.
 * Supports formats from both NHentai and EHentai extensions.
 */
@Composable
internal fun MetadataBadges(manga: Manga) {
    val description = manga.description ?: return

    var pageCount: String? = null
    var favorites: String? = null
    var rating: String? = null

    for (line in description.split("\n")) {
        if (pageCount == null) {
            // "Pages: 25" or "Length: 25 pages"
            PAGE_COUNT_REGEX.find(line)?.groupValues?.get(1)?.let { pageCount = it }
        }
        if (favorites == null) {
            // "Favorited by: 1234" or "Favorited: 1234 times"
            FAVORITES_REGEX.find(line)?.groupValues?.get(1)?.let { favorites = it }
        }
        if (rating == null) {
            // "Rating: 4.5" or "Rating: 4.50 (123)"
            RATING_REGEX.find(line)?.groupValues?.get(1)?.let { rating = it }
        }
    }

    if (pageCount == null && favorites == null && rating == null) return

    Row {
        pageCount?.let { count ->
            TextBadge(
                text = count,
                icon = Icons.Outlined.FilterNone,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        favorites?.let { favs ->
            Spacer(modifier = Modifier.width(4.dp))
            TextBadge(
                text = formatCount(favs.toLongOrNull() ?: 0),
                icon = Icons.Outlined.Favorite,
                color = MaterialTheme.colorScheme.error,
            )
        }

        rating?.let { rate ->
            Spacer(modifier = Modifier.width(4.dp))
            TextBadge(
                text = rate,
                icon = Icons.Outlined.Star,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

// Keep old name as alias for compatibility
@Composable
internal fun NHentaiMetadataBadges(manga: Manga) = MetadataBadges(manga)

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
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colorScheme.onSecondary,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000.0)}M"
        count >= 1000 -> "${"%.1f".format(count / 1000.0)}K"
        else -> count.toString()
    }
}

// Regex patterns for parsing metadata from description
// Matches "Pages: 25" and "Length: 25 pages"
private val PAGE_COUNT_REGEX = Regex("""(?:Pages|Length):\s*(\d+)""")

// Matches "Favorited by: 1234" and "Favorited: 1234 times"
private val FAVORITES_REGEX = Regex("""Favorited(?:\s*by)?:\s*(\d+)""")

// Matches "Rating: 4.5" and "Rating: 4.50 (123)"
private val RATING_REGEX = Regex("""Rating:\s*([\d.]+)""")
