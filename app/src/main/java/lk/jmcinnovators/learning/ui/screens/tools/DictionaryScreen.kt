package lk.jmcinnovators.learning.ui.screens.tools

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.jmcinnovators.learning.ui.components.EmptyState
import lk.jmcinnovators.learning.ui.components.ErrorState
import lk.jmcinnovators.learning.ui.components.FullScreenLoading
import lk.jmcinnovators.learning.viewmodel.DictionaryEntry
import lk.jmcinnovators.learning.viewmodel.DictionaryUiState
import lk.jmcinnovators.learning.viewmodel.DictionaryViewModel

/** Native port of legacy-web/dictionary_.html, calling the same dictionaryapi.dev endpoint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onBack: () -> Unit) {
    val viewModel: DictionaryViewModel = viewModel()
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dictionary") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxWidth().padding(padding).padding(20.dp)) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search a word…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search, keyboardType = KeyboardType.Text
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { viewModel.search() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            when (val state = uiState) {
                DictionaryUiState.Idle -> EmptyState("Search a word to see its definitions.")
                DictionaryUiState.Loading -> FullScreenLoading("Looking up \"${viewModel.query}\"…")
                is DictionaryUiState.Error -> ErrorState(state.message, onRetry = viewModel::search)
                is DictionaryUiState.Result -> DictionaryResults(state.entries)
            }
        }
    }
}

@Composable
private fun DictionaryResults(entries: List<DictionaryEntry>) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        items(entries) { entry ->
            Text(entry.word, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (entry.phonetic.isNotBlank()) {
                Text(entry.phonetic, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }
            Spacer(Modifier.height(8.dp))
            entry.meanings.forEach { meaning ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(meaning.partOfSpeech, style = MaterialTheme.typography.labelLarge, fontStyle = FontStyle.Italic)
                        Spacer(Modifier.height(4.dp))
                        meaning.definitions.take(4).forEachIndexed { i, def ->
                            Text("${i + 1}. $def", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
