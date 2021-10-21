/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.packageDependencies.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.PerformAnalysisInBackgroundOption;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.packageDependencies.DependenciesToolWindow;
import com.intellij.packageDependencies.MyDependenciesBuilder;
import com.intellij.packageDependencies.ui.DependenciesPanel;
import com.intellij.packageDependencies.ui.MyDependenciesPanel;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class MyDependenciesHandlerBase {
  @NotNull
  protected final Project myProject;
  private final List<? extends AnalysisScope> myScopes;
  private final Set<PsiFile> myExcluded;

  public MyDependenciesHandlerBase(@NotNull Project project, final List<? extends AnalysisScope> scopes, Set<PsiFile> excluded) {
    myScopes = scopes;
    myExcluded = excluded;
    myProject = project;
  }

  public void analyze() {
    final List<MyDependenciesBuilder> builders = new ArrayList<>();

    final Task task;
    if (canStartInBackground()) {
      task = new Task.Backgroundable(myProject, getProgressTitle(), true, new PerformAnalysisInBackgroundOption(myProject)) {
        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
          indicator.setIndeterminate(false);
          perform(builders, indicator);
        }

        @Override
        public void onSuccess() {
          MyDependenciesHandlerBase.this.onSuccess(builders);
        }
      };
    } else {
      task = new Task.Modal(myProject, getProgressTitle(), true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          indicator.setIndeterminate(false);
          perform(builders, indicator);
        }

        @Override
        public void onSuccess() {
          MyDependenciesHandlerBase.this.onSuccess(builders);
        }
      };
    }
    ProgressManager.getInstance().run(task);
  }

  protected boolean canStartInBackground() {
    return true;
  }

  protected boolean shouldShowDependenciesPanel(List<? extends MyDependenciesBuilder> builders) {
    return true;
  }

  protected abstract @NlsContexts.ProgressTitle String getProgressTitle();

  protected abstract @NlsContexts.TabTitle String getPanelDisplayName(AnalysisScope scope);

  protected abstract MyDependenciesBuilder createDependenciesBuilder(AnalysisScope scope);

  private void perform(List<MyDependenciesBuilder> builders, @NotNull ProgressIndicator indicator) {
    try {
      PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
      for (AnalysisScope scope : myScopes) {
        builders.add(createDependenciesBuilder(scope));
      }
      for (MyDependenciesBuilder builder : builders) {
        builder.analyze();
      }
      snapshot.logResponsivenessSinceCreation("Dependency analysis");
    }
    catch (IndexNotReadyException e) {
      DumbService.getInstance(myProject).showDumbModeNotification(
        CodeInsightBundle.message("analyze.dependencies.not.available.notification.indexing"));
      throw new ProcessCanceledException();
    }
  }

  private void onSuccess(final List<MyDependenciesBuilder> builders) {
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(() -> {
      if (shouldShowDependenciesPanel(builders)) {
        final String displayName = getPanelDisplayName(builders);
        MyDependenciesPanel panel = new MyDependenciesPanel(myProject, builders, myExcluded);
        Content content = ContentFactory.SERVICE.getInstance().createContent(panel, displayName, false);
        content.setDisposer(panel);
        panel.setContent(content);
        DependenciesToolWindow.getInstance(myProject).addContent(content);
      }
    });
  }

  protected @NlsContexts.TabTitle String getPanelDisplayName(List<? extends MyDependenciesBuilder> builders) {
    return getPanelDisplayName(builders.get(0).getScope());
  }
}
