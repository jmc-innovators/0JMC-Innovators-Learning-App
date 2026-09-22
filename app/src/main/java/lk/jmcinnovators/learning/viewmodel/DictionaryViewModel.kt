package lk.jmcinnovators.learning.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class DictionaryMeaning(val partOfSpeech: String, val definitions: List<String>)
data class DictionaryEntry(val word: String, val phonetic: String, val meanings: List<DictionaryMeaning>)

sealed class DictionaryUiState {
    object Idle : DictionaryUiState()
    object Loading : DictionaryUiState()
    data class Result(val entries: List<DictionaryEntry>) : DictionaryUiState()
    data class Error(val message: String) : DictionaryUiState()
}

/** Same public API legacy-web/dictionary_.html calls: api.dictionaryapi.dev (no key required). */
class DictionaryViewModel : ViewModel() {

    var query by mutableStateOf("")
        private set
    var uiState: DictionaryUiState by mutableStateOf(DictionaryUiState.Idle)
        private set

    fun onQueryChange(value: String) {
        query = value
    }

    fun search() {
        val word = query.trim()
        if (word.isEmpty()) return
        uiState = DictionaryUiState.Loading
        viewModelScope.launch {
            uiState = try {
                val entries = withContext(Dispatchers.IO) { fetchDefinitions(word) }
                if (entries.isEmpty()) DictionaryUiState.Error("No definitions found for \"$word\".")
                else DictionaryUiState.Result(entries)
            } catch (e: Exception) {
                DictionaryUiState.Error("Couldn't reach the dictionary service. Check your connection and try again.")
            }
        }
    }

    private fun fetchDefinitions(word: String): List<DictionaryEntry> {
        val encoded = URLEncoder.encode(word, "UTF-8")
        val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        if (connection.responseCode != 200) {
            connection.disconnect()
            return emptyList()
        }
        val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        connection.disconnect()

        val entriesJson = JSONArray(body)
        val results = mutableListOf<DictionaryEntry>()
        for (i in 0 until entriesJson.length()) {
            val entry = entriesJson.getJSONObject(i)
            val phoneticsArr = entry.optJSONArray("phonetics")
            val phonetic = entry.optString("phonetic").ifBlank {
                (0 until (phoneticsArr?.length() ?: 0))
                    .map { phoneticsArr!!.getJSONObject(it).optString("text") }
                    .firstOrNull { it.isNotBlank() } ?: ""
            }
            val meaningsJson = entry.optJSONArray("meanings")
            val meanings = mutableListOf<DictionaryMeaning>()
            for (m in 0 until (meaningsJson?.length() ?: 0)) {
                val meaning = meaningsJson!!.getJSONObject(m)
                val defsJson = meaning.optJSONArray("definitions")
                val defs = (0 until (defsJson?.length() ?: 0)).map {
                    defsJson!!.getJSONObject(it).optString("definition")
                }
                meanings.add(DictionaryMeaning(meaning.optString("partOfSpeech"), defs))
            }
            results.add(DictionaryEntry(entry.optString("word", word), phonetic, meanings))
        }
        return results
    }
}
