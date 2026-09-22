package lk.jmcinnovators.learning.ui.screens.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import lk.jmcinnovators.learning.ui.components.EmptyState

/**
 * legacy-web's Maths Lab / Science World are interactive JS-only pages with no data model to
 * port (see MIGRATION.md). Rather than fake a working tool, this screen honestly says so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingToolScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
            EmptyState(
                "$title is still being rebuilt as a native screen. It's listed as pending in MIGRATION.md.",
                icon = Icons.Filled.Construction
            )
        }
    }
}
