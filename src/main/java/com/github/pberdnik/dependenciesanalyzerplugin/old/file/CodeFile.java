package com.github.pberdnik.dependenciesanalyzerplugin.old.file;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import java.io.Serializable;
import java.util.List;

@Tag("file")
public class CodeFile implements Serializable {
    @Attribute("path")
    @Property(alwaysWrite = true)
    public String path = "";

    @Attribute("size")
    public int size = 0;

    @Tag("dependencies")
    @XCollection(elementName = "file", valueAttributeName = "path")
    public List<String> dependencies;

    public CodeFile() {
    }

    public CodeFile(String path) {
        this.path = path;
    }

    public CodeFile(String path, int size, List<String> dependencies) {
        this.path = path;
        this.size = size;
        this.dependencies = dependencies;
    }
}
