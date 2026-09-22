package lk.jmcinnovators.learning

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DictionaryViewModel's network call itself isn't unit-testable without Robolectric or a fake
 * server (see TESTING.md); this locks down the JSON parsing shape it depends on, using a
 * trimmed real response from api.dictionaryapi.dev/api/v2/entries/en/test.
 */
class DictionaryViewModelTest {

    private val sampleResponse = """
        [
          {
            "word": "test",
            "phonetic": "/tɛst/",
            "phonetics": [ { "text": "/tɛst/" } ],
            "meanings": [
              {
                "partOfSpeech": "noun",
                "definitions": [
                  { "definition": "A procedure for critical evaluation." },
                  { "definition": "A means of testing." }
                ]
              },
              {
                "partOfSpeech": "verb",
                "definitions": [
                  { "definition": "To put to the proof." }
                ]
              }
            ]
          }
        ]
    """.trimIndent()

    @Test
    fun `parses word, phonetic, and every meaning`() {
        val entries = JSONArray(sampleResponse)
        assertEquals(1, entries.length())

        val entry = entries.getJSONObject(0)
        assertEquals("test", entry.getString("word"))
        assertEquals("/tɛst/", entry.getString("phonetic"))

        val meanings = entry.getJSONArray("meanings")
        assertEquals(2, meanings.length())
        assertEquals("noun", meanings.getJSONObject(0).getString("partOfSpeech"))

        val nounDefs = meanings.getJSONObject(0).getJSONArray("definitions")
        assertEquals(2, nounDefs.length())
        assertEquals("A procedure for critical evaluation.", nounDefs.getJSONObject(0).getString("definition"))
    }
}
