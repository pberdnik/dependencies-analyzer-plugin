package com.github.pberdnik.dependenciesanalyzerplugin.old.graph

import com.github.pberdnik.dependenciesanalyzerplugin.old.common.Config

class GraphConfig(val config: Config) {
    var filteredModules = mapFromString(config.filteredModules)
    var filteredClasses = mapFromString(config.filteredClasses)

    var greenModules = mapFromString(config.greenModules)
    var greenClasses = mapFromString(config.greenClasses)
    var redClasses = mapFromString(config.redClasses)

    private fun mapFromString(str: String): MutableSet<String> {
        return str.lines().filter { it.isNotBlank() }.toMutableSet()
    }
}