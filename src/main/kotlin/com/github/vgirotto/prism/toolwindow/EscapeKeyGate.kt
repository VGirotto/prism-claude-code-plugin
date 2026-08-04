package com.github.vgirotto.prism.toolwindow

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.AWTEvent
import java.awt.event.KeyEvent
import java.util.concurrent.TimeUnit

/**
 * Keeps a *held* Escape from reaching the agent after a popup has already answered it.
 *
 * Escape over a session terminal has two plausible owners: an open popup wants to close,
 * and the agent behind it wants to interrupt. Holding the key makes both happen in
 * sequence — auto-repeat delivers one KEY_PRESSED after another, the first closes the
 * popup, and the next arrives with no popup left to claim it, so it reaches the PTY and
 * Codex answers "No previous messages to edit." for a keystroke the user spent on the
 * popup. A grace period after the popup closes cannot separate the two, because the
 * repeat delay is a per-user OS setting and can outlast any window worth picking.
 *
 * So the gate latches when Escape is pressed while a popup is up, and stays latched until
 * the key is physically released, dropping the repeats in between. The press that latches
 * is passed through untouched — the popup still needs it to close.
 *
 * Dropping them here, at the event queue, is deliberate: a forwarding action that disables
 * itself instead would leave the keystroke unconsumed, and the terminal widget writes
 * Escape to the PTY through its own key handling, so the keystroke would arrive anyway by
 * a different route.
 *
 * Scoped to a session's [Disposable] rather than the application so the dispatcher cannot
 * outlive a plugin unload. The state is about the physical key, so several instances simply
 * latch and unlatch in agreement.
 */
internal class EscapeKeyGate(parent: Disposable) {

    private var latchedAtNanos: Long? = null

    init {
        IdeEventQueue.getInstance().addDispatcher(
            object : IdeEventQueue.EventDispatcher {
                override fun dispatch(event: AWTEvent): Boolean = dispatchEscape(event)
            },
            parent,
        )
    }

    /** @return true to swallow [event] so neither the forwarding action nor the PTY sees it. */
    private fun dispatchEscape(event: AWTEvent): Boolean {
        if (event !is KeyEvent || !event.isEscape()) return false
        return when (event.id) {
            KeyEvent.KEY_PRESSED ->
                if (JBPopupFactory.getInstance().isPopupActive) {
                    // Latch, but let the popup have this press.
                    latchedAtNanos = System.nanoTime()
                    false
                } else {
                    // No popup left: either a repeat of the press one just consumed, or a
                    // fresh press meant for the agent.
                    isLatched()
                }

            KeyEvent.KEY_TYPED -> isLatched()

            KeyEvent.KEY_RELEASED -> {
                latchedAtNanos = null
                false
            }

            else -> false
        }
    }

    /**
     * A release delivered to another window would never reach the queue, so the latch also
     * expires on its own: a briefly swallowed Escape is a far smaller bug than one that
     * stops interrupting the agent for the rest of the session.
     */
    private fun isLatched(): Boolean {
        val latchedAt = latchedAtNanos ?: return false
        if (System.nanoTime() - latchedAt > LATCH_EXPIRY_NANOS) {
            latchedAtNanos = null
            return false
        }
        return true
    }

    // KEY_TYPED carries no key code, only the character.
    private fun KeyEvent.isEscape(): Boolean =
        keyCode == KeyEvent.VK_ESCAPE || (id == KeyEvent.KEY_TYPED && keyChar == ESCAPE_CHAR)

    companion object {
        private const val ESCAPE_CHAR = '\u001B'

        private val LATCH_EXPIRY_NANOS = TimeUnit.SECONDS.toNanos(5)
    }
}
