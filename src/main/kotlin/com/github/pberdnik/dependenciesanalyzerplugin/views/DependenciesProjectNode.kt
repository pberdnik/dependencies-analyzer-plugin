// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.pberdnik.dependenciesanalyzerplugin.views

import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.ReadAction
import java.lang.RuntimeException
import com.intellij.psi.search.FilenameIndex
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.PresentationData
import com.intellij.icons.AllIcons
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.JBColor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.util.Alarm
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileEvent
import javax.swing.SwingUtilities
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import java.util.ArrayList
import java.util.HashSet

class DependenciesProjectNode : AbstractTreeNode<VirtualFile?> {
    constructor(project: Project) : super(project, project.guessProjectDir()!!) {
        scanImages(project)
    }

    constructor(project: Project?, file: VirtualFile?) : super(project, file!!) {}

    private fun scanImages(project: Project) {
        addAllByExt(project, "kt")
        addAllByExt(project, "java")
    }

    // Creates a collection of files asynchronously
    private fun addAllByExt(project: Project, ext: String) {
        val imagesFiles = getAllFiles(project)
        val projectDir = project.guessProjectDir()
        try {
            val files = ReadAction.compute<Collection<VirtualFile>, RuntimeException> {
                FilenameIndex.getAllFilesByExt(project, ext)
            }
            for (f in files) {
                var file: VirtualFile? = f
                while (file != null && file != projectDir) {
                    imagesFiles.add(file)
                    file = file.parent
                }
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    private fun getAllFiles(project: Project): MutableSet<VirtualFile> {
        var files = project.getUserData(FILES_PROJECT_DIRS)
        if (files == null) {
            files = HashSet()
            project.putUserData(FILES_PROJECT_DIRS, files)
        }
        return files
    }

    override fun getVirtualFile(): VirtualFile {
        return value!!
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val files: MutableList<VirtualFile> = ArrayList(0)
        for (file in value!!.children) {
            if (getAllFiles(myProject).contains(file)) {
                files.add(file)
            }
        }
        if (files.isEmpty()) {
            return emptyList()
        }
        val nodes: MutableList<AbstractTreeNode<*>> = ArrayList(files.size)
        val alwaysOnTop = ProjectView.getInstance(myProject).isFoldersAlwaysOnTop("")
        files.sortWith { o1: VirtualFile, o2: VirtualFile ->
            if (alwaysOnTop) {
                val d1 = o1.isDirectory
                val d2 = o2.isDirectory
                if (d1 && !d2) {
                    return@sortWith -1
                }
                if (!d1 && d2) {
                    return@sortWith 1
                }
            }
            StringUtil.naturalCompare(o1.name, o2.name)
        }
        for (file in files) {
            nodes.add(DependenciesProjectNode(myProject, file))
        }
        return nodes
    }

    override fun update(data: PresentationData) {
        val value = value ?: return
        data.setIcon(if (value.isDirectory) AllIcons.Nodes.Folder else value.fileType.icon)
        data.addText(value.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        data.addText(" " + value.length, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN))
        data.addText(" 200", SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED))
    }

    override fun canNavigate(): Boolean {
        return value?.isDirectory == false
    }

    override fun canNavigateToSource(): Boolean {
        return canNavigate()
    }

    override fun navigate(requestFocus: Boolean) {
        val value = value ?: return
        FileEditorManager.getInstance(myProject).openFile(value, false)
    }

    companion object {
        private val FILES_PROJECT_DIRS = Key.create<MutableSet<VirtualFile>>("files.or.directories")
    }
}