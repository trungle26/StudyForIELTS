package com.trungld.studyforielts.presentation.writing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.trungld.studyforielts.R
import com.trungld.studyforielts.ui.theme.Dimens

/**
 * Writing section landing page.
 *
 * Three cards: Task 1 (Academic) -> lesson list, Task 2 (Essay) -> lesson list,
 * Free Practice (Task 2 without a lesson) -> the original
 * [WritingPracticeScreen] with no `lessonId` argument.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingHomeScreen(
    onTask1Click: () -> Unit,
    onTask2Click: () -> Unit,
    onFreePracticeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.writing_home_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
        ) {
            Text(
                text = stringResource(R.string.writing_home_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            WritingModeCard(
                title = stringResource(R.string.writing_home_task1_title),
                subtitle = stringResource(R.string.writing_home_task1_subtitle),
                icon = Icons.Default.BarChart,
                onClick = onTask1Click,
            )

            WritingModeCard(
                title = stringResource(R.string.writing_home_task2_title),
                subtitle = stringResource(R.string.writing_home_task2_subtitle),
                icon = Icons.Default.Edit,
                onClick = onTask2Click,
            )

            WritingModeCard(
                title = stringResource(R.string.writing_home_free_title),
                subtitle = stringResource(R.string.writing_home_free_subtitle),
                icon = Icons.Default.EditNote,
                onClick = onFreePracticeClick,
            )
        }
    }
}

@Composable
private fun WritingModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Dimens.ContentPadding)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(androidx.compose.ui.Alignment.End),
            )
        }
    }
}
