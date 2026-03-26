package com.github.vgirotto.prism.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class OpenClaudeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Prism") ?: return

        if (toolWindow.isVisible) {
            if (toolWindow.isActive) {
                toolWindow.hide()
            } else {
                toolWindow.activate(null)
            }
        } else {
            toolWindow.activate(null)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
