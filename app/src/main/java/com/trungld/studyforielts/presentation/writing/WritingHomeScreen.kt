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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
        topBar = { TopAppBar(title = { Text("Writing") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Chọn chế độ luyện viết",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            WritingModeCard(
                title = "Task 1 (Academic)",
                subtitle = "Mô tả biểu đồ, đồ thị hoặc quy trình bằng tiếng Anh học thuật.",
                icon = Icons.Default.BarChart,
                onClick = onTask1Click,
            )

            WritingModeCard(
                title = "Task 2 (Essay)",
                subtitle = "Bài luận ngắn về một chủ đề xã hội, học thuật hoặc công việc.",
                icon = Icons.Default.Edit,
                onClick = onTask2Click,
            )

            WritingModeCard(
                title = "Free Practice",
                subtitle = "Luyện Task 2 tự do không cần chọn bài học — dùng prompt mặc định.",
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
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(androidx.compose.ui.Alignment.End),
            )
        }
    }
}
