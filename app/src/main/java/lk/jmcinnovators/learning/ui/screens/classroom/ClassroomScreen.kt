package lk.jmcinnovators.learning.ui.screens.classroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.jmcinnovators.learning.data.model.SchoolClass
import lk.jmcinnovators.learning.ui.components.EmptyState
import lk.jmcinnovators.learning.viewmodel.ClassroomViewModel
import lk.jmcinnovators.learning.viewmodel.JoinCodeUiState
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

/** Native replacement for legacy-web/jmc_Classroom.html's join flow and class list. */
@Composable
fun ClassroomScreen(
    factory: ViewModelFactory,
    onOpenClass: (String) -> Unit
) {
    val viewModel: ClassroomViewModel = viewModel(factory = factory)
    val myClasses by viewModel.myClasses.collectAsState()
    val joinState by viewModel.joinState.collectAsState()
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showJoinDialog = true }) {
                Icon(Icons.Filled.School, contentDescription = "Join classroom")
            }
        }
    ) { padding ->
        if (myClasses.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState("No classrooms yet. Tap the button below to join one with a code.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().padding(padding),
                contentPadding = PaddingValues(20.dp)
            ) {
                item {
                    Text("Your Classrooms", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                }
                items(myClasses) { schoolClass ->
                    ClassroomCard(schoolClass) { onOpenClass(schoolClass.id) }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinClassDialog(
            joinState = joinState,
            onDismiss = { showJoinDialog = false; viewModel.resetJoinState() },
            onJoin = viewModel::joinClass
        )
        LaunchedEffect(joinState) {
            if (joinState is JoinCodeUiState.Joined) {
                showJoinDialog = false
                viewModel.resetJoinState()
            }
        }
    }
}

@Composable
private fun ClassroomCard(schoolClass: SchoolClass, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(schoolClass.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(schoolClass.subject, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("${schoolClass.studentCount} students", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun JoinClassDialog(
    joinState: JoinCodeUiState,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join a classroom") },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Classroom code") },
                    placeholder = { Text("ABC123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                when (joinState) {
                    is JoinCodeUiState.Joining -> Row {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(" Joining…")
                    }
                    JoinCodeUiState.NotFound -> Text(
                        "Classroom code not found.", color = MaterialTheme.colorScheme.error
                    )
                    JoinCodeUiState.AlreadyMember -> Text(
                        "You're already in this classroom.", color = MaterialTheme.colorScheme.error
                    )
                    is JoinCodeUiState.Error -> Text(joinState.message, color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(onClick = { onJoin(code) }, enabled = code.isNotBlank() && joinState !is JoinCodeUiState.Joining) {
                Text("Join")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
