package com.github.pberdnik.dependenciesanalyzerplugin.storage

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "GraphConfig", storages = [Storage("graphConfig.xml")])
class GraphConfigStorageService(val project: Project) : PersistentStateComponent<GraphConfigState> {

    private val state = GraphConfigState()

    override fun getState() = state

    override fun loadState(state: GraphConfigState) = XmlSerializerUtil.copyBean<GraphConfigState>(state, this.state)
}