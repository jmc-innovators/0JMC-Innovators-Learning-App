package lk.jmcinnovators.learning.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.jmcinnovators.learning.data.model.AppNotification
import lk.jmcinnovators.learning.ui.components.EmptyState
import lk.jmcinnovators.learning.ui.theme.JmcPalette
import lk.jmcinnovators.learning.viewmodel.NotificationsViewModel
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

private val categoryColors = mapOf(
    "assignment" to JmcPalette.Blue, "result" to JmcPalette.Green, "announcement" to JmcPalette.Gold,
    "ai" to JmcPalette.Violet, "security" to JmcPalette.Pink, "parent" to JmcPalette.Teal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(factory: ViewModelFactory, onBack: () -> Unit) {
    val viewModel: NotificationsViewModel = viewModel(factory = factory)
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState("You're all caught up — no notifications yet.")
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().padding(padding), contentPadding = PaddingValues(20.dp)) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationRow(
                        notification,
                        onClick = { if (!notification.read) viewModel.markRead(notification.id) },
                        onDelete = { viewModel.remove(notification.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(categoryColors[notification.category] ?: MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.height(0.dp))
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(notification.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (notification.body.isNotBlank()) {
                    Text(notification.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete notification")
            }
        }
    }
}
