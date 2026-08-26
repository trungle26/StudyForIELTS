package com.trungld.studyforielts.presentation.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trungld.studyforielts.R
import com.trungld.studyforielts.domain.model.IeltsSkillType

private data class SkillHubItem(
    val skill: IeltsSkillType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val SKILL_HUB_ITEMS = listOf(
    SkillHubItem(
        skill = IeltsSkillType.LISTENING,
        title = "Listening",
        subtitle = "Maps, MCQs & Notes",
        icon = Icons.Default.Headphones,
    ),
    SkillHubItem(
        skill = IeltsSkillType.READING,
        title = "Reading",
        subtitle = "TFNG & Headings",
        icon = Icons.Default.MenuBook,
    ),
    SkillHubItem(
        skill = IeltsSkillType.WRITING_TASK1,
        title = "Writing Task 1",
        subtitle = "Charts & Overview",
        icon = Icons.Default.Create,
    ),
    SkillHubItem(
        skill = IeltsSkillType.WRITING_TASK2,
        title = "Writing Task 2",
        subtitle = "Essays & Structure",
        icon = Icons.Default.School,
    ),
    SkillHubItem(
        skill = IeltsSkillType.SPEAKING,
        title = "Speaking",
        subtitle = "Part 2 & Discussion",
        icon = Icons.Default.Mic,
    ),
)

@Composable
fun StrategySkillHub(
    onSkillClick: (IeltsSkillType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.strategy_hub_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.strategy_hub_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(SKILL_HUB_ITEMS) { item ->
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onSkillClick(item.skill) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
