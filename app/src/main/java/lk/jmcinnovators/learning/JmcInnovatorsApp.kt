package lk.jmcinnovators.learning

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class JmcInnovatorsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)

            // Offline persistence: cached reads/writes survive a dropped connection
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) {
            android.util.Log.e("JmcInnovatorsApp", "Firebase initialization failed: ${e.message}", e)
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                getString(R.string.channel_general_id),
                getString(R.string.channel_general_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                getString(R.string.channel_classroom_id),
                getString(R.string.channel_classroom_name),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }
}
