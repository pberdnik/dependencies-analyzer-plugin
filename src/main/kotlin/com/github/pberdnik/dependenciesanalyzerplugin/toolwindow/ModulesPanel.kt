package com.github.pberdnik.dependenciesanalyzerplugin.toolwindow

import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphConfigStorageService
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.selected
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JPanel

class ModulesPanel(val project: Project) : JPanel(BorderLayout()) {
    private val graphConfig = GraphConfigStorageService.getInstance(project).state

    init {
        val modulesPanel = JPanel()
        modulesPanel.layout = BoxLayout(modulesPanel, BoxLayout.Y_AXIS)
        add(ScrollPaneFactory.createScrollPane(modulesPanel), BorderLayout.CENTER)
        ModuleManager.getInstance(project).modules.forEach { module ->
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
}