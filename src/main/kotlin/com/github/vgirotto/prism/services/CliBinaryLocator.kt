package com.github.vgirotto.prism.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.EnvironmentUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
 * Lookups are memoized, since callers re-ask on every "+ New Session" click. A
 * cached path is still stat'd on every call, so a CLI that is uninstalled
 * mid-session is not handed out as a dead path; a cached miss is retried after
 * [negativeTtlMs]. [invalidate] drops everything.
 */
class CliBinaryLocator(
    private val binaryName: String,
    private val candidatePaths: List<String>,
    private val negativeTtlMs: Long = NEGATIVE_TTL_MS,
    private val nowNanos: () -> Long = System::nanoTime,
    private val pathEntries: () -> List<String> = ::loginShellPath,
) {

    private val log = Logger.getInstance(CliBinaryLocator::class.java)

    private val cache = ConcurrentHashMap<String, Resolution>()

    /** Full path to the binary, or null if it is not found in any candidate or on PATH. */
    fun locate(): String? = cached(DEFAULT_KEY) { locateUncached() }

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
        return cached(trimmed) { resolveUncached(trimmed) }
    }

    /** True if [resolve] would return a non-null path for [configuredPath]. */
    fun canResolve(configuredPath: String): Boolean = resolve(configuredPath) != null

    /** Drops memoized results so the next lookup re-checks disk and PATH. */
    fun invalidate() {
        cache.clear()
    }

    private fun locateUncached(): String? {
        for (path in candidatePaths) {
            val expanded = expandHome(path)
            if (isExecutable(expanded)) {
                log.debug("Found $binaryName at known path: $expanded")
                return expanded
            }
        }
        return onPath(binaryName)
    }

    private fun resolveUncached(trimmed: String): String? {
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

    private fun cached(key: String, resolve: () -> String?): String? {
        val hit = cache[key]
        val hitPath = hit?.path
        when {
            hit == null -> {}
            // Cheap re-check: the search was the expensive part, not the stat.
            hitPath != null -> if (isExecutable(hitPath)) return hitPath
            nowNanos() - hit.resolvedAtNanos < negativeTtlNanos -> return null
        }
        // Resolved outside any lock on purpose: a duplicate concurrent lookup is
        // cheaper than serializing every caller behind the search.
        val resolved = resolve()
        cache[key] = Resolution(resolved, nowNanos())
        return resolved
    }

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

    private val negativeTtlNanos: Long get() = TimeUnit.MILLISECONDS.toNanos(negativeTtlMs)

    private class Resolution(val path: String?, val resolvedAtNanos: Long)

    companion object {
        /** How long a "not found" answer is trusted before disk and PATH are re-checked. */
        const val NEGATIVE_TTL_MS = 60_000L

        /**
         * Cache key for the no-configured-path lookup performed by [locate]. Written as a
         * textual escape so the source stays valid UTF-8 text; a literal NUL byte makes Git
         * treat the file as binary and hides its diff. Cannot collide with a [resolve] key,
         * since those are trimmed and non-blank.
         */
        private const val DEFAULT_KEY = "\u0000default"

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
