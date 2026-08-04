package com.github.vgirotto.prism.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.EnvironmentUtil
import java.io.File

/**
 * Locates a CLI executable by checking a list of candidate paths, then scanning the
 * PATH the user's login shell exports. Shared between the per-CLI validation services
 * so lookup behavior stays uniform across agents.
 *
 * PATH comes from [EnvironmentUtil] rather than [System.getenv] because a GUI-launched
 * IDE inherits the desktop session's environment — on macOS, launchd's
 * `/usr/bin:/bin:/usr/sbin:/sbin` — which cannot see a CLI under `~/.local/bin`, nvm or
 * Homebrew. That map is loaded once at IDE startup and may still be loading, so callers
 * must stay off the EDT.
 *
 * Nothing is memoized: a full miss is tens of microseconds, while a stale answer
 * survives an install or an uninstall and reports the wrong thing.
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
