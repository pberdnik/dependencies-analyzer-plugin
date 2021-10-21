// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.BaseAnalysisAction;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MyAnalyzeDependenciesAction extends BaseAnalysisAction {
  public MyAnalyzeDependenciesAction() {
    super(CodeInsightBundle.messagePointer("action.forward.dependency.analysis"), CodeInsightBundle.messagePointer("action.analysis.noun"));
  }

  @Override
  protected void analyze(@NotNull final Project project, @NotNull AnalysisScope scope) {
    new MyAnalyzeDependenciesHandler(project, scope, 0).analyze();
  }

  @Override
  protected void canceled() {
    super.canceled();
  }

}
