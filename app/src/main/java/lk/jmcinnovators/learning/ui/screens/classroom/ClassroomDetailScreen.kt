package lk.jmcinnovators.learning.ui.screens.classroom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.jmcinnovators.learning.data.model.Announcement
import lk.jmcinnovators.learning.data.model.Assignment
import lk.jmcinnovators.learning.ui.components.EmptyState
import lk.jmcinnovators.learning.viewmodel.ClassroomDetailViewModel
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomDetailScreen(
    classId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: ClassroomDetailViewModel = viewModel(
        factory = factory, key = classId
    )
    viewModel.setClassId(classId)
    val assignments by viewModel.assignments.collectAsState()
    val announcements by viewModel.announcements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classroom") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxWidth().padding(padding), contentPadding = PaddingValues(20.dp)) {
            item {
                Text("Announcements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            if (announcements.isEmpty()) {
                item { EmptyState("No announcements yet.") }
            } else {
                items(announcements) { announcement -> AnnouncementCard(announcement); Spacer(Modifier.height(8.dp)) }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text("Assignments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            if (assignments.isEmpty()) {
                item { EmptyState("No assignments posted yet.") }
            } else {
                items(assignments) { assignment -> AssignmentCard(assignment); Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp)) {
            Text(announcement.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(announcement.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AssignmentCard(assignment: Assignment) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp)) {
            Text(assignment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(assignment.instructions, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
    }
}
