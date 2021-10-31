package com.github.pberdnik.dependenciesanalyzerplugin.storage;

import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class GraphConfigState implements Serializable {
    @Tag("greenModules")
    @XCollection
    @Property(alwaysWrite = true)
    public Set<String> greenModules = new HashSet<>();

    GraphConfigState() {
    }
}
