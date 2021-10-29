package com.github.pberdnik.dependenciesanalyzerplugin.toolwindow

import com.intellij.ide.impl.ContentManagerWatcher
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager

class FileDependenciesToolWindow(private val project: Project) {
    private val LOG = Logger.getInstance(FileDependenciesToolWindow::class.java)

    private var contentManager: ContentManager? = null

    fun initToolWindow(toolWindow: ToolWindow) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized {
            contentManager = toolWindow.contentManager
            toolWindow.setAvailable(true, null)
        }
    }

    fun addContent(content: Content) {
        LOG.warn("addContent($content); contentManager=$contentManager", Exception())

        val contentManager = contentManager ?: return
        StartupManager.getInstance(project).runWhenProjectIsInitialized {
            contentManager.removeAllContents(false)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
            ToolWindowManager.getInstance(project).getToolWindow("File Dependencies")!!.activate(null)
        }
    }

    fun closeContent(content: Content?) {
        contentManager?.removeContent(content!!, true)
    }

    companion object {
        fun getInstance(project: Project): FileDependenciesToolWindow {
            return ServiceManager.getService(project, FileDependenciesToolWindow::class.java)
        }
    }
}