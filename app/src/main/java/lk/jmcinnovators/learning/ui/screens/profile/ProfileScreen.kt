package lk.jmcinnovators.learning.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import lk.jmcinnovators.learning.ui.components.FullScreenLoading
import lk.jmcinnovators.learning.viewmodel.ProfileViewModel
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

private val themeOptions = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

@Composable
fun ProfileScreen(
    factory: ViewModelFactory,
    onSignedOut: () -> Unit
) {
    val viewModel: ProfileViewModel = viewModel(factory = factory)
    val profile by viewModel.profile.collectAsState()
    val themePref by viewModel.themePreference.collectAsState()

    if (profile == null) {
        FullScreenLoading("Loading profile…")
        return
    }
    val p = profile!!

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = p.photoUrl,
                        contentDescription = "Profile photo",
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Column {
                    Text(p.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(p.email, style = MaterialTheme.typography.bodyMedium)
                    Text(p.role.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("School", p.school)
                    if (p.grade.isNotBlank()) InfoRow("Grade", p.grade)
                    InfoRow("Country", p.country)
                    InfoRow(
                        "Language",
                        when (p.language) { "si" -> "Sinhala"; "ta" -> "Tamil"; else -> "English" }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = themePref == value,
                        onClick = { viewModel.setThemePreference(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size)
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = { viewModel.signOut(); onSignedOut() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign out", color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "\u2014" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
