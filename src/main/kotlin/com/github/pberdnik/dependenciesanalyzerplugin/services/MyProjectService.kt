package com.github.pberdnik.dependenciesanalyzerplugin.services

import com.intellij.openapi.project.Project
import com.github.pberdnik.dependenciesanalyzerplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
