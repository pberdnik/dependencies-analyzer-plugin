package com.github.pberdnik.dependenciesanalyzerplugin.toolwindow

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBCheckBox
import java.awt.BorderLayout
import javax.swing.JPanel

class ModulesPanel(val project: Project) : JPanel(BorderLayout()) {

    init {
        val checkBox = JBCheckBox()
        add(ScrollPaneFactory.createScrollPane(myRightTree), BorderLayout.CENTER)
        ModuleManager.getInstance(project).modules
    }
}