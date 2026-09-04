package com.github.vgirotto.prism.services

internal data class InteractionAttribution(val sessionNames: List<String>)

/**
 * Groups overlapping agent interactions into one project-wide diff interval.
 * All calls are serialized by FileSnapshotService's executor.
 */
internal class GlobalInteractionCoordinator {
    private val activeSessionIds = linkedSetOf<String>()
    private val participants = linkedMapOf<String, String>()

    /** Returns true only when this interaction must capture a new global baseline. */
    fun begin(sessionId: String, sessionName: String): Boolean {
        if (!activeSessionIds.add(sessionId)) return false
        val captureBaseline = participants.isEmpty()
        participants[sessionId] = sessionName
        return captureBaseline
    }

    /** Returns attribution only when the last overlapping interaction finishes. */
    fun finish(sessionId: String): InteractionAttribution? {
        if (!activeSessionIds.remove(sessionId) || activeSessionIds.isNotEmpty()) return null
        val attribution = InteractionAttribution(participants.values.toList())
        participants.clear()
        return attribution
    }

    fun reset() {
        activeSessionIds.clear()
        participants.clear()
    }

    fun hasActiveInteraction(): Boolean = activeSessionIds.isNotEmpty()
}
