package lk.jmcinnovators.learning

import lk.jmcinnovators.learning.data.model.ParentControls
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentControlsTest {

    @Test
    fun `emergencyLock overrides every feature flag`() {
        val controls = ParentControls(
            emergencyLock = true,
            aiEnabled = true,
            downloadsEnabled = true,
            chatEnabled = true,
            videosEnabled = true
        )
        assertFalse(controls.isFeatureEnabled("ai"))
        assertFalse(controls.isFeatureEnabled("downloads"))
        assertFalse(controls.isFeatureEnabled("chat"))
        assertFalse(controls.isFeatureEnabled("videos"))
    }

    @Test
    fun `individual flags are respected when not locked`() {
        val controls = ParentControls(aiEnabled = false, downloadsEnabled = true)
        assertFalse(controls.isFeatureEnabled("ai"))
        assertTrue(controls.isFeatureEnabled("downloads"))
    }

    @Test
    fun `unknown feature keys default to enabled unless locked`() {
        val controls = ParentControls()
        assertTrue(controls.isFeatureEnabled("some_future_feature"))
    }

    @Test
    fun `defaults match legacy-web parent-controls-enforcer`() {
        val defaults = ParentControls()
        assertFalse(defaults.studyModeEnabled)
        assertTrue(defaults.aiEnabled)
        assertTrue(defaults.downloadsEnabled)
        assertFalse(defaults.emergencyLock)
        assertTrue(defaults.allowedSubjects.contains("all"))
    }
}
