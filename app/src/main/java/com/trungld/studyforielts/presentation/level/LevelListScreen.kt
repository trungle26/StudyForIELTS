package com.trungld.studyforielts.presentation.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.stringResource
import com.trungld.studyforielts.R
import com.trungld.studyforielts.ui.theme.Dimens

@Composable
fun LevelListScreen(
    onLevelClick: (String) -> Unit,
    onOnlineYoutubeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.TopCenter,
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Dimens.ContentMaxWidth)
                .padding(horizontal = Dimens.ContentPadding, vertical = Dimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs + 2.dp)) {
                    Text(
                        text = stringResource(R.string.level_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.level_list_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOnlineYoutubeClick),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs + 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.level_list_online_youtube_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.level_list_online_youtube_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            items(STUDY_LEVELS) { level ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLevelClick(level) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.SurfaceAlpha),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs + 2.dp),
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(levelDescriptionRes(level)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        }
    }
}

@StringRes
private fun levelDescriptionRes(level: String): Int {
    return when (level) {
        "A1" -> R.string.level_description_a1
        "A2" -> R.string.level_description_a2
        "B1" -> R.string.level_description_b1
        "B2" -> R.string.level_description_b2
        "C1" -> R.string.level_description_c1
        else -> R.string.level_description_c2
    }
}

private val STUDY_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")
