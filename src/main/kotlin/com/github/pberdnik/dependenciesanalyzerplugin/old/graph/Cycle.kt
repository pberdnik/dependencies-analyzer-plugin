package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.className


class Cycle {
    val nodes = mutableListOf<Node>()

    fun add(node: Node) {
        if (nodes.isNotEmpty() && !node.dependencies.contains(nodes.last())) {
            error("Adding node [$node] has no dependency on last node in cycle: [${nodes.last()}]")
        }
        nodes.add(node)
    }

    override fun toString(): String {
        return nodes.joinToString("\n        <- ") { ".(${it.codeFile.className}:1)" }
    }
}