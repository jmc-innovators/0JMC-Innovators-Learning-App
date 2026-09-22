package lk.jmcinnovators.learning

import lk.jmcinnovators.learning.data.model.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun `teacher is teacherOrAbove`() {
        assertTrue(UserProfile(role = "teacher").isTeacherOrAbove)
    }

    @Test
    fun `operator is teacherOrAbove`() {
        assertTrue(UserProfile(role = "operator").isTeacherOrAbove)
    }

    @Test
    fun `student is not teacherOrAbove`() {
        assertFalse(UserProfile(role = "student").isTeacherOrAbove)
    }

    @Test
    fun `parent is not teacherOrAbove`() {
        assertFalse(UserProfile(role = "parent").isTeacherOrAbove)
    }
}
