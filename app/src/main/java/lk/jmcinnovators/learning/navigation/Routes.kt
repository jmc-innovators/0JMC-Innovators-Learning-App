package lk.jmcinnovators.learning.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val PROFILE_SETUP = "profile_setup"

    // Bottom-nav top-level destinations
    const val HOME = "home"
    const val CLASSROOM = "classroom"
    const val TOOLS = "tools"
    const val NOTES = "notes"
    const val PROFILE = "profile"

    // Secondary destinations
    const val NOTIFICATIONS = "notifications"
    const val SEARCH = "search"
    const val NOTE_EDITOR = "note_editor"
    const val NOTE_EDITOR_ARG = "noteId"
    fun noteEditor(noteId: String = "new") = "note_editor/$noteId"
    const val NOTE_EDITOR_ROUTE = "note_editor/{$NOTE_EDITOR_ARG}"

    const val CLASSROOM_DETAIL = "classroom_detail"
    const val CLASSROOM_ID_ARG = "classId"
    fun classroomDetail(classId: String) = "classroom_detail/$classId"
    const val CLASSROOM_DETAIL_ROUTE = "classroom_detail/{$CLASSROOM_ID_ARG}"

    const val DICTIONARY = "tools/dictionary"
    const val MATHS_LAB = "tools/maths"
    const val SCIENCE_WORLD = "tools/science"
}

val BOTTOM_NAV_ROUTES = listOf(Routes.HOME, Routes.CLASSROOM, Routes.TOOLS, Routes.NOTES, Routes.PROFILE)
