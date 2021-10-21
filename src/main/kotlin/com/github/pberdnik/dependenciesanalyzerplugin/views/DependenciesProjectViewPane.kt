// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.pberdnik.dependenciesanalyzerplugin.views

import com.intellij.ide.projectView.impl.AbstractProjectViewPSIPane
import com.intellij.ide.SelectInTarget
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.ViewSettings
import javax.swing.tree.DefaultTreeModel
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.openapi.project.Project
import icons.SdkIcons
import java.lang.IllegalStateException
import javax.swing.Icon

class DependenciesProjectViewPane constructor(project: Project) : AbstractProjectViewPSIPane(project) {

    override fun getTitle(): String = "Marked Dependencies"
    override fun getIcon(): Icon = SdkIcons.coloredGraph
    override fun getId(): String = ID
    override fun getWeight(): Int = 10

    override fun createSelectInTarget(): SelectInTarget {
        return object : ProjectViewSelectInTarget(myProject) {
            override fun toString(): String = ID
            override fun getMinorViewId(): String = ID
            override fun getWeight(): Float = 10f
        }
    }

    override fun createStructure(): ProjectAbstractTreeStructureBase {
        return object : ProjectTreeStructure(myProject, ID) {
            override fun createRoot(project: Project, settings: ViewSettings) = DependenciesProjectNode(project)
            // Children will be searched in async mode
            override fun isToBuildChildrenInBackground(element: Any) = true
        }
    }

    override fun createTree(model: DefaultTreeModel): ProjectViewTree {
        return object : ProjectViewTree(model) {
            override fun isRootVisible() = true
        }
    }

    //  Legacy code, awaiting refactoring of AbstractProjectViewPSIPane#createBuilder
    override fun createBuilder(treeModel: DefaultTreeModel)= null

    //  Legacy code, awaiting refactoring of AbstractProjectViewPSIPane#createTreeUpdater
    override fun createTreeUpdater(builder: AbstractTreeBuilder): AbstractTreeUpdater {
        throw IllegalStateException("DependenciesProjectViewPane tree is async now")
    }

    companion object {
        const val ID = "MARKED_DEPENDENCIES"
    }
}