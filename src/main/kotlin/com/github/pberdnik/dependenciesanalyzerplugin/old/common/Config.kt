package com.github.pberdnik.dependenciesanalyzerplugin.old.common

const val ARE_OTHER_PACKAGES_RED = false

class Config {
    var configJson = ConfigJson()

    val graphJsonPath get() = configJson.graphJsonPath
    val graphXmlPath get() = configJson.graphXmlPath

    val filteredModules get() = configJson.filteredModules
    val filteredClasses get() = configJson.filteredClasses

    val greenModules get() = configJson.greenModules
    val greenClasses get() = configJson.greenClasses
    val redClasses get() = configJson.redClasses
}