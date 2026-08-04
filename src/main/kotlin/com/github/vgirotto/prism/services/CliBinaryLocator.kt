package com.github.vgirotto.prism.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.EnvironmentUtil
import java.io.File

/**
 * Locates a CLI executable on the host machine by checking a list of
 * candidate paths, then falling back to a scan of the PATH the user's login
 * shell exports.
 *
 * Shared between per-CLI validation services (Claude, Codex, ...) so the
 * lookup behavior stays uniform across agents.
 *
 * PATH comes from [EnvironmentUtil], not [System.getenv]: a GUI-launched IDE
 * inherits its environment from the desktop session rather than from a login
 * shell, so on macOS the IDE process sees launchd's
 * `/usr/bin:/bin:/usr/sbin:/sbin` and a CLI under `~/.local/bin`, nvm, volta or
 * Homebrew is invisible to it even though `which` finds it in a terminal.
 * [EnvironmentUtil] runs the login shell once during IDE startup and caches the
 * result, so a lookup here costs a handful of stats and never spawns a process.
 * Because that map may still be loading, callers must stay off the EDT.
 *
 * Results are deliberately not memoized: a full miss over every candidate and
 * every PATH entry measures tens of microseconds, which is far below the cost of
 * being wrong. A cache that outlives an install, an upgrade or an uninstall
 * reports a CLI that isn't there, or hides one that is.
 */
class CliBinaryLocator(
    private val binaryName: String,
    private val candidatePaths: List<String>,
    private val pathEntries: () -> List<String> = ::loginShellPath,
) {

    private val log = Logger.getInstance(CliBinaryLocator::class.java)

    /** Full path to the binary, or null if it is not found in any candidate or on PATH. */
    fun locate(): String? {
        for (path in candidatePaths) {
            val expanded = expandHome(path)
            if (isExecutable(expanded)) {
                log.debug("Found $binaryName at known path: $expanded")
                return expanded
            }
        }
        return onPath(binaryName)
    }

    /** True if [locate] would return a non-null path. */
    fun exists(): Boolean = locate() != null

    /**
     * Resolves a user-configured CLI path entry from settings:
     *   - blank or equal to the default [binaryName] → fall back to [locate].
     *   - contains a `/` (or `~`) → treat as a filesystem path and check
     *     directly (after expanding `~`).
     *   - bare name (different from the default) → PATH lookup using that
     *     name; candidate paths are skipped since they're tied to the default
     *     binary.
     */
    fun resolve(configuredPath: String): String? {
        val trimmed = configuredPath.trim()
        if (trimmed.isEmpty() || trimmed == binaryName) return locate()

        val expanded = expandHome(trimmed)
        if (!expanded.contains('/')) return onPath(trimmed)
        // Logged like the other branches: without this, a configured path is the one
        // resolution that leaves no trace in the log when a lookup has to be diagnosed.
        return if (isExecutable(expanded)) {
            log.debug("Found $binaryName at configured path: $expanded")
            expanded
        } else {
            log.debug("No executable for $binaryName at configured path: $expanded")
            null
        }
    }

    /** True if [resolve] would return a non-null path for [configuredPath]. */
    fun canResolve(configuredPath: String): Boolean = resolve(configuredPath) != null

    /** A directory or a non-executable file is not a usable binary, matching `which`. */
    private fun isExecutable(path: String): Boolean = File(path).let { it.isFile && it.canExecute() }

    private fun onPath(name: String): String? {
        for (dir in pathEntries()) {
            val candidate = File(dir, name).path
            if (isExecutable(candidate)) {
                log.debug("Found $name on PATH: $candidate")
                return candidate
            }
        }
        log.debug("No $name in candidate paths or on PATH")
        return null
    }

    companion object {
        fun expandHome(path: String): String =
            if (path.startsWith("~")) path.replaceFirst("~", System.getProperty("user.home")) else path

        /** PATH as the user's login shell exports it, which is not the IDE process's own. */
        private fun loginShellPath(): List<String> =
            EnvironmentUtil.getValue("PATH")
                ?.split(File.pathSeparatorChar)
                ?.filter { it.isNotEmpty() }
                .orEmpty()
    }
}
