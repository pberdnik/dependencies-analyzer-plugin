package com.github.pberdnik.dependenciesanalyzerplugin.toolwindow

import com.github.pberdnik.dependenciesanalyzerplugin.panel.FileDependenciesPanel
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager

private const val GREEN_MODULES = "Green Modules"

class FileDependenciesToolWindow(private val project: Project) {
    private val LOG = Logger.getInstance(FileDependenciesToolWindow::class.java)

    private var contentManager: ContentManager? = null

    fun initToolWindow(toolWindow: ToolWindow) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized {
            contentManager = toolWindow.contentManager
            toolWindow.setAvailable(true, null)

            val panel = ModulesPanel(project)
            val content = ContentFactory.SERVICE.getInstance().createContent(panel, GREEN_MODULES, false)
            addContent(content)
        }
    }

    fun addContent(content: Content) {
        val contentManager = contentManager ?: return
        StartupManager.getInstance(project).runWhenProjectIsInitialized {
//            contentManager.removeAllContents(false)
            removeContentsExceptModules(contentManager)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
            ToolWindowManager.getInstance(project).getToolWindow("File Dependencies")!!.activate(null)
        }
    }

    private fun removeContentsExceptModules(contentManager: ContentManager) {
        val contents = contentManager.contents.clone()
        contents.forEach { content ->
            if (!content.displayName.equals(GREEN_MODULES)) {
                contentManager.removeContent(content, true)
            }
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