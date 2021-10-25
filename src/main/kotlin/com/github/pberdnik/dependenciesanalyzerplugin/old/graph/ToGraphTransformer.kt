package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.className
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.module

fun asDependencyGraph(codeFiles: MutableCollection<CodeFile>, config: GraphConfig): DependencyGraph {
    val graph = DependencyGraph()
    codeFiles.forEach { codeFile ->
        if (!isBad(codeFile, config)) {
            codeFile.dependencies.forEach { dependentCodeFilePath ->
                val dependentCodeFile = codeFiles.find { it.path == dependentCodeFilePath }!!
                if (!isBad(dependentCodeFile, config)) {
                    graph.add(codeFile, dependentCodeFile)
                }
            }
        }
    }
    return graph
}

private fun isBad(codeFile: CodeFile, config: GraphConfig): Boolean {
    val path = codeFile.path
    if (path.contains("\$USER_HOME\$")) return true
    config.filteredClasses.forEach { if (path.endsWith(it)) return true }

    if (config.filteredModules.contains(codeFile.module)) return true
    if (codeFile.className.startsWith("Whetstone")) return true
    if (codeFile.className.startsWith("Dagger")) return true

    return false
}