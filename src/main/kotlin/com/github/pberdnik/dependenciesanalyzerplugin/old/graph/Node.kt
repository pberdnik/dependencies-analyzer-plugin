package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile
import com.github.pberdnik.dependenciesanalyzerplugin.old.platform.File
import com.github.pberdnik.dependenciesanalyzerplugin.old.util.TextLines
import kotlinx.coroutines.CoroutineScope

class Node(val codeFile: CodeFile) : File {
    val id: String = codeFile.path
    val path: String = codeFile.path

    val dependencies = mutableSetOf<Node>()
    val backwardDependencies = mutableSetOf<Node>()

    var _color: Color = Color.WHITE
    var cycle: Cycle? = null
    var depth = -1

    var onlyRed: Node? = null
    override val name: String
        get() = codeFile.className
    override val isDirectory = false
    override val children: List<File> = emptyList()
    override val hasChildren = false
    override var redSize: Int
        get() = if (_color == Color.RED) codeFile.size.toInt() else 0
        set(_) {}
    override var greenSize: Int
        get() = if (_color == Color.GREEN) codeFile.size.toInt() else 0
        set(_) {}
    override var yellowSize: Int
        get() = if (_color == Color.RED && onlyRed != null) codeFile.size.toInt() else 0
        set(_) {}
    override val color: Color
        get() = _color

    override fun readLines(scope: CoroutineScope): TextLines {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is Node && this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
