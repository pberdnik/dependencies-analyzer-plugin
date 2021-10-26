package com.github.pberdnik.dependenciesanalyzerplugin.views

sealed class NodeView

class DirNodeView(
    val greenSize: Int,
    val redSize: Int,
    val yellowSize: Int
) : NodeView()

class FileNodeView(
    val color: FileNodeViewColor,
    val size: Long,
    val depth: Int,
    val isCycle: Boolean
) : NodeView()


val nodeViews = mutableMapOf<String, NodeView>(
    "/home/pberdnik/work/countonme-starter/app/src/main/java/com/raywenderlich/countonme" to DirNodeView(300, 200, 100),
    "/home/pberdnik/work/countonme-starter/app/src/main/java/com/raywenderlich/countonme/sub" to DirNodeView(300, 0, 0),
    "/home/pberdnik/work/countonme-starter/app/src/main/java/com/raywenderlich/countonme/sub/SD.java" to FileNodeView(FileNodeViewColor.GREEN, 100, 0, false),
    "/home/pberdnik/work/countonme-starter/app/src/main/java/com/raywenderlich/countonme/sub/SDKotlin.kt" to FileNodeView(FileNodeViewColor.GRAY, 200, 1, false),
    "/home/pberdnik/work/countonme-starter/app/src/main/java/com/raywenderlich/countonme/A.kt" to FileNodeView(FileNodeViewColor.RED, 50, 0, false),
    "/home/pberdnik/work/countonme-starter/app/src/main/java/com/raywenderlich/countonme/B.kt" to FileNodeView(FileNodeViewColor.RED, 150, 0, true),
    "/home/pberdnik/work/countonme-starter/app/src/main/java/com/raywenderlich/countonme/JC.java" to FileNodeView(FileNodeViewColor.YELLOW, 150, 0, false),
)