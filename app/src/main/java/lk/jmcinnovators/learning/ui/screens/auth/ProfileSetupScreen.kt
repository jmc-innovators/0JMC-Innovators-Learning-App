package lk.jmcinnovators.learning.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.google.firebase.auth.FirebaseAuth
import lk.jmcinnovators.learning.data.model.UserProfile
import lk.jmcinnovators.learning.viewmodel.ProfileSetupUiState
import lk.jmcinnovators.learning.viewmodel.ProfileSetupViewModel
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

private data class RoleOption(val id: String, val emoji: String, val label: String, val desc: String)

private val roleOptions = listOf(
    RoleOption("student", "\uD83C\uDF93", "Student", "Courses, AI tutor, quizzes"),
    RoleOption("teacher", "\uD83E\uDDD1\u200D\uD83C\uDFEB", "Teacher", "Classes, grading, announcements"),
    RoleOption("parent", "\uD83D\uDC6A", "Parent", "Monitor & guide"),
    RoleOption("operator", "\uD83C\uDFEB", "School Operator", "Manage a school")
)

private val grades = listOf("Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "Grade 13")
private val languages = listOf("en" to "English", "si" to "සිංහල (Sinhala)", "ta" to "தமிழ் (Tamil)")

/** Mirrors legacy-web/profile.html's role/grade/language form so the same users/{uid} shape results. */
@Composable
fun ProfileSetupScreen(
    factory: ViewModelFactory,
    onSaved: () -> Unit
) {
    val viewModel: ProfileSetupViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    var role by remember { mutableStateOf("student") }
    var fullName by remember { mutableStateOf(firebaseUser?.displayName ?: "") }
    var school by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf(grades.first()) }
    var country by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    var gradeMenuExpanded by remember { mutableStateOf(false) }
    var langMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is ProfileSetupUiState.Saved) onSaved()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Text("Set up your profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("This decides which dashboard you land on.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Text("I am a…", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        item {
            LazyRow {
                items(roleOptions) { option ->
                    RoleCard(option, selected = role == option.id, onClick = { role = option.id })
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            OutlinedTextField(
                value = fullName, onValueChange = { fullName = it },
                label = { Text("Full name") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = school, onValueChange = { school = it },
                label = { Text("School") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = country, onValueChange = { country = it },
                label = { Text("Country") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            if (role == "student") {
                ExposedDropdownMenuBox(expanded = gradeMenuExpanded, onExpandedChange = { gradeMenuExpanded = it }) {
                    OutlinedTextField(
                        value = grade, onValueChange = {}, readOnly = true,
                        label = { Text("Grade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                            androidx.compose.material3.ExposedDropdownMenu(
                        expanded = gradeMenuExpanded, onDismissRequest = { gradeMenuExpanded = false }
                    ) {
                        grades.forEach { g ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(g) },
                                onClick = { grade = g; gradeMenuExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            ExposedDropdownMenuBox(expanded = langMenuExpanded, onExpandedChange = { langMenuExpanded = it }) {
                OutlinedTextField(
                    value = languages.first { it.first == language }.second, onValueChange = {}, readOnly = true,
                    label = { Text("Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                androidx.compose.material3.ExposedDropdownMenu(
                    expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }
                ) {
                    languages.forEach { (code, label) ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { language = code; langMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (uiState is ProfileSetupUiState.Error) {
                Text(
                    (uiState as ProfileSetupUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val uid = firebaseUser?.uid ?: return@Button
                    viewModel.submit(
                        UserProfile(
                            uid = uid,
                            fullName = fullName,
                            email = firebaseUser.email ?: "",
                            role = role,
                            school = school,
                            grade = if (role == "student") grade else "",
                            country = country,
                            language = language,
                            photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                },
                enabled = fullName.isNotBlank() && school.isNotBlank() && uiState !is ProfileSetupUiState.Saving,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState is ProfileSetupUiState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun RoleCard(option: RoleOption, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(end = 10.dp).height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(option.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(option.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(option.desc, style = MaterialTheme.typography.labelLarge, maxLines = 2)
        }
    }
}

