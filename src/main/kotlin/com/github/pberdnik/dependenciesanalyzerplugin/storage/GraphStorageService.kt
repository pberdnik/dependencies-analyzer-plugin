package com.github.pberdnik.dependenciesanalyzerplugin.storage

import com.github.pberdnik.dependenciesanalyzerplugin.old.graph.*
import com.github.pberdnik.dependenciesanalyzerplugin.views.DirNodeView
import com.github.pberdnik.dependenciesanalyzerplugin.views.FileNodeView
import com.github.pberdnik.dependenciesanalyzerplugin.views.FileNodeViewColor
import com.github.pberdnik.dependenciesanalyzerplugin.views.NodeView
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NonNls
import java.nio.file.Paths

@State(name = "DependenciesGraph", storages = [Storage("dependenciesInfo.xml")])
class GraphStorageService(val project: Project) : PersistentStateComponent<GraphState> {
    private val LOG = Logger.getInstance(GraphStorageService::class.java)

    private val state = GraphState()
    var dependencyGraph = DependencyGraph()
        private set
    val graphConfig = GraphConfig(project)
    var nodeViews = mutableMapOf<String, NodeView>()
    val virtualFileManager = VirtualFileManager.getInstance()

    override fun getState() = state

    override fun loadState(state: GraphState) = XmlSerializerUtil.copyBean<GraphState>(state, this.state)

    fun analyze() {
        dependencyGraph = asDependencyGraph(state.codeFiles, graphConfig)
        dependencyGraph.process(graphConfig)
        val projectDir = project.guessProjectDir()
        nodeViews.clear()
        dependencyGraph.nodes.forEach { (_, node) ->
            val virtualFile = virtualFileManager.findFileByNioPath(Paths.get(node.path))
            if (virtualFile == null) {
                LOG.error("Can't find virtual file for path: ${node.path}")
                return@forEach
            }
            val path = virtualFile.path
            val fileNodeView = FileNodeView(node.asNodeViewColor(), node.codeFile.size, node.depth, node.cycle != null)
            if (nodeViews.containsKey(path)) {
                LOG.error("nodeViews already contains path [$path] with value: ${nodeViews[path]}")
            }
            nodeViews[path] = fileNodeView
            var parent = virtualFile.parent
            while (parent != null && parent != projectDir) {
                val dirNodeView = nodeViews[parent.path] as? DirNodeView ?: run {
                    val newDirNodeView = DirNodeView()
                    nodeViews[parent.path] = newDirNodeView
                    newDirNodeView
                }
                when (fileNodeView.color) {
                    FileNodeViewColor.GREEN -> dirNodeView.greenSize += fileNodeView.size
                    FileNodeViewColor.RED -> dirNodeView.redSize += fileNodeView.size
                    FileNodeViewColor.YELLOW -> dirNodeView.yellowSize += fileNodeView.size
                    else -> Unit
                }
                parent = parent.parent
            }
        }
    }

    fun getForwardDepsForPath(path: @NonNls String): List<VirtualFile> {
        val node = dependencyGraph.nodes[path] ?: return mutableListOf()
        return node.dependencies.mapNotNull { dep -> virtualFileManager.findFileByNioPath(Paths.get(dep.path)) }
    }

    fun getBackwardDepsForPath(path: @NonNls String): List<VirtualFile> {
        val node = dependencyGraph.nodes[path] ?: return mutableListOf()
        return node.backwardDependencies.mapNotNull { dep -> virtualFileManager.findFileByNioPath(Paths.get(dep.path)) }
    }

    fun getCycleDepsForPath(path: @NonNls String): List<VirtualFile> {
        val node = dependencyGraph.nodes[path] ?: return mutableListOf()
        return node.cycle?.nodes?.mapNotNull { dep -> virtualFileManager.findFileByNioPath(Paths.get(dep.path)) } ?: mutableListOf()
    }

    companion object {
        fun getInstance(project: Project): GraphStorageService {
            return ServiceManager.getService(project, GraphStorageService::class.java)
        }
    }
}

private fun Node.asNodeViewColor() = when {
    color == Color.GREEN -> FileNodeViewColor.GREEN
    color == Color.RED -> if (onlyRed != null) FileNodeViewColor.YELLOW else FileNodeViewColor.RED
    else -> FileNodeViewColor.GRAY
}
