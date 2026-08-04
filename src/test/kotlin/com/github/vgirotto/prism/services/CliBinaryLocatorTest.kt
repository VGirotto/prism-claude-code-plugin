package com.github.vgirotto.prism.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class CliBinaryLocatorTest {

    @Test
    fun `locate returns first existing candidate path`(@TempDir tmp: Path) {
        val fake = executable(tmp, "fake-cli")

        val locator = locator(
            binaryName = "fake-cli",
            candidatePaths = listOf(
                "/definitely/missing/fake-cli",
                fake.toString(),
            ),
        )
        assertEquals(fake.toString(), locator.locate())
        assertTrue(locator.exists())
    }

    @Test
    fun `locate skips non-executable candidate paths`(@TempDir tmp: Path) {
        val nonExec = tmp.resolve("not-exec")
        Files.writeString(nonExec, "")
        nonExec.toFile().setExecutable(false)

        val locator = locator(binaryName = "missing", candidatePaths = listOf(nonExec.toString()))
        assertNull(locator.locate())
        assertFalse(locator.exists())
    }

    @Test
    fun `locate falls back to a PATH directory when no candidate matches`(@TempDir tmp: Path) {
        val onPath = Files.createDirectory(tmp.resolve("bin"))
        val fake = executable(onPath, "fake-cli")

        val locator = locator(
            binaryName = "fake-cli",
            candidatePaths = listOf("/definitely/missing/fake-cli"),
            pathEntries = listOf("/definitely/missing/bin", onPath.toString()),
        )
        assertEquals(fake.toString(), locator.locate())
    }

    @Test
    fun `locate ignores a PATH entry that is a directory sharing the binary name`(@TempDir tmp: Path) {
        // A directory is executable as far as the filesystem is concerned, so a bare
        // exists() check would hand back a path that cannot be launched.
        val shadow = Files.createDirectory(tmp.resolve("bin"))
        Files.createDirectory(shadow.resolve("fake-cli"))

        val locator = locator(
            binaryName = "fake-cli",
            candidatePaths = emptyList(),
            pathEntries = listOf(shadow.toString()),
        )
        assertNull(locator.locate())
    }

    @Test
    fun `expandHome replaces leading tilde with user home`() {
        val expanded = CliBinaryLocator.expandHome("~/foo/bar")
        assertEquals("${System.getProperty("user.home")}/foo/bar", expanded)
    }

    @Test
    fun `expandHome leaves non-tilde paths untouched`() {
        assertEquals("/abs/path", CliBinaryLocator.expandHome("/abs/path"))
        assertEquals("relative/path", CliBinaryLocator.expandHome("relative/path"))
    }

    @Test
    fun `resolve accepts a custom absolute path outside the candidate list`(@TempDir tmp: Path) {
        val custom = executable(tmp, "custom-cli")

        val locator = locator(
            binaryName = "fake-cli",
            candidatePaths = listOf("/definitely/missing/fake-cli"),
        )

        assertEquals(custom.toString(), locator.resolve(custom.toString()))
        assertTrue(locator.canResolve(custom.toString()))
    }

    @Test
    fun `resolve rejects a configured absolute path that doesn't exist`() {
        val locator = locator(
            binaryName = "fake-cli",
            candidatePaths = listOf("/definitely/missing/fake-cli"),
        )
        assertNull(locator.resolve("/no/such/binary"))
        assertFalse(locator.canResolve("/no/such/binary"))
    }

    @Test
    fun `resolve expands leading tilde in configured path`(@TempDir tmp: Path) {
        // We can only meaningfully test this if the candidate path lives under user.home;
        // fall back to a plain path-exists assertion otherwise.
        val home = System.getProperty("user.home")
        val custom = executable(tmp, "custom-cli")

        val locator = locator(binaryName = "fake-cli", candidatePaths = emptyList())

        if (custom.toString().startsWith(home)) {
            val withTilde = "~" + custom.toString().removePrefix(home)
            assertEquals(custom.toString(), locator.resolve(withTilde))
        } else {
            assertEquals(custom.toString(), locator.resolve(custom.toString()))
        }
    }

    @Test
    fun `resolve falls back to candidate paths when configured value equals default name`(@TempDir tmp: Path) {
        val fake = executable(tmp, "fake-cli")

        val locator = locator(binaryName = "fake-cli", candidatePaths = listOf(fake.toString()))
        // configuredPath == default → locate() path returns the candidate hit.
        assertEquals(fake.toString(), locator.resolve("fake-cli"))
    }

    @Test
    fun `resolve falls back to candidate paths when configured value is blank`(@TempDir tmp: Path) {
        val fake = executable(tmp, "fake-cli")

        val locator = locator(binaryName = "fake-cli", candidatePaths = listOf(fake.toString()))
        assertEquals(fake.toString(), locator.resolve(""))
        assertEquals(fake.toString(), locator.resolve("   "))
    }

    @Test
    fun `resolve looks up a bare name override on PATH`(@TempDir tmp: Path) {
        val onPath = Files.createDirectory(tmp.resolve("bin"))
        val override = executable(onPath, "other-cli")

        val locator = locator(
            binaryName = "fake-cli",
            candidatePaths = listOf("/definitely/missing/fake-cli"),
            pathEntries = listOf(onPath.toString()),
        )
        assertEquals(override.toString(), locator.resolve("other-cli"))
    }

    @Test
    fun `a cached hit skips the candidate scan`(@TempDir tmp: Path) {
        val preferred = tmp.resolve("preferred-cli")
        val fallback = executable(tmp, "fallback-cli")

        val locator = locator(
            binaryName = "fake-cli",
            candidatePaths = listOf(preferred.toString(), fallback.toString()),
        )
        assertEquals(fallback.toString(), locator.locate())

        // The higher-priority candidate appears afterwards. A cached hit is only
        // re-stat'd, never re-searched, so the answer stands.
        executable(tmp, "preferred-cli")
        assertEquals(fallback.toString(), locator.locate())
    }

    @Test
    fun `a cached hit is dropped once the binary stops being executable`(@TempDir tmp: Path) {
        val fake = executable(tmp, "fake-cli")

        val locator = locator(binaryName = "fake-cli", candidatePaths = listOf(fake.toString()))
        assertEquals(fake.toString(), locator.locate())

        // Uninstalled mid-session: handing out the cached path would launch a dead binary.
        Files.delete(fake)
        assertNull(locator.locate())
        assertFalse(locator.exists())
    }

    @Test
    fun `invalidate forces the next lookup to re-check disk`(@TempDir tmp: Path) {
        val fake = tmp.resolve("fake-cli")
        val locator = locator(binaryName = "fake-cli", candidatePaths = listOf(fake.toString()))
        assertNull(locator.locate())

        executable(tmp, "fake-cli")
        locator.invalidate()

        assertEquals(fake.toString(), locator.locate())
    }

    @Test
    fun `resolve caches per configured path so a changed path is looked up again`(@TempDir tmp: Path) {
        val first = executable(tmp, "first-cli")

        val locator = locator(binaryName = "fake-cli", candidatePaths = emptyList())
        assertEquals(first.toString(), locator.resolve(first.toString()))
        // A different configured path is a different cache key — no stale hit.
        assertNull(locator.resolve(tmp.resolve("second-cli").toString()))
    }

    @Test
    fun `a cached miss is trusted until the negative window lapses`(@TempDir tmp: Path) {
        var nanos = 0L
        val fake = tmp.resolve("fake-cli")
        val locator = CliBinaryLocator(
            binaryName = "fake-cli",
            candidatePaths = listOf(fake.toString()),
            negativeTtlMs = 60_000L,
            nowNanos = { nanos },
            pathEntries = { emptyList() },
        )
        assertNull(locator.locate())

        // Installed without touching settings, so nothing invalidates the cache.
        executable(tmp, "fake-cli")
        nanos += TimeUnit.MILLISECONDS.toNanos(59_999)
        assertNull(locator.locate())

        // Past the window the miss is retried, so no IDE restart is needed.
        nanos += TimeUnit.MILLISECONDS.toNanos(2)
        assertEquals(fake.toString(), locator.locate())
    }

    /**
     * PATH is always injected: the production default reads the login-shell
     * environment through the platform, which needs a running IDE, and a test that
     * leaned on the JVM's own PATH would pass or fail with the machine it runs on.
     */
    private fun locator(
        binaryName: String,
        candidatePaths: List<String>,
        pathEntries: List<String> = emptyList(),
    ) = CliBinaryLocator(
        binaryName = binaryName,
        candidatePaths = candidatePaths,
        pathEntries = { pathEntries },
    )

    private fun executable(dir: Path, name: String): Path {
        val file = dir.resolve(name)
        Files.writeString(file, "#!/bin/sh\nexit 0\n")
        file.toFile().setExecutable(true)
        return file
    }
}
