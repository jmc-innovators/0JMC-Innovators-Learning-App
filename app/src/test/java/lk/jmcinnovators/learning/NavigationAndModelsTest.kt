package lk.jmcinnovators.learning

import lk.jmcinnovators.learning.data.model.Assignment
import lk.jmcinnovators.learning.data.model.Note
import lk.jmcinnovators.learning.data.model.Quiz
import lk.jmcinnovators.learning.data.model.QuizQuestion
import lk.jmcinnovators.learning.data.model.SchoolClass
import lk.jmcinnovators.learning.data.model.UserProfile
import lk.jmcinnovators.learning.data.repository.AuthResult
import lk.jmcinnovators.learning.navigation.BOTTOM_NAV_ROUTES
import lk.jmcinnovators.learning.navigation.Routes
import lk.jmcinnovators.learning.viewmodel.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class NavigationAndModelsTest {

    @Test
    fun `verifies route definitions and parameterized paths`() {
        assertEquals("home", Routes.HOME)
        assertEquals("classroom", Routes.CLASSROOM)
        assertEquals("tools", Routes.TOOLS)
        assertEquals("notes", Routes.NOTES)
        assertEquals("profile", Routes.PROFILE)

        assertEquals("tools/maths", Routes.MATHS_LAB)
        assertEquals("tools/science", Routes.SCIENCE_WORLD)
        assertEquals("tools/dictionary", Routes.DICTIONARY)

        assertEquals("note_editor/note_123", Routes.noteEditor("note_123"))
        assertEquals("note_editor/new", Routes.noteEditor())
        assertEquals("classroom_detail/class_abc", Routes.classroomDetail("class_abc"))

        assertEquals(5, BOTTOM_NAV_ROUTES.size)
        assertTrue(BOTTOM_NAV_ROUTES.contains(Routes.HOME))
        assertTrue(BOTTOM_NAV_ROUTES.contains(Routes.CLASSROOM))
        assertTrue(BOTTOM_NAV_ROUTES.contains(Routes.TOOLS))
        assertTrue(BOTTOM_NAV_ROUTES.contains(Routes.NOTES))
        assertTrue(BOTTOM_NAV_ROUTES.contains(Routes.PROFILE))
    }

    @Test
    fun `verifies login ui state and auth result models`() {
        val idleState: LoginUiState = LoginUiState.Idle
        val loadingState: LoginUiState = LoginUiState.Loading
        val errorState: LoginUiState = LoginUiState.Error("No Google account found")
        val signedInWithoutProfile: LoginUiState = LoginUiState.SignedIn(hasProfile = false)
        val signedInWithProfile: LoginUiState = LoginUiState.SignedIn(hasProfile = true)

        assertTrue(idleState is LoginUiState.Idle)
        assertTrue(loadingState is LoginUiState.Loading)
        assertTrue(errorState is LoginUiState.Error)
        assertEquals("No Google account found", (errorState as LoginUiState.Error).message)
        assertFalse((signedInWithoutProfile as LoginUiState.SignedIn).hasProfile)
        assertTrue((signedInWithProfile as LoginUiState.SignedIn).hasProfile)

        val cancelledResult: AuthResult = AuthResult.Cancelled
        val errorResult: AuthResult = AuthResult.Error("Network error")
        assertTrue(cancelledResult is AuthResult.Cancelled)
        assertEquals("Network error", (errorResult as AuthResult.Error).message)
    }

    @Test
    fun `verifies user profile fields and role support`() {
        val supportedRoles = listOf("student", "teacher", "parent", "operator")

        val profile = UserProfile(
            uid = "firebase_auth_uid_123",
            fullName = "John Doe",
            displayName = "John Doe",
            email = "john@example.com",
            role = "student",
            school = "JMC International",
            grade = "Grade 11",
            country = "Sri Lanka",
            language = "en",
            photoUrl = "https://lh3.googleusercontent.com/a/photo.jpg",
            createdAtMillis = 1700000000000L,
            updatedAtMillis = 1700000000000L
        )

        assertEquals("firebase_auth_uid_123", profile.uid)
        assertEquals("John Doe", profile.fullName)
        assertEquals("John Doe", profile.displayName)
        assertEquals("john@example.com", profile.email)
        assertTrue(supportedRoles.contains(profile.role))
        assertEquals("Grade 11", profile.grade)
        assertFalse(profile.isTeacherOrAbove)

        val teacherProfile = profile.copy(role = "teacher", grade = "")
        assertTrue(teacherProfile.isTeacherOrAbove)

        val operatorProfile = profile.copy(role = "operator", grade = "")
        assertTrue(operatorProfile.isTeacherOrAbove)

        val parentProfile = profile.copy(role = "parent", grade = "")
        assertFalse(parentProfile.isTeacherOrAbove)
    }

    @Test
    fun `verifies note model properties and defaults`() {
        val note = Note(
            id = "test_note_id",
            ownerUid = "user_456",
            title = "Algebra Notes",
            body = "Quadratic formula: x = (-b +- sqrt(b^2 - 4ac)) / 2a",
            pinned = true
        )
        assertEquals("test_note_id", note.id)
        assertEquals("user_456", note.ownerUid)
        assertEquals("Algebra Notes", note.title)
        assertEquals("Quadratic formula: x = (-b +- sqrt(b^2 - 4ac)) / 2a", note.body)
        assertTrue(note.pinned)
    }

    @Test
    fun `verifies classroom and quiz structure`() {
        val schoolClass = SchoolClass(
            id = "cls_101",
            name = "Grade 11 Science",
            subject = "Physics",
            joinCode = "SCI11",
            studentCount = 28
        )
        val quiz = Quiz(
            id = "quiz_1",
            classId = "cls_101",
            title = "Physics Motion Quiz",
            timeLimitMinutes = 30,
            questionCount = 10
        )
        val question = QuizQuestion(
            id = "q1",
            prompt = "What is the unit of force?",
            options = listOf("Joule", "Newton", "Watt", "Pascal"),
            correctIndex = 1
        )
        val assignment = Assignment(
            id = "asg_1",
            classId = "cls_101",
            title = "Newton Laws Lab Report",
            instructions = "Submit 2-page report on force and mass experiment"
        )

        assertEquals("Grade 11 Science", schoolClass.name)
        assertEquals("SCI11", schoolClass.joinCode)
        assertEquals(28, schoolClass.studentCount)
        assertEquals("cls_101", quiz.classId)
        assertEquals(30, quiz.timeLimitMinutes)
        assertEquals(1, question.correctIndex)
        assertEquals("Newton", question.options[1])
        assertEquals("asg_1", assignment.id)
    }

    @Test
    fun `verifies geometry calculations`() {
        val radius = 5.0
        val area = PI * radius * radius
        val circ = 2 * PI * radius
        assertEquals(78.5398, area, 0.001)
        assertEquals(31.4159, circ, 0.001)

        val base = 6.0
        val height = 4.0
        val triangleArea = 0.5 * base * height
        assertEquals(12.0, triangleArea, 0.001)
    }
}
