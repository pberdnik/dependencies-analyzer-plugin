package com.github.pberdnik.dependenciesanalyzerplugin.storage

import com.github.pberdnik.dependenciesanalyzerplugin.old.common.Config
import com.github.pberdnik.dependenciesanalyzerplugin.old.graph.DependencyGraph
import com.github.pberdnik.dependenciesanalyzerplugin.old.graph.GraphConfig
import com.github.pberdnik.dependenciesanalyzerplugin.old.graph.asDependencyGraph
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "DependenciesGraph", storages = [Storage("dependenciesInfo.xml")])
class GraphStorageService(val project: Project) : PersistentStateComponent<GraphState> {
    private val state = GraphState()
    var dependencyGraph = DependencyGraph()
        private set
    val graphConfig = GraphConfig(Config())

    override fun getState(): GraphState {
        return state
    }

    override fun loadState(state: GraphState) {
        XmlSerializerUtil.copyBean<GraphState>(state, this.state)
    }

    fun analyze() {
        val graphConfig = GraphConfig(Config())
        dependencyGraph = asDependencyGraph(state.codeFiles, graphConfig)
        dependencyGraph.process(graphConfig)
    }

    companion object {
        fun getInstance(project: Project): GraphStorageService {
            return ServiceManager.getService(project, GraphStorageService::class.java)
        }
    }
}