package com.github.vgirotto.prism.services

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Locates a CLI executable on the host machine by checking a list of
 * candidate paths, then falling back to `which <binary>` on PATH.
 *
 * Shared between per-CLI validation services (Claude, Codex, ...) so the
 * lookup behavior stays uniform across agents.
 *
 * Lookups are memoized, since callers re-ask on every "+ New Session" click and
 * a miss can cost a `which` spawn. A cached path is still stat'd on every call,
 * so a CLI that is uninstalled mid-session is not handed out as a dead path; a
 * cached miss is retried after [negativeTtlMs]. [invalidate] drops everything.
 */
class CliBinaryLocator(
    private val binaryName: String,
    private val candidatePaths: List<String>,
    private val negativeTtlMs: Long = NEGATIVE_TTL_MS,
    private val nowNanos: () -> Long = System::nanoTime,
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
     *   - bare name (different from the default) → `which` lookup using
     *     that name; candidate paths are skipped since they're tied to
     *     the default binary.
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
        return whichOnPath(binaryName)
    }

    private fun resolveUncached(trimmed: String): String? {
        val expanded = expandHome(trimmed)
        if (expanded.contains('/')) {
            return expanded.takeIf { isExecutable(it) }
        }
        return whichOnPath(trimmed)
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
        // cheaper than serializing every caller behind a `which` spawn.
        val resolved = resolve()
        cache[key] = Resolution(resolved, nowNanos())
        return resolved
    }

    private fun isExecutable(path: String): Boolean = File(path).let { it.exists() && it.canExecute() }

    private fun whichOnPath(name: String): String? {
        return try {
            val process = ProcessBuilder("which", name).start()
            val completed = process.waitFor(WHICH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (completed && process.exitValue() == 0) {
                val path = process.inputStream.bufferedReader().readText().trim()
                path.takeIf { it.isNotEmpty() }?.also {
                    log.debug("Found $name via which: $it")
                }
            } else null
        } catch (e: Exception) {
            log.debug("which lookup failed for $name", e)
            null
        }
    }

    private val negativeTtlNanos: Long get() = TimeUnit.MILLISECONDS.toNanos(negativeTtlMs)

    private class Resolution(val path: String?, val resolvedAtNanos: Long)

    companion object {
        /** How long a "not found" answer is trusted before disk and PATH are re-checked. */
        const val NEGATIVE_TTL_MS = 60_000L

        private const val WHICH_TIMEOUT_SECONDS = 5L

        /**
         * Cache key for the no-configured-path lookup performed by [locate]. Written as a
         * textual escape so the source stays valid UTF-8 text; a literal NUL byte makes Git
         * treat the file as binary and hides its diff. Cannot collide with a [resolve] key,
         * since those are trimmed and non-blank.
         */
        private const val DEFAULT_KEY = "\u0000default"

        fun expandHome(path: String): String =
            if (path.startsWith("~")) path.replaceFirst("~", System.getProperty("user.home")) else path
    }
}
