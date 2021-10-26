package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.className
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.module

fun asDependencyGraph(codeFiles: MutableCollection<CodeFile>, config: GraphConfig): DependencyGraph {
    val graph = DependencyGraph()
    codeFiles.forEach { codeFile ->
        if (!codeFile.isBad(config)) {
            codeFile.dependencies.forEach { dependentCodeFilePath ->
                val dependentCodeFile = codeFiles.find { it.path == dependentCodeFilePath }!!
                if (!dependentCodeFile.isBad(config)) {
                    graph.add(codeFile, dependentCodeFile)
                }
            }
        }
    }
    return graph
}

fun CodeFile.isBad(config: GraphConfig): Boolean {
    val path = path
    if (path.contains("\$USER_HOME\$")) return true
    config.filteredClasses.forEach { if (path.endsWith(it)) return true }

    if (config.filteredModules.contains(module)) return true
    if (className.startsWith("Whetstone")) return true
    if (className.startsWith("Dagger")) return true

    return false
}