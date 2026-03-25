package com.github.vgirotto.prism.listeners

import com.github.vgirotto.prism.services.FileSnapshotService
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent

/**
 * Listens for VFS file changes.
 *
 * - before(): saves original content BEFORE the file is modified (critical for diff)
 * - after(): records the path as changed for diff computation
 */
class FileChangeListener : BulkFileListener {

    override fun before(events: List<VFileEvent>) {
        for (event in events) {
            // Only save content for modifications and deletions (file exists now, will change)
            val path = when (event) {
                is VFileContentChangeEvent -> event.path
                is VFileDeleteEvent -> event.path
                else -> continue
            }

            for (project in ProjectManager.getInstance().openProjects) {
                val basePath = project.basePath ?: continue
                if (path.startsWith(basePath)) {
                    FileSnapshotService.getInstance(project).saveOriginalContent(path)
                }
            }
        }
    }

    override fun after(events: List<VFileEvent>) {
        for (event in events) {
            val path = when (event) {
                is VFileContentChangeEvent -> event.path
                is VFileCreateEvent -> event.path
                is VFileDeleteEvent -> event.path
                is VFileMoveEvent -> event.path
                else -> continue
            }

            for (project in ProjectManager.getInstance().openProjects) {
                val basePath = project.basePath ?: continue
                if (path.startsWith(basePath)) {
                    FileSnapshotService.getInstance(project).recordChange(path)
                }
            }
        }
    }
}
