package lk.jmcinnovators.learning.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Mirrors users/{uid} exactly as written by legacy-web/profile.html so the native app
 * and the existing website read and write the same document shape.
 */
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "student", // student | teacher | parent | operator
    val school: String = "",
    val grade: String = "",
    val country: String = "",
    val language: String = "en", // en | si | ta
    val photoUrl: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAtMillis: Long = 0L,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAtMillis: Long = 0L
) {
    val isTeacherOrAbove: Boolean get() = role == "teacher" || role == "operator"
}

data class ThemeSettings(
    val theme: String = "dark" // light | dark | system, matches legacy-web/js/theme.js
)

data class NotificationPrefs(
    val assignment: Boolean = true,
    val result: Boolean = true,
    val announcement: Boolean = true,
    val ai: Boolean = true,
    val security: Boolean = true,
    val parent: Boolean = true,
    val system: Boolean = true
)

data class UserSettings(
    val notif: NotificationPrefs = NotificationPrefs(),
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true
)
