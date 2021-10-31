package com.github.pberdnik.dependenciesanalyzerplugin.actions

import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphStorageService
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import icons.SdkIcons

class GraphAnalysisAction : AnAction("Run Graph Analysis", "Run graph analysis", SdkIcons.coloredGraph) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        GraphStorageService.getInstance(project).analyze()
        ProjectView.getInstance(project).refresh()
    }
}