package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile

class Node(val codeFile: CodeFile) {
    val id: String = codeFile.path
    val path: String = codeFile.path

    val dependencies = mutableSetOf<Node>()
    val backwardDependencies = mutableSetOf<Node>()

    var _color: Color = Color.WHITE
    var cycle: Cycle? = null
    var depth = -1

    var onlyRed: Node? = null
    val color: Color
        get() = _color

    override fun equals(other: Any?): Boolean {
        return other != null && other is Node && this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
