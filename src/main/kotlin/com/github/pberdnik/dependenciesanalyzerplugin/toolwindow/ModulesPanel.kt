package com.github.pberdnik.dependenciesanalyzerplugin.toolwindow

import com.github.pberdnik.dependenciesanalyzerplugin.actions.GraphAnalysisAction
import com.github.pberdnik.dependenciesanalyzerplugin.panel.FileDependenciesPanel
import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphConfigStorageService
import com.github.pberdnik.dependenciesanalyzerplugin.toolwindow.FileDependenciesToolWindow.Companion.getInstance
import com.intellij.CommonBundle
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.actions.AnalyzeDependenciesHandler
import com.intellij.packageDependencies.actions.MyAnalyzeDependenciesAction
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBCheckBox
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ModulesPanel(val project: Project) : JPanel(BorderLayout()) {
    private val graphConfig = GraphConfigStorageService.getInstance(project).state

    init {
        val modulesPanel = JPanel()
        modulesPanel.layout = BoxLayout(modulesPanel, BoxLayout.Y_AXIS)
        add(createToolbar(), BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(modulesPanel), BorderLayout.CENTER)
        ModuleManager.getInstance(project).modules.sortedBy { it.name }.forEach { module ->
            val name = module.name
            val checkBox = JBCheckBox(name, graphConfig.greenModules.contains(name))
            checkBox.addItemListener { itemEvent ->
                if (itemEvent.stateChange == ItemEvent.SELECTED) {
                    graphConfig.greenModules.add(name)
                } else if (itemEvent.stateChange == ItemEvent.DESELECTED) {
                    graphConfig.greenModules.remove(name)
                }
            }
            modulesPanel.add(checkBox)
        }
    }

    private fun createToolbar(): JComponent {
        val group = DefaultActionGroup()
        val myAnalyzeDependenciesAction = MyAnalyzeDependenciesAction()
        with(myAnalyzeDependenciesAction.templatePresentation) {
            icon = AllIcons.Actions.Rerun
            text = "Run Full Analysis"
            description = "Run full analysis"
        }
        group.add(myAnalyzeDependenciesAction)
        group.add(GraphAnalysisAction())
        val toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true)
        return toolbar.component
    }
}