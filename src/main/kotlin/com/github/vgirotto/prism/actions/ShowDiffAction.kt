package com.github.vgirotto.prism.actions

import com.github.vgirotto.prism.toolwindow.AgentToolWindowFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class ShowDiffAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Prism") ?: return
        toolWindow.activate {
            for (content in toolWindow.contentManager.contentsRecursively) {
                content.getUserData(AgentToolWindowFactory.DIFF_PANEL_KEY)?.refreshDiff()
            }
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
