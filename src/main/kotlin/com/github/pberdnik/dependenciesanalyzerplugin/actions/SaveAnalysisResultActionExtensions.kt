package com.github.pberdnik.dependenciesanalyzerplugin.actions

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile
import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphStorageService.Companion.getInstance
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.util.*

fun performAction(dependencies: MutableMap<PsiFile, MutableSet<PsiFile>>, project: Project) {
    val codeFiles: MutableList<CodeFile> = ArrayList()

    val files = dependencies.keys
    for (file in files) {
        val filePath = file.virtualFile.path
        val deps: MutableList<String> = ArrayList()
        val fileDeps = dependencies[file] ?: Collections.emptyList()
        for (dep in fileDeps) {
            deps.add(dep.virtualFile.path)
        }
        codeFiles.add(
            CodeFile(
                filePath,
                0,
                deps
            )
        )
    }

    val storage = getInstance(project)
    val state = storage.state
    state.codeFiles = codeFiles
}