package com.github.vgirotto.prism.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for Claude CLI validation and error handling.
 */
class ClaudeValidationServiceTest {

    private val validator = ClaudeValidationService.getInstance()

    @Test
    fun testGetClaudeNotFoundMessage() {
        val message = validator.getClaudeNotFoundMessage()
        assertTrue(message.contains("Claude Code is not installed"))
        assertTrue(message.contains("npm install -g @anthropic-ai/claude-code"))
    }

    @Test
    fun testGetSessionDiedMessage() {
        val message = validator.getSessionDiedMessage()
        assertTrue(message.contains("Claude session ended unexpectedly"))
        assertTrue(message.contains("Out of memory"))
    }

    @Test
    fun testIsClaudeAvailable() {
        // This test depends on system state
        // If 'which' command doesn't exist (Windows), it should return false
        val available = validator.isClaudeAvailable()
        assertNotNull(available)
    }

    @Test
    fun testIsProcessAlive() {
        // Create a process and test if it's alive
        val process = ProcessBuilder("echo", "test").start()
        process.waitFor()

        // After process finishes, should be dead
        assertFalse(validator.isProcessAlive(process))
    }

    @Test
    fun testProcessDeathDetection() {
        // Create a short-lived process
        val process = ProcessBuilder("sleep", "0.1").start()

        // Should be alive initially
        assertTrue(process.isAlive)

        // Wait for it to finish
        process.waitFor()

        // Should be dead now
        assertFalse(process.isAlive)
    }
}
