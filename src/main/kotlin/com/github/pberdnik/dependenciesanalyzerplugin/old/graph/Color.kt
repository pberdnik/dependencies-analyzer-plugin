package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

enum class Color {
    WHITE, // not processed

    // for DFS
    GRAY, // visiting
    BLACK, // visited

    // for moveability analysis
    GREEN, // can be easily be moved to module
    RED // hard to move to module
}