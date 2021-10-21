package com.github.pberdnik.dependenciesanalyzerplugin.storage

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "DependenciesGraph", storages = [Storage("dependenciesGraph.xml")])
class GraphStorageService(val project: Project) : PersistentStateComponent<GraphState> {
    private val state = GraphState()

    override fun getState(): GraphState {
        return state
    }

    override fun loadState(state: GraphState) {
        XmlSerializerUtil.copyBean<GraphState>(state, this.state)
    }

    companion object {
        fun getInstance(project: Project): GraphStorageService {
            return ServiceManager.getService(
                project,
                GraphStorageService::class.java
            )
        }
    }
}