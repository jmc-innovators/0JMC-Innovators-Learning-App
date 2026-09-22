package lk.jmcinnovators.learning.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * The jmc-class Firebase project only persists schools / supervisors / teachers / invites
 * today — classes, assignments and quizzes live in the web app's localStorage
 * (see MIGRATION.md). These models define the target Firestore shape the app writes to,
 * so the native app is itself the migration off localStorage rather than a copy of it.
 */
data class SchoolClass(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val name: String = "",
    val subject: String = "",
    val teacherUid: String = "",
    val joinCode: String = "",
    val studentCount: Int = 0,
    @ServerTimestamp val createdAt: Date? = null
)

data class Assignment(
    @DocumentId val id: String = "",
    val classId: String = "",
    val title: String = "",
    val instructions: String = "",
    val dueAt: Date? = null,
    val attachmentUrls: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null
)

data class Submission(
    @DocumentId val id: String = "",
    val assignmentId: String = "",
    val studentUid: String = "",
    val fileUrl: String = "",
    val note: String = "",
    val status: String = "submitted", // submitted | graded | late
    val grade: String = "",
    val feedback: String = "",
    @ServerTimestamp val submittedAt: Date? = null
)

data class Quiz(
    @DocumentId val id: String = "",
    val classId: String = "",
    val title: String = "",
    val timeLimitMinutes: Int = 0,
    val questionCount: Int = 0,
    @ServerTimestamp val createdAt: Date? = null
)

data class QuizQuestion(
    @DocumentId val id: String = "",
    val prompt: String = "",
    val type: String = "mcq", // mcq | true_false
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0 // read only by Cloud Functions during grading, never sent to students
)

data class QuizAttempt(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val classId: String = "",
    val studentUid: String = "",
    val answers: Map<String, Int> = emptyMap(),
    val score: Int = 0,
    val percentage: Double = 0.0,
    val durationSeconds: Int = 0,
    @ServerTimestamp val submittedAt: Date? = null
)

data class Announcement(
    @DocumentId val id: String = "",
    val classId: String = "",
    val authorUid: String = "",
    val title: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Date? = null
)
