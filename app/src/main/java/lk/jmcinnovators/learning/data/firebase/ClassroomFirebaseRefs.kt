package lk.jmcinnovators.learning.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import lk.jmcinnovators.learning.R
import org.json.JSONObject

/**
 * jmc-class is a second, separate Firebase project (legacy-web/jmc_Classroom.html). Its
 * google-services file (res/raw/jmc_class_services.json) is parsed by hand and used to spin
 * up a *named* secondary FirebaseApp, so classroom data never mixes with the jmc-home2 default
 * app used everywhere else. See MIGRATION.md for why these two projects are still separate.
 */
private const val SECONDARY_APP_NAME = "jmc-class"

object ClassroomFirebaseRefs {

    private var initialized = false

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        val existing = FirebaseApp.getApps(context).firstOrNull { it.name == SECONDARY_APP_NAME }
        if (existing == null) {
            val json = JSONObject(
                context.resources.openRawResource(R.raw.jmc_class_services)
                    .bufferedReader().use { it.readText() }
            )
            val client = json.getJSONArray("client").getJSONObject(0)
            val projectInfo = json.getJSONObject("project_info")
            val options = FirebaseOptions.Builder()
                .setApplicationId(client.getJSONObject("client_info").getString("mobilesdk_app_id"))
                .setApiKey(client.getJSONArray("api_key").getJSONObject(0).getString("current_key"))
                .setProjectId(projectInfo.getString("project_id"))
                .setStorageBucket(projectInfo.getString("storage_bucket"))
                .build()
            FirebaseApp.initializeApp(context, options, SECONDARY_APP_NAME)
        }
        initialized = true
    }

    fun app(context: Context) = run {
        ensureInitialized(context)
        FirebaseApp.getInstance(SECONDARY_APP_NAME)
    }

    fun auth(context: Context) = Firebase.auth(app(context))
    fun firestore(context: Context) = Firebase.firestore(app(context))
}
