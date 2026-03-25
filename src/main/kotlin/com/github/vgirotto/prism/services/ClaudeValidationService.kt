package com.github.vgirotto.prism.services

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Service for validating Claude CLI installation and process health.
 */
class ClaudeValidationService {

    private val log = Logger.getInstance(ClaudeValidationService::class.java)

    /**
     * Standard paths where Claude CLI is commonly installed.
     * Checks in order:
     * 1. ~/.local/bin/claude (npm install -g default on Linux/Mac)
     * 2. ~/.npm-global/bin/claude (npm global directory alternativo)
     * 3. /usr/local/bin/claude (Homebrew on Intel Mac)
     * 4. /opt/homebrew/bin/claude (Homebrew on Apple Silicon Mac)
     * 5. Fallback: `which claude` via PATH
     */
    private val CLAUDE_PATHS = listOf(
        "~/.local/bin/claude",
        "~/.npm-global/bin/claude",
        "/usr/local/bin/claude",
        "/opt/homebrew/bin/claude",
        "/usr/bin/claude"
    )

    /**
     * Checks if Claude CLI is available in known locations or PATH.
     * @return true if `claude` command exists and is executable
     */
    fun isClaudeAvailable(): Boolean {
        // First, check known installation paths
        for (path in CLAUDE_PATHS) {
            val expandedPath = expandHome(path)
            if (File(expandedPath).exists() && File(expandedPath).canExecute()) {
                log.debug("Found Claude at: $expandedPath")
                return true
            }
        }

        // Fallback: use `which` to search PATH
        return try {
            val process = ProcessBuilder("which", "claude").start()
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            completed && process.exitValue() == 0
        } catch (e: Exception) {
            log.debug("Claude availability check failed", e)
            false
        }
    }

    /**
     * Gets the full path to Claude CLI if available.
     * Searches in known locations first, then falls back to `which`.
     * @return path to claude executable, or null if not found
     */
    fun getClaudePath(): String? {
        // First, check known installation paths
        for (path in CLAUDE_PATHS) {
            val expandedPath = expandHome(path)
            val file = File(expandedPath)
            if (file.exists() && file.canExecute()) {
                log.debug("Found Claude at known path: $expandedPath")
                return expandedPath
            }
        }

        // Fallback: use `which` to search PATH
        return try {
            val process = ProcessBuilder("which", "claude").start()
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (completed && process.exitValue() == 0) {
                val path = process.inputStream.bufferedReader().readText().trim()
                if (path.isNotEmpty()) {
                    log.debug("Found Claude via which: $path")
                    path
                } else null
            } else null
        } catch (e: Exception) {
            log.debug("Failed to get Claude path", e)
            null
        }
    }

    /**
     * Expands ~ to user home directory.
     */
    private fun expandHome(path: String): String {
        return if (path.startsWith("~")) {
            val userHome = System.getProperty("user.home")
            path.replaceFirst("~", userHome)
        } else {
            path
        }
    }

    /**
     * Validates Claude CLI version (basic check that it responds to help).
     * @return true if Claude responds to --version or --help
     */
    fun validateClaudeVersion(): Boolean {
        return try {
            val process = ProcessBuilder("claude", "--version").start()
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            completed && process.exitValue() == 0
        } catch (e: Exception) {
            log.debug("Claude version validation failed", e)
            false
        }
    }

    /**
     * Checks if a process is still alive and reports errors if dead.
     * @return true if process is alive, false if dead
     */
    fun isProcessAlive(process: Process): Boolean {
        return try {
            process.isAlive
        } catch (e: Exception) {
            log.warn("Error checking process alive status", e)
            false
        }
    }

    /**
     * Generates a user-friendly error message for Claude not being found.
     */
    fun getClaudeNotFoundMessage(): String {
        return """
            |Claude Code is not installed.
            |
            |Install it with:
            |  npm install -g @anthropic-ai/claude-code
            |
            |After installation, restart the IDE.
        """.trimMargin()
    }

    /**
     * Generates error message for when a session dies unexpectedly.
     */
    fun getSessionDiedMessage(): String {
        return """
            |Claude session ended unexpectedly.
            |
            |This may happen due to:
            |  • Out of memory (OOM)
            |  • Process crash
            |  • System resource limits
            |
            |Try restarting the session.
        """.trimMargin()
    }

    companion object {
        fun getInstance(): ClaudeValidationService = ClaudeValidationService()
    }
}
