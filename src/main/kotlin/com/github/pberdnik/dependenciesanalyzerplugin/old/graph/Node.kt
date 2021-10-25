package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.className
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.libBroPackage
import com.github.pberdnik.dependenciesanalyzerplugin.old.file.module
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
        get() = if (_color == Color.RED) codeFile.size else 0
        set(_) {}
    override var greenSize: Int
        get() = if (_color == Color.GREEN) codeFile.size else 0
        set(_) {}
    override var yellowSize: Int
        get() = if (_color == Color.RED && onlyRed != null) codeFile.size else 0
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

    override fun toString(): String {
        return this.asClass()
    }
}

private fun Node.asClass() = if (_color == Color.GREEN) this.asGreenClass() else if (_color == Color.RED) this.asRedClass() + (if (this.cycle != null) "\n CYCLE \n$cycle" else "") else "COLOR UNKNOWN .(${codeFile.className}:1)"
private fun Node.asDependency() = if (_color == Color.GREEN) this.asGreenDependency() else if (_color == Color.RED) this.asRedDependency() else "COLOR UNKNOWN .(${codeFile.className}:1)"
private fun Node.asGreenDependency() = "    GREEN <$depth> ${if (codeFile.module != "lib-bro") codeFile.module else "lib-bro." + codeFile.libBroPackage}.(${codeFile.className}:1)"
private fun Node.asGreenClass() = "${if (codeFile.module != "lib-bro") codeFile.module else "lib-bro." + codeFile.libBroPackage}.(${codeFile.className}:1) GREEN <$depth>"
private fun Node.asRedDependency() = "    RED <$depth>[${backwardDependencies.size}] ${if (codeFile.module != "lib-bro") codeFile.module else "lib-bro." + codeFile.libBroPackage}.(${codeFile.className}:1)"
private fun Node.asRedClass() = "${if (codeFile.module != "lib-bro") codeFile.module else "lib-bro." + codeFile.libBroPackage}.(${codeFile.className}:1) RED <$depth>[${backwardDependencies.size}]"
