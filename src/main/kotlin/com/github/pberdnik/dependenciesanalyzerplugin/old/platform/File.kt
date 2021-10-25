@file:Suppress("NewApi")

package com.github.pberdnik.dependenciesanalyzerplugin.old.platform

import com.github.pberdnik.dependenciesanalyzerplugin.old.graph.Color
import com.github.pberdnik.dependenciesanalyzerplugin.old.util.TextLines
import kotlinx.coroutines.CoroutineScope

interface File {
    val name: String
    val isDirectory: Boolean
    val children: List<File>
    val hasChildren: Boolean

    var redSize: Int
    var greenSize: Int
    var yellowSize: Int
    val color: Color

    fun readLines(scope: CoroutineScope): TextLines
}
