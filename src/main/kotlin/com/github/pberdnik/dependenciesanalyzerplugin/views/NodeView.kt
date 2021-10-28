package com.github.pberdnik.dependenciesanalyzerplugin.views

sealed class NodeView

class DirNodeView(
    var greenSize: Long = 0,
    var redSize: Long = 0,
    var yellowSize: Long = 0
) : NodeView()

class FileNodeView(
    val color: FileNodeViewColor,
    val size: Long,
    val depth: Int,
    val isCycle: Boolean
) : NodeView()

enum class FileNodeViewColor {
    GREEN, YELLOW, RED, GRAY
}