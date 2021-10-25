package com.github.pberdnik.dependenciesanalyzerplugin.storage;

import com.github.pberdnik.dependenciesanalyzerplugin.old.file.CodeFile;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Tag("graph")
public class GraphState implements Serializable {
    @Tag("files")
    @XCollection()
    @Property(alwaysWrite = true)
    public List<CodeFile> codeFiles;

    GraphState() {
        codeFiles = new ArrayList<>();
    }
}
