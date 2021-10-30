/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.github.pberdnik.dependenciesanalyzerplugin.panel;

import com.github.pberdnik.dependenciesanalyzerplugin.actions.SaveAnalysisResultActionExtensionsKt;
import com.github.pberdnik.dependenciesanalyzerplugin.toolwindow.FileDependenciesToolWindow;
import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.PerformAnalysisInBackgroundOption;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.packageDependencies.MyDependenciesBuilder;
import com.intellij.packageDependencies.actions.MyForwardDependenciesBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

public class FileAnalyzeDependenciesHandler {
  @NotNull
  protected final Project myProject;
  private final List<? extends AnalysisScope> myScopes;
  private final Set<PsiFile> myExcluded;
  private final int myTransitiveBorder;

  public FileAnalyzeDependenciesHandler(@NotNull Project project, List<? extends AnalysisScope> scopes, int transitiveBorder, Set<PsiFile> excluded) {
    myProject = project;
    myScopes = scopes;
    myTransitiveBorder = transitiveBorder;
    myExcluded = excluded;
  }

  public FileAnalyzeDependenciesHandler(final Project project, final AnalysisScope scope, final int transitiveBorder) {
    this(project, Collections.singletonList(scope), transitiveBorder, new HashSet<>());
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
          FileAnalyzeDependenciesHandler.this.onSuccess(builders);
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
          FileAnalyzeDependenciesHandler.this.onSuccess(builders);
        }
      };
    }
    ProgressManager.getInstance().run(task);
  }

  private boolean canStartInBackground() {
    return true;
  }

  private boolean shouldShowDependenciesPanel(List<? extends MyDependenciesBuilder> builders) {
    return true;
  }

  private MyDependenciesBuilder createDependenciesBuilder(AnalysisScope scope) {
    return new MyForwardDependenciesBuilder(myProject, scope, myTransitiveBorder);
  }

  private String getPanelDisplayName(final AnalysisScope scope) {
    return CodeInsightBundle.message("package.dependencies.toolwindow.title", scope.getDisplayName());
  }

  private String getProgressTitle() {
    return CodeInsightBundle.message("package.dependencies.progress.title");
  }

  private void perform(List<MyDependenciesBuilder> builders, @NotNull ProgressIndicator indicator) {
    try {
      for (AnalysisScope scope : myScopes) {
        builders.add(createDependenciesBuilder(scope));
      }
      for (MyDependenciesBuilder builder : builders) {
        builder.analyze();
      }
      Map<PsiFile, Set<PsiFile>> myDependencies = new HashMap<>();
      for (MyDependenciesBuilder builder : builders) {
        myDependencies.putAll(builder.getDependencies());
      }
      SaveAnalysisResultActionExtensionsKt.performAction(myDependencies, myProject);
    } catch (IndexNotReadyException e) {
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
        FileDependenciesPanel panel = new FileDependenciesPanel(myProject, builders, myExcluded);
        Content content = ContentFactory.SERVICE.getInstance().createContent(panel, displayName, false);
        content.setDisposer(panel);
        panel.setContent(content);
        FileDependenciesToolWindow.Companion.getInstance(myProject).addContent(content);
      }
    });
    ProjectView.getInstance(myProject).refresh();
  }

  private @NlsContexts.TabTitle String getPanelDisplayName(List<? extends MyDependenciesBuilder> builders) {
    return getPanelDisplayName(builders.get(0).getScope());
  }
}