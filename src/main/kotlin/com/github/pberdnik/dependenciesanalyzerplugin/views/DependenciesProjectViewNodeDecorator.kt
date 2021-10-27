package com.github.pberdnik.dependenciesanalyzerplugin.views

import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphStorageService
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes

private val REGULAR_TEXT = SimpleTextAttributes.REGULAR_ATTRIBUTES
private val GREEN_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN)
private val RED_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED)
private val YELLOW_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.YELLOW)
private val GRAY_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY)

class DependenciesProjectViewNodeDecorator(val project: Project) : ProjectViewNodeDecorator {
    private val LOG = Logger.getInstance(DependenciesProjectViewNodeDecorator::class.java)
    private val storage = GraphStorageService.getInstance(project)

    init {
        LOG.warn("INIT; storage=$storage")
    }

    override fun decorate(node: ProjectViewNode<*>?, data: PresentationData?) {
        if (node == null || data == null) return
        val value = node.value ?: return
        val file = node.virtualFile ?: PsiUtilCore.getVirtualFile(value as? PsiElement)
        val path = file?.path ?: return
        val nodeView = storage.nodeViews[path] ?: return
        data.clearText()
        data.presentableText = ""
        data.addText(file.name, REGULAR_TEXT)
        when (nodeView) {
            is DirNodeView -> with(nodeView) {
                if (greenSize > 0) data.addText(" $greenSize", GREEN_TEXT)
                if (redSize > 0) data.addText(" $redSize", RED_TEXT)
                if (yellowSize > 0) data.addText(" $yellowSize", YELLOW_TEXT)
            }
            is FileNodeView -> with(nodeView) {
                val textColor = when (color) {
                    FileNodeViewColor.GREEN -> GREEN_TEXT
                    FileNodeViewColor.RED -> RED_TEXT
                    FileNodeViewColor.YELLOW -> YELLOW_TEXT
                    FileNodeViewColor.GRAY -> GRAY_TEXT
                }
                data.addText(" $size [$depth]", textColor)
                if (isCycle) data.addText(" {C}", RED_TEXT)
            }
        }
    }

    override fun decorate(node: PackageDependenciesNode?, cellRenderer: ColoredTreeCellRenderer?) {
    }
}