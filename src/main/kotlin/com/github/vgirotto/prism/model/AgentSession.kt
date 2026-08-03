package com.github.vgirotto.prism.model

import com.github.vgirotto.prism.services.AgentTtyConnector
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.util.Timer
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Represents a single agent session with its own process, state, and metadata.
 */
class AgentSession(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Chat",
    val cli: AgentCli = AgentCli.DEFAULT,
) : Disposable {

    var process: Process? = null
    var connector: AgentTtyConnector? = null
    var idleTimer: Timer? = null
    var healthTimer: Timer? = null

    @Volatile var model: String = ""
    @Volatile var effort: String = ""
    @Volatile var state: SessionState = SessionState.STOPPED
    @Volatile var userHasInteracted: Boolean = false
    @Volatile var outputActive: Boolean = false
    @Volatile var idleFiredForCurrentInteraction: Boolean = false
    @Volatile var snapshotTakenForCurrentInput: Boolean = false

    /** Monotonic reading taken as the session launch begins; 0 until it does. */
    @Volatile var launchStartedAtNanos: Long = 0L

    /** Guards the one-shot "first output" startup timing log. */
    @Volatile var firstOutputLogged: Boolean = false

    enum class SessionState { STOPPED, STARTING, IDLE, WORKING }

    val isAlive: Boolean get() = process?.isAlive == true

    /** Ms since the launch began, or -1 if this session hasn't started yet. */
    fun elapsedSinceLaunchMs(): Long =
        if (launchStartedAtNanos == 0L) -1
        else TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - launchStartedAtNanos)

    override fun dispose() {
        idleTimer?.cancel()
        idleTimer = null
        healthTimer?.cancel()
        healthTimer = null
        try { connector?.close() } catch (_: Exception) {}
        try {
            process?.let { if (it.isAlive) it.destroy() }
        } catch (_: Exception) {}
        process = null
        connector = null
        state = SessionState.STOPPED
    }
}
