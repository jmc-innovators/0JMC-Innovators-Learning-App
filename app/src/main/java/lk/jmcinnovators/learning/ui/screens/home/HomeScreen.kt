package lk.jmcinnovators.learning.ui.screens.home

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import lk.jmcinnovators.learning.ui.components.EmptyState
import lk.jmcinnovators.learning.ui.components.FullScreenLoading
import lk.jmcinnovators.learning.viewmodel.HomeViewModel
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

private data class QuickAction(val emoji: String, val label: String, val route: String)

private val quickActions = listOf(
    QuickAction("\uD83D\uDCDA", "Classroom", "classroom"),
    QuickAction("\uD83D\uDCDD", "Notes", "notes"),
    QuickAction("\uD83D\uDD0E", "Dictionary", "tools/dictionary"),
    QuickAction("\u2795", "Maths Lab", "tools/maths"),
    QuickAction("\uD83D\uDD2C", "Science", "tools/science")
)

@Composable
fun HomeScreen(
    factory: ViewModelFactory,
    onOpenNotifications: () -> Unit,
    onOpenAction: (String) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.loading) {
        FullScreenLoading("Loading your dashboard…")
        return
    }

    val profile = uiState.profile
    val firstName = profile?.fullName?.trim()?.substringBefore(" ").takeUnless { it.isNullOrBlank() } ?: "there"

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good morning, $firstName \uD83D\uDC4B", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Ready to learn?", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                    if (!profile?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.photoUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(quickActions) { action ->
                    QuickActionChip(action) { onOpenAction(action.route) }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            Text("Today's Learning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            // No content source is wired to Firestore for this section yet (see MIGRATION.md);
            // an honest empty state beats inventing lessons that don't exist.
            EmptyState("Nothing here yet — join a classroom to see today's work.")
            Spacer(Modifier.height(28.dp))
        }

        item {
            Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            EmptyState("Your stats will show up here once you complete lessons or quizzes.")
        }
    }
}

@Composable
private fun QuickActionChip(action: QuickAction, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(action.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(action.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
