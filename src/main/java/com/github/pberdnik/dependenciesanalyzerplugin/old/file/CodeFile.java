package com.github.pberdnik.dependenciesanalyzerplugin.old.file;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Tag("file")
public class CodeFile implements Serializable {
    @Attribute("path")
    @Property(alwaysWrite = true)
    public String path = "";

    @Attribute("module")
    @Property(alwaysWrite = true)
    public String module = "";

    @Attribute("className")
    @Property(alwaysWrite = true)
    public String className = "";

    @Attribute("size")
    public long size = 0;

    @Tag("dependencies")
    @XCollection(elementName = "file", valueAttributeName = "path")
    public List<String> dependencies;

    public CodeFile() {
    }

    public CodeFile(String path) {
        this.path = path;
    }

    public CodeFile(String path, String module, String className, long size, List<String> dependencies) {
        this.path = path;
        this.module = module;
        this.className = className;
        this.size = size;
        this.dependencies = dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeFile codeFile = (CodeFile) o;
        return path.equals(codeFile.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
