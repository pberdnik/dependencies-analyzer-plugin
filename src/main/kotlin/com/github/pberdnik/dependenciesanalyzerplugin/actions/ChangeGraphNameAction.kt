// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.pberdnik.dependenciesanalyzerplugin.actions

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile
import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphStorageService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.util.*
import javax.swing.Icon

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file. But when added at runtime this class is instantiated by an action group.
 */
class ChangeGraphNameAction : AnAction {
    /**
     * This default constructor is used by the IntelliJ Platform framework to instantiate this class based on plugin.xml
     * declarations. Only needed in [ChangeGraphNameAction] class because a second constructor is overridden.
     *
     * @see AnAction
     */
    constructor() : super() {}

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     *
     * @param text        The text to be displayed as a menu item.
     * @param description The description of the menu item.
     * @param icon        The icon to be used with the menu item.
     */
    constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon) {}

    /**
     * Gives the user feedback when the dynamic action menu is chosen.
     * Pops a simple message dialog. See the psi_demo plugin for an
     * example of how to use [AnActionEvent] to access data.
     *
     * @param event Event received when the associated menu item is chosen.
     */
    override fun actionPerformed(event: AnActionEvent) {
        // Using the event, create and show a dialog
        val currentProject = event.project ?: return
        val graphStorageService = GraphStorageService.getInstance(currentProject)
        val graph = graphStorageService.state
        val random = Random()
        val randomPath = "/random/path${random.nextInt() % 12}"
//        val codeFiles = mutableSetOf<CodeFile>()
//        codeFiles.add(CodeFile(randomPath, 0, listOf()))
//        graph.codeFiles = codeFiles.toList()
        graph.codeFiles.add(
            CodeFile(
                randomPath,
                0,
                listOf()
            )
        )
        val dlgMsg = "Added path: $randomPath"
        val dlgTitle = "Graph Name Changed"
        Messages.showMessageDialog(currentProject, dlgMsg, dlgTitle, Messages.getInformationIcon())
    }

    /**
     * Determines whether this menu item is available for the current context.
     * Requires a project to be open.
     *
     * @param e Event received when the associated group-id menu is chosen.
     */
    override fun update(e: AnActionEvent) {
        // Set the availability based on whether a project is open
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }
}