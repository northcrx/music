package com.nishant.northmusic.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.ui.components.AppDialog
import java.net.URLEncoder

@Composable
fun WebSearchDialog(
    song: Song,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val query = "${song.artist} ${song.title}".trim()
    val encodedQuery = try {
        URLEncoder.encode(query, "UTF-8")
    } catch (_: Exception) {
        query.replace(" ", "+")
    }

    val searchProviders = listOf(
        "Google" to "https://www.google.com/search?q=$encodedQuery",
        "YouTube" to "https://www.youtube.com/results?search_query=$encodedQuery",
        "Last.fm" to "https://www.last.fm/search?q=$encodedQuery",
        "Wikipedia" to "https://en.wikipedia.org/wiki/Special:Search?search=${try { URLEncoder.encode(song.artist, "UTF-8") } catch (_: Exception) { song.artist }}"
    )

    AppDialog(
        onDismissRequest = onDismiss,
        title = "Search the web",
        subtitle = "Search for \"$query\" on your preferred provider",
        icon = Icons.Rounded.Search,
        dismissButtonText = "Cancel",
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searchProviders.forEach { (name, url) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
