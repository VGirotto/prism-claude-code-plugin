package com.github.vgirotto.prism.toolwindow

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.AWTEvent
import java.awt.event.KeyEvent
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.SwingUtilities

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
 * So the latch closes when Escape is pressed while a popup is up, and stays closed until
 * the key is physically released. The press that closes it is passed through untouched —
 * the popup still needs it — and so is every Escape not headed for [terminal], since
 * silencing the key for the rest of the IDE is none of this gate's business.
 *
 * Dropping events here, at the event queue, is deliberate: a forwarding action that
 * disabled itself instead would leave the keystroke unconsumed, and the terminal widget
 * writes Escape to the PTY through its own key handling, so it would arrive anyway by a
 * different route.
 *
 * Scoped to a session's [Disposable] rather than the application so the dispatcher cannot
 * outlive a plugin unload.
 */
internal class EscapeKeyGate(
    private val terminal: JComponent,
    parent: Disposable,
    private val latch: EscapeLatch = EscapeLatch(),
) {

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
            // A popup anywhere in the IDE closes the latch, but only this session's terminal
            // is ever silenced by it, so onPress runs first for its side effect.
            KeyEvent.KEY_PRESSED ->
                latch.onPress(JBPopupFactory.getInstance().isPopupActive) && isBoundForTerminal(event)

            KeyEvent.KEY_TYPED -> latch.isClosed() && isBoundForTerminal(event)

            KeyEvent.KEY_RELEASED -> {
                latch.onRelease()
                false
            }

            else -> false
        }
    }

    private fun isBoundForTerminal(event: KeyEvent): Boolean {
        val target = event.component ?: return false
        return SwingUtilities.isDescendingFrom(target, terminal)
    }

    // KEY_TYPED carries no key code, only the character.
    private fun KeyEvent.isEscape(): Boolean =
        keyCode == KeyEvent.VK_ESCAPE || (id == KeyEvent.KEY_TYPED && keyChar == ESCAPE_CHAR)

    private companion object {
        const val ESCAPE_CHAR = '\u001B'
    }
}

/**
 * Whether Escape is currently spoken for by a popup rather than by the agent.
 *
 * Split out of [EscapeKeyGate] so the press/release/expiry rules can be tested without an
 * event queue, a popup, or a running IDE.
 */
internal class EscapeLatch(private val nowNanos: () -> Long = System::nanoTime) {

    private var closedAtNanos: Long? = null

    /**
     * @param popupActive whether a popup was open when the press arrived.
     * @return true if the press should be dropped instead of reaching the agent.
     */
    fun onPress(popupActive: Boolean): Boolean {
        if (popupActive) {
            // The popup gets this press; the auto-repeats behind it are the problem.
            closedAtNanos = nowNanos()
            return false
        }
        return isClosed()
    }

    fun onRelease() {
        closedAtNanos = null
    }

    /**
     * A release delivered to another window never reaches the queue, so the latch also
     * expires on its own: a briefly swallowed Escape is a far smaller bug than one that
     * stops interrupting the agent for the rest of the session.
     */
    fun isClosed(): Boolean {
        val closedAt = closedAtNanos ?: return false
        if (nowNanos() - closedAt > EXPIRY_NANOS) {
            closedAtNanos = null
            return false
        }
        return true
    }

    companion object {
        val EXPIRY_NANOS: Long = TimeUnit.SECONDS.toNanos(5)
    }
}
