package com.github.vgirotto.prism.toolwindow

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Verifies the keystroke sequences that drive Codex's two-step `/model`
 * picker. The picker is a stable, 1-based numbered list where pressing a
 * digit selects that row and advances/confirms; pressing Enter keeps the
 * pre-highlighted (current) row. See [CodexModelPicker] for the flow.
 */
class CodexModelPickerTest {

    @Test
    fun `selectModel picks model row then confirms the current effort`() {
        // gpt-5.4 is row 2; high effort is row 3 on the reasoning step.
        assertEquals(listOf("/model", "\r", "2", "3"), CodexModelPicker.selectModel(2, "high"))
    }

    @Test
    fun `selectModel accepts the default effort when current effort is unknown`() {
        // "auto"/unknown has no reasoning row, so the effort step is confirmed
        // with Enter, keeping the picked model's default level.
        assertEquals(listOf("/model", "\r", "1", "\r"), CodexModelPicker.selectModel(1, "auto"))
        assertEquals(listOf("/model", "\r", "3", "\r"), CodexModelPicker.selectModel(3, ""))
    }

    @Test
    fun `selectModel matches effort case-insensitively`() {
        assertEquals(listOf("/model", "\r", "2", "4"), CodexModelPicker.selectModel(2, "XHIGH"))
    }

    @Test
    fun `selectEffort keeps current model then picks the effort row`() {
        // Enter keeps the highlighted current model, advancing to the effort step;
        // digit 1 selects Low.
        assertEquals(listOf("/model", "\r", "\r", "1"), CodexModelPicker.selectEffort(1))
    }

    @Test
    fun `open helpers stop at the model and effort steps respectively`() {
        assertEquals(listOf("/model", "\r"), CodexModelPicker.OPEN_MODEL)
        assertEquals(listOf("/model", "\r", "\r"), CodexModelPicker.OPEN_EFFORT)
    }

    @Test
    fun `model and effort rows are numbered contiguously from one`() {
        assertEquals(listOf(1, 2, 3), CodexModelPicker.MODELS.map { it.second })
        assertEquals(listOf(1, 2, 3, 4), CodexModelPicker.EFFORTS.map { it.second })
    }
}
