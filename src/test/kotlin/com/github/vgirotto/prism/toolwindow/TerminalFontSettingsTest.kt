package com.github.vgirotto.prism.toolwindow

import com.github.vgirotto.prism.toolwindow.AgentToolWindowFactory.Companion.supportsDedicatedTerminalFontSettings
import com.intellij.openapi.util.BuildNumber
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TerminalFontSettingsTest {

    private fun build(value: String): BuildNumber = requireNotNull(BuildNumber.fromString(value))

    @Test
    fun `the oldest supported IDE has no dedicated terminal font settings`() {
        assertFalse(supportsDedicatedTerminalFontSettings(build("243.21565.193")))
    }

    @Test
    fun `IntelliJ 2025_1 has no dedicated terminal font settings`() {
        assertFalse(supportsDedicatedTerminalFontSettings(build("251.23774.435")))
    }

    @Test
    fun `IntelliJ 2025_1_1 is the first release with dedicated terminal font settings`() {
        assertTrue(supportsDedicatedTerminalFontSettings(build("251.25410.28")))
    }

    @Test
    fun `a later IDE keeps the dedicated terminal font settings`() {
        assertTrue(supportsDedicatedTerminalFontSettings(build("262.10315.69")))
    }
}
