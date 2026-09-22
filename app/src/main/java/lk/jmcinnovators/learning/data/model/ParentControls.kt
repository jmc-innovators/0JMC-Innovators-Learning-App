package lk.jmcinnovators.learning.data.model

/**
 * parentControls/{studentUid} — field names and defaults copied verbatim from
 * legacy-web/js/parent-controls-enforcer.js so the app enforces the same rules a parent
 * already set from the website.
 */
data class ParentControls(
    val linked: Boolean = false,
    val studyModeEnabled: Boolean = false,
    val aiEnabled: Boolean = true,
    val downloadsEnabled: Boolean = true,
    val chatEnabled: Boolean = true,
    val videosEnabled: Boolean = true,
    val allowedSubjects: List<String> = listOf("all"),
    val dailyLimitMinutes: Int = 0,
    val weeklyLimitMinutes: Int = 0,
    val monthlyReportEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val emergencyLock: Boolean = false
) {
    fun isFeatureEnabled(feature: String): Boolean {
        if (emergencyLock) return false
        return when (feature) {
            "ai" -> aiEnabled
            "downloads" -> downloadsEnabled
            "chat" -> chatEnabled
            "videos" -> videosEnabled
            else -> true
        }
    }
}
