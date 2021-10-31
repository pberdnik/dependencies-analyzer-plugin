package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphConfigStorageService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

class GraphConfig(
    val project: Project
) {
    val projectDir: String = project.guessProjectDir()?.path ?: ""

    val graphConfigState = GraphConfigStorageService.getInstance(project).state

    var filteredModules = mutableSetOf<String>()
    var filteredClasses = mutableSetOf<String>()

    val greenModules get() = graphConfigState.greenModules
    var greenClasses = mutableSetOf<String>()
    var redClasses = mutableSetOf<String>()
}