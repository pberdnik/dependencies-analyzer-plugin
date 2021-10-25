package com.github.pberdnik.dependenciesanalyzerplugin.old.common

import com.google.gson.annotations.Expose

class ConfigJson(
    @Expose val graphJsonPath: String = "",
    @Expose val graphXmlPath: String = "",
    @Expose val filteredModules: String = "",
    @Expose val filteredClasses: String = "",
    @Expose val greenModules: String = "",
    @Expose val greenClasses: String = "",
    @Expose val redClasses: String = ""
) {
    constructor(config: Config) : this(
        graphJsonPath = config.graphJsonPath,
        graphXmlPath = config.graphXmlPath,
        filteredModules = config.filteredModules,
        filteredClasses = config.filteredClasses,
        greenModules = config.greenModules,
        greenClasses = config.greenClasses,
        redClasses = config.redClasses
    )
}