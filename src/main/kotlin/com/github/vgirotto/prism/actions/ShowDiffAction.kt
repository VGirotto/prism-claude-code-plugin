package com.github.vgirotto.prism.actions

import com.github.vgirotto.prism.services.FileSnapshotService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class ShowDiffAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Compute the diff
        FileSnapshotService.getInstance(project).computeDiff()

        // Show and activate the Claude Code tool window (Changes panel is always visible)
        ToolWindowManager.getInstance(project)
            .getToolWindow("Claude Code")
            ?.activate(null)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
