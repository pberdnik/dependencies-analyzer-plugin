package com.github.pberdnik.dependenciesanalyzerplugin.actions

import com.github.pberdnik.dependenciesanalyzerplugin.old.common.Config
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile
import com.github.pberdnik.dependenciesanalyzerplugin.old.graph.GraphConfig
import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphStorageService.Companion.getInstance
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.util.*

fun performAction(dependencies: MutableMap<PsiFile, MutableSet<PsiFile>>, project: Project) {
    val storage = getInstance(project)
    val codeFiles = mutableMapOf<String, CodeFile>()
    val config = storage.graphConfig
    val files = dependencies.keys
    for (file in files) {
        if (file.isBad(config)) continue
        val codeFile = codeFiles.createAndAddCodeFile(file)
        val deps: MutableList<String> = ArrayList()
        val fileDeps = dependencies[file] ?: Collections.emptyList()
        for (dep in fileDeps) {
            if (dep.isBad(config)) continue
            codeFiles.createAndAddCodeFile(dep)
            deps.add(dep.virtualFile.path)
        }
        codeFile.dependencies = deps
    }

    val state = storage.state
    state.codeFiles = codeFiles.values.toList()

    storage.analyze()
}

private fun MutableMap<String, CodeFile>.createAndAddCodeFile(
    file: PsiFile
): CodeFile {
    val virtualFile = file.virtualFile
    val path = virtualFile.path
    return this[path] ?: run {
        val module = ModuleUtil.findModuleForFile(file)?.name ?: ""
        val newCodeFile = CodeFile(path, module, virtualFile.name, virtualFile.length, mutableListOf())
        this[path] = newCodeFile
        newCodeFile
    }
}

private fun PsiFile.isBad(config: GraphConfig): Boolean {
    val path = virtualFile.path
    val className = virtualFile.name
    val module = ModuleUtil.findModuleForFile(this)?.name ?: ""

    if (path.contains("\$USER_HOME\$")) return true
    config.filteredClasses.forEach { if (path.endsWith(it)) return true }

    if (config.filteredModules.contains(module)) return true
    if (className.startsWith("Whetstone")) return true
    if (className.startsWith("Dagger")) return true

    return false
}