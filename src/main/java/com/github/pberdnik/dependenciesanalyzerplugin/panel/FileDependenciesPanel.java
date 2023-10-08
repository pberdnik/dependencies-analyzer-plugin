// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.pberdnik.dependenciesanalyzerplugin.panel;

import com.github.pberdnik.dependenciesanalyzerplugin.storage.GraphStorageService;
import com.github.pberdnik.dependenciesanalyzerplugin.toolwindow.FileDependenciesToolWindow;
import com.github.pberdnik.dependenciesanalyzerplugin.views.FileNodeView;
import com.github.pberdnik.dependenciesanalyzerplugin.views.FileNodeViewColor;
import com.github.pberdnik.dependenciesanalyzerplugin.views.NodeView;
import com.intellij.CommonBundle;
import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.PerformAnalysisInBackgroundOption;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.FlattenModulesToggleAction;
import com.intellij.idea.ActionsBundle;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependencyRule;
import com.intellij.packageDependencies.DependencyUISettings;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.packageDependencies.MyDependenciesBuilder;
import com.intellij.packageDependencies.actions.MyBackwardDependenciesBuilder;
import com.intellij.packageDependencies.actions.MyForwardDependenciesBuilder;
import com.intellij.packageDependencies.ui.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.PlatformIcons;
import com.intellij.util.Processor;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

public final class FileDependenciesPanel extends JPanel implements Disposable, DataProvider {
  private final Map<PsiFile, Set<PsiFile>> myDependencies;
  private Map<VirtualFile, Map<DependencyRule, Set<PsiFile>>> myIllegalDependencies;
  private final MyTree myLeftTree = new MyTree();
  private final MyTree myRightTree = new MyTree();

  private static final Set<PsiFile> EMPTY_FILE_SET = new HashSet<>(0);
  private final TreeExpansionMonitor myRightTreeExpansionMonitor;

  private final Marker myRightTreeMarker;
  private Set<VirtualFile> myIllegalsInRightTree = new HashSet<>();

  private final Project myProject;
  private final List<MyDependenciesBuilder> myBuilders;
  private final Set<PsiFile> myExcluded;
  private Content myContent;
  private final DependenciesPanel.DependencyPanelSettings mySettings = new DependenciesPanel.DependencyPanelSettings();
  private static final Logger LOG = Logger.getInstance(FileDependenciesPanel.class);

  private final AnalysisScope myScopeOfInterest;
  private final int myTransitiveBorder;
  private final GraphStorageService mGraphStorageService;

  private PsiFile mSelectedPsiFile;

  public FileDependenciesPanel(Project project, final List<MyDependenciesBuilder> builders, final Set<PsiFile> excluded) {
    super(new BorderLayout());
    myBuilders = builders;
    myExcluded = excluded;
    final MyDependenciesBuilder main = myBuilders.get(0);
    myScopeOfInterest = main instanceof MyBackwardDependenciesBuilder ? ((MyBackwardDependenciesBuilder) main).getScopeOfInterest() : null;
    myTransitiveBorder = main instanceof MyForwardDependenciesBuilder ? ((MyForwardDependenciesBuilder) main).getTransitiveBorder() : 0;
    myDependencies = new HashMap<>();
    myIllegalDependencies = new HashMap<>();
    for (MyDependenciesBuilder builder : builders) {
      myDependencies.putAll(builder.getDependencies());
      putAllDependencies(builder);
    }
    exclude(excluded);
    myProject = project;
    mGraphStorageService = GraphStorageService.Companion.getInstance(project);

    add(ScrollPaneFactory.createScrollPane(myRightTree), BorderLayout.CENTER);
    add(createToolbar(), BorderLayout.NORTH);

    myRightTreeExpansionMonitor = PackageTreeExpansionMonitor.install(myRightTree, myProject);

    myRightTreeMarker = new Marker() {
      @Override
      public boolean isMarked(@NotNull VirtualFile file) {
        return myIllegalsInRightTree.contains(file);
      }
    };

    updateRightTreeModel();

    MessageBusConnection connection = myProject.getMessageBus().connect(myProject);
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
        mSelectedPsiFile = PsiManager.getInstance(project).findFile(file);
        updateRightTreeModel();
      }

      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        mSelectedPsiFile = PsiManager.getInstance(project).findFile(event.getNewFile());
        updateRightTreeModel();
      }
    });

    initTree(myRightTree, true);

    setEmptyText(mySettings.UI_FILTER_LEGALS);
  }

  private void putAllDependencies(MyDependenciesBuilder builder) {
    final Map<PsiFile, Map<DependencyRule, Set<PsiFile>>> dependencies = builder.getIllegalDependencies();
    for (Map.Entry<PsiFile, Map<DependencyRule, Set<PsiFile>>> entry : dependencies.entrySet()) {
      myIllegalDependencies.put(entry.getKey().getVirtualFile(), entry.getValue());
    }
  }

  private void processDependencies(final Set<? extends PsiFile> searchIn, final Set<? extends PsiFile> searchFor, Processor<? super List<PsiFile>> processor) {
    if (myTransitiveBorder == 0) return;
    Set<PsiFile> initialSearchFor = new HashSet<>(searchFor);
    for (MyDependenciesBuilder builder : myBuilders) {
      for (PsiFile from : searchIn) {
        for (PsiFile to : initialSearchFor) {
          final List<List<PsiFile>> paths = builder.findPaths(from, to);
          paths.sort(Comparator.comparingInt(List::size));
          for (List<PsiFile> path : paths) {
            if (!path.isEmpty()){
              path.add(0, from);
              path.add(to);
              if (!processor.process(path)) return;
            }
          }
        }
      }
    }
  }

  private void exclude(final Set<? extends PsiFile> excluded) {
    for (PsiFile psiFile : excluded) {
      myDependencies.remove(psiFile);
      myIllegalDependencies.remove(psiFile);
    }
  }

  private void traverseToLeaves(final PackageDependenciesNode treeNode, final StringBuffer denyRules, final StringBuffer allowRules) {
    final Enumeration enumeration = treeNode.breadthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      PsiElement childPsiElement = ((PackageDependenciesNode)enumeration.nextElement()).getPsiElement();
      if (myIllegalDependencies.containsKey(childPsiElement)) {
        final Map<DependencyRule, Set<PsiFile>> illegalDeps = myIllegalDependencies.get(childPsiElement);
        for (final DependencyRule rule : illegalDeps.keySet()) {
          if (rule.isDenyRule()) {
            if (denyRules.indexOf(rule.getDisplayText()) == -1) {
              denyRules.append(rule.getDisplayText());
              denyRules.append("\n");
            }
          }
          else {
            if (allowRules.indexOf(rule.getDisplayText()) == -1) {
              allowRules.append(rule.getDisplayText());
              allowRules.append("\n");
            }
          }
        }
      }
    }
  }

  private JComponent createToolbar() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new CloseAction());
    group.add(new FlattenPackagesAction());
    mySettings.UI_SHOW_FILES = true;
    if (ModuleManager.getInstance(myProject).getModules().length > 1) {
      mySettings.UI_SHOW_MODULES = true;
      group.add(createFlattenModulesAction());
      if (ModuleManager.getInstance(myProject).hasModuleGroups()) {
        mySettings.UI_SHOW_MODULE_GROUPS = true;
      }
    }
    group.add(new GroupByScopeTypeAction());
    //group.add(new GroupByFilesAction());
    group.add(new FilterLegalsAction());
    group.add(new MarkAsIllegalAction());
    group.add(new ChooseScopeTypeAction());
    group.add(new EditDependencyRulesAction());

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true);
    return toolbar.getComponent();
  }

  @NotNull
  private FlattenModulesToggleAction createFlattenModulesAction() {
    return new FlattenModulesToggleAction(myProject, () -> mySettings.UI_SHOW_MODULES, () -> !mySettings.UI_SHOW_MODULE_GROUPS, (value) -> {
      DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS = !value;
      mySettings.UI_SHOW_MODULE_GROUPS = !value;
      rebuild();
    });
  }

  private void rebuild() {
    myIllegalDependencies = new HashMap<>();
    for (MyDependenciesBuilder builder : myBuilders) {
      putAllDependencies(builder);
    }
    updateRightTreeModel();
  }

  private void initTree(final MyTree tree, boolean isRightTree) {
    tree.setCellRenderer(new MyTreeCellRenderer(mGraphStorageService));
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);

    TreeUtil.installActions(tree);
    SmartExpander.installOn(tree);
    EditSourceOnDoubleClickHandler.install(tree);
    new TreeSpeedSearch(tree);

    PopupHandler.installUnknownPopupHandler(tree, createTreePopupActions(isRightTree));
  }

  private void updateRightTreeModel() {
    Set<VirtualFile> forwardDeps = new HashSet<>();
    Set<VirtualFile> backwardDeps = new HashSet<>();
    Set<VirtualFile> cycleDeps = new HashSet<>();
    Set<PsiFile> scope = new HashSet<>();
    Set<VirtualFile> vScope = new HashSet<>();
    if (mSelectedPsiFile != null) {
      scope.add(mSelectedPsiFile);
      vScope.add(mSelectedPsiFile.getVirtualFile());
    } else {
      return;
    }
    myIllegalsInRightTree = new HashSet<>();
    for (PsiFile psiFile : scope) {
      Map<DependencyRule, Set<PsiFile>> illegalDeps = myIllegalDependencies.get(psiFile.getVirtualFile());
      if (illegalDeps != null) {
        for (final DependencyRule rule : illegalDeps.keySet()) {
          final Set<PsiFile> files = illegalDeps.get(rule);
          for (PsiFile file : files) {
            myIllegalsInRightTree.add(file.getVirtualFile());
          }
        }
      }
      final List<VirtualFile> forwardFiles = mGraphStorageService.getForwardDepsForPath(psiFile.getVirtualFile().getPath()); //
      for (VirtualFile file : forwardFiles) {
        if (file != null && file.isValid()) {
          forwardDeps.add(file);
        }
      }
      final List<VirtualFile> backwardFiles = mGraphStorageService.getBackwardDepsForPath(psiFile.getVirtualFile().getPath()); //
      for (VirtualFile file : backwardFiles) {
        if (file != null && file.isValid()) {
          backwardDeps.add(file);
        }
      }
      final List<VirtualFile> cycleFiles = mGraphStorageService.getCycleDepsForPath(psiFile.getVirtualFile().getPath()); //
      for (VirtualFile file : cycleFiles) {
        if (file != null && file.isValid()) {
          cycleDeps.add(file);
        }
      }
    }
    forwardDeps.removeAll(vScope);
    backwardDeps.removeAll(vScope);
    myRightTreeExpansionMonitor.freeze();
    myRightTree.setModel(buildTreeModel(forwardDeps, backwardDeps, cycleDeps, myRightTreeMarker));
    myRightTreeExpansionMonitor.restore();
    expandFirstLevel(myRightTree);
  }

  private ActionGroup createTreePopupActions(boolean isRightTree) {
    DefaultActionGroup group = new DefaultActionGroup();
    final ActionManager actionManager = ActionManager.getInstance();
    group.add(actionManager.getAction(IdeActions.ACTION_EDIT_SOURCE));
    group.add(actionManager.getAction(IdeActions.GROUP_VERSION_CONTROLS));

    if (isRightTree) {
      group.add(actionManager.getAction(IdeActions.GROUP_ANALYZE));
      group.add(new AddToScopeAction());
      group.add(new ShowDetailedInformationAction());
    } else {
      group.add(new RemoveFromScopeAction());
    }

    return group;
  }

  private TreeModel buildTreeModel(Set<VirtualFile> forwardDeps, Set<VirtualFile> backwardDeps, Set<VirtualFile> cycleDeps, Marker marker) {
    return MyFileTreeModelBuilder.createTreeModel(myProject, false, forwardDeps, backwardDeps, cycleDeps, marker, mySettings);
  }

  private static void expandFirstLevel(Tree tree) {
    PackageDependenciesNode root = (PackageDependenciesNode)tree.getModel().getRoot();
    int count = root.getChildCount();
    if (count < 10) {
      for (int i = 0; i < count; i++) {
        PackageDependenciesNode child = (PackageDependenciesNode)root.getChildAt(i);
        expandNodeIfNotTooWide(tree, child);
      }
    }
  }

  private static void expandNodeIfNotTooWide(Tree tree, PackageDependenciesNode node) {
    int count = node.getChildCount();
    if (count > 5) return;
    //another level of nesting
    if (count == 1 && node.getChildAt(0).getChildCount() > 5){
      return;
    }
    tree.expandPath(new TreePath(node.getPath()));
  }

  private Set<PsiFile> getSelectedScope(final Tree tree) {
    TreePath[] paths = tree.getSelectionPaths();
    if (paths == null ) return EMPTY_FILE_SET;
    Set<PsiFile> result = new HashSet<>();
    for (TreePath path : paths) {
      PackageDependenciesNode node = (PackageDependenciesNode)path.getLastPathComponent();
      node.fillFiles(result, !mySettings.UI_FLATTEN_PACKAGES);
    }
    return result;
  }

  public void setContent(Content content) {
    myContent = content;
  }

  @Override
  public void dispose() {
    MyFileTreeModelBuilder.clearCaches(myProject);
  }

  @Override
  @Nullable
  @NonNls
  public Object getData(@NotNull @NonNls String dataId) {
    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      final PackageDependenciesNode selectedNode = myRightTree.getSelectedNode();
      if (selectedNode != null) {
        final PsiElement element = selectedNode.getPsiElement();
        return element != null && element.isValid() ? element : null;
      }
    }
    if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return "dependency.viewer.tool.window";
    }
    return null;
  }

  private static class MyTreeCellRenderer extends ColoredTreeCellRenderer {
    private static final SimpleTextAttributes REGULAR_TEXT = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    private static final SimpleTextAttributes GREEN_TEXT = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new JBColor(JBColor.green.darker(), JBColor.green));
    private static final SimpleTextAttributes RED_TEXT = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new JBColor(JBColor.red.darker(), JBColor.red));
    private static final SimpleTextAttributes YELLOW_TEXT = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new JBColor(JBColor.yellow.darker(), JBColor.yellow));
    private static final SimpleTextAttributes GRAY_TEXT = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY);
    GraphStorageService mGraphStorageService;

    MyTreeCellRenderer(GraphStorageService graphStorageService) {
      mGraphStorageService = graphStorageService;
    }
    @Override
    public void customizeCellRenderer(
            @NotNull JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
    ){
      if (!(value instanceof PackageDependenciesNode)) {
        LOG.error("value type should be PackageDependenciesNode but is " + value.getClass() + "; And value is " + Arrays.toString(((DefaultMutableTreeNode) value).getPath()));
        return;
      }
      PackageDependenciesNode node = (PackageDependenciesNode)value;
      if (node.isValid()) {
        setIcon(node.getIcon());
      } else {
        append(UsageViewBundle.message("node.invalid") + " ", SimpleTextAttributes.ERROR_ATTRIBUTES);
      }
      append(node.toString(), node.hasMarked() && !selected ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
      PsiElement psiElement = node.getPsiElement();
      if (psiElement instanceof PsiFile) {
        String path = ((PsiFile) psiElement).getVirtualFile().getPath();
        NodeView nodeView = mGraphStorageService.getNodeViews().get(path);
        if (nodeView instanceof FileNodeView) {
          FileNodeView fileNodeView = (FileNodeView) nodeView;
          SimpleTextAttributes textColor = REGULAR_TEXT;
          FileNodeViewColor fileNodeViewColor = fileNodeView.getColor();
          if (fileNodeViewColor == FileNodeViewColor.GREEN) {
            textColor = GREEN_TEXT;
          } else if (fileNodeViewColor == FileNodeViewColor.RED) {
            textColor = RED_TEXT;
          } else if (fileNodeViewColor == FileNodeViewColor.YELLOW) {
            textColor = YELLOW_TEXT;
          } else if (fileNodeViewColor == FileNodeViewColor.GRAY) {
            textColor = GRAY_TEXT;
          }
          append(" " + fileNodeView.getSize() + " [" + fileNodeView.getDepth() + "]", textColor);
          if (fileNodeView.isCycle()) append(" {C}", RED_TEXT);
        }
      }
    }
  }

  private final class CloseAction extends AnAction implements DumbAware {
    CloseAction() {
      super(CommonBundle.messagePointer("action.close"), CodeInsightBundle.messagePointer("action.close.dependency.description"),
              AllIcons.Actions.Cancel);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      FileDependenciesToolWindow.Companion.getInstance(myProject).closeContent(myContent);
      mySettings.copyToApplicationDependencySettings();
    }
  }

  private final class FlattenPackagesAction extends ToggleAction {
    FlattenPackagesAction() {
      super(CodeInsightBundle.messagePointer("action.flatten.packages"), CodeInsightBundle.messagePointer("action.flatten.packages"),
              PlatformIcons.FLATTEN_PACKAGES_ICON);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return mySettings.UI_FLATTEN_PACKAGES;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      DependencyUISettings.getInstance().UI_FLATTEN_PACKAGES = flag;
      mySettings.UI_FLATTEN_PACKAGES = flag;
      rebuild();
    }
  }

  private final class GroupByScopeTypeAction extends ToggleAction {
    GroupByScopeTypeAction() {
      super(CodeInsightBundle.messagePointer("action.group.by.scope.type"),
              CodeInsightBundle.messagePointer("action.group.by.scope.type.description"), AllIcons.Actions.GroupByTestProduction);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return mySettings.UI_GROUP_BY_SCOPE_TYPE;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      DependencyUISettings.getInstance().UI_GROUP_BY_SCOPE_TYPE = flag;
      mySettings.UI_GROUP_BY_SCOPE_TYPE = flag;
      rebuild();
    }
  }


  private final class FilterLegalsAction extends ToggleAction {
    FilterLegalsAction() {
      super(CodeInsightBundle.messagePointer("action.show.illegals.only"),
              CodeInsightBundle.messagePointer("action.show.illegals.only.description"), AllIcons.General.Filter);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return mySettings.UI_FILTER_LEGALS;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      DependencyUISettings.getInstance().UI_FILTER_LEGALS = flag;
      mySettings.UI_FILTER_LEGALS = flag;
      setEmptyText(flag);
      rebuild();
    }
  }

  private void setEmptyText(boolean flag) {
    final String emptyText = flag ? LangBundle.message("status.text.no.illegal.dependencies.found") : LangBundle.message("status.text.nothing.to.show");
    myLeftTree.getEmptyText().setText(emptyText);
    myRightTree.getEmptyText().setText(emptyText);
  }

  private final class EditDependencyRulesAction extends AnAction {
    EditDependencyRulesAction() {
      super(CodeInsightBundle.messagePointer("action.edit.rules"), CodeInsightBundle.messagePointer("action.edit.rules.description"),
              AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      boolean applied = ShowSettingsUtil.getInstance().editConfigurable(FileDependenciesPanel.this, new DependencyConfigurable(myProject));
      if (applied) {
        rebuild();
      }
    }
  }

  private static class MyTree extends Tree implements DataProvider {
    @Override
    public Object getData(@NotNull String dataId) {
      PackageDependenciesNode node = getSelectedNode();
      if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
        return node;
      }
      if (CommonDataKeys.PSI_ELEMENT.is(dataId) && node != null)  {
        final PsiElement element = node.getPsiElement();
        return element != null && element.isValid() ? element : null;
      }
      return null;
    }

    @Nullable
    public PackageDependenciesNode getSelectedNode() {
      TreePath[] paths = getSelectionPaths();
      if (paths == null || paths.length != 1) return null;
      return (PackageDependenciesNode)paths[0].getLastPathComponent();
    }
  }

  private final class ShowDetailedInformationAction extends AnAction {
    private ShowDetailedInformationAction() {
      super(ActionsBundle.messagePointer("action.ShowDetailedInformationAction.text"));
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      @NonNls final String delim = "&nbsp;-&gt;&nbsp;";
      final StringBuffer buf = new StringBuffer();
      processDependencies(getSelectedScope(myLeftTree), getSelectedScope(myRightTree), path -> {
        if (buf.length() > 0) buf.append("<br>");
        buf.append(StringUtil.join(path, psiFile -> psiFile.getName(), delim));
        return true;
      });
      final JEditorPane pane = new JEditorPane(UIUtil.HTML_MIME, XmlStringUtil.wrapInHtml(buf));
      pane.setForeground(JBColor.foreground());
      pane.setBackground(HintUtil.getInformationColor());
      pane.setOpaque(true);
      final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(pane);
      final Dimension dimension = pane.getPreferredSize();
      scrollPane.setMinimumSize(new Dimension(dimension.width, dimension.height + 20));
      scrollPane.setPreferredSize(new Dimension(dimension.width, dimension.height + 20));
      JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, pane).setTitle(LangBundle.message("popup.title.dependencies"))
              .setMovable(true).createPopup().showInBestPositionFor(e.getDataContext());
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      final boolean[] direct = new boolean[]{true};
      processDependencies(getSelectedScope(myLeftTree), getSelectedScope(myRightTree), path -> {
        direct [0] = false;
        return false;
      });
      e.getPresentation().setEnabled(!direct[0]);
    }
  }

  private final class RemoveFromScopeAction extends AnAction {
    private RemoveFromScopeAction() {
      super(ActionsBundle.messagePointer("action.RemoveFromScopeAction.text"));
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      e.getPresentation().setEnabled(!getSelectedScope(myLeftTree).isEmpty());
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      final Set<PsiFile> selectedScope = getSelectedScope(myLeftTree);
      exclude(selectedScope);
      myExcluded.addAll(selectedScope);
      final TreePath[] paths = myLeftTree.getSelectionPaths();
      assert paths != null;
      for (TreePath path : paths) {
        TreeUtil.removeLastPathComponent(myLeftTree, path);
      }
    }
  }

  private final class AddToScopeAction extends AnAction {
    private AddToScopeAction() {
      super(ActionsBundle.messagePointer("action.AddToScopeAction.text"));
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      e.getPresentation().setEnabled(getScope() != null);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      final AnalysisScope scope = getScope();
      LOG.assertTrue(scope != null);
      final MyDependenciesBuilder builder = new MyForwardDependenciesBuilder(myProject, scope, myTransitiveBorder);
      String message = CodeInsightBundle.message("package.dependencies.progress.title");
      ProgressManager.getInstance().run(new Task.Backgroundable(myProject, message, true, new PerformAnalysisInBackgroundOption(myProject)) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          builder.analyze();
          myBuilders.add(builder);
          myDependencies.putAll(builder.getDependencies());
          putAllDependencies(builder);
          exclude(myExcluded);
          rebuild();
        }
      });
    }

    @Nullable
    private AnalysisScope getScope() {
      final Set<PsiFile> selectedScope = getSelectedScope(myRightTree);
      Set<PsiFile> result = new HashSet<>();
      ((PackageDependenciesNode)myLeftTree.getModel().getRoot()).fillFiles(result, !mySettings.UI_FLATTEN_PACKAGES);
      selectedScope.removeAll(result);
      if (selectedScope.isEmpty()) return null;
      List<VirtualFile> files = new ArrayList<>();
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
      for (PsiFile psiFile : selectedScope) {
        final VirtualFile file = psiFile.getVirtualFile();
        LOG.assertTrue(file != null);
        if (fileIndex.isInContent(file)) {
          files.add(file);
        }
      }
      if (!files.isEmpty()) {
        return new AnalysisScope(myProject, files);
      }
      return null;
    }
  }

  private class SelectInLeftTreeAction extends AnAction {
    SelectInLeftTreeAction() {
      super(CodeInsightBundle.messagePointer("action.select.in.left.tree"),
              CodeInsightBundle.messagePointer("action.select.in.left.tree.description"), null);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      PackageDependenciesNode node = myRightTree.getSelectedNode();
      e.getPresentation().setEnabled(node != null && node.canSelectInLeftTree(myDependencies));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      PackageDependenciesNode node = myRightTree.getSelectedNode();
      if (node != null) {
        PsiElement elt = node.getPsiElement();
        if (elt != null) {
          DependencyUISettings.getInstance().UI_FILTER_LEGALS = false;
          mySettings.UI_FILTER_LEGALS = false;
          selectElementInLeftTree(elt);

        }
      }
    }
  }

  private void selectElementInLeftTree(PsiElement elt) {
    PsiManager manager = PsiManager.getInstance(myProject);

    PackageDependenciesNode root = (PackageDependenciesNode)myLeftTree.getModel().getRoot();
    Enumeration enumeration = root.breadthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      PackageDependenciesNode child = (PackageDependenciesNode)enumeration.nextElement();
      if (manager.areElementsEquivalent(child.getPsiElement(), elt)) {
        myLeftTree.setSelectionPath(new TreePath(((DefaultTreeModel)myLeftTree.getModel()).getPathToRoot(child)));
        break;
      }
    }
  }

  private class MarkAsIllegalAction extends AnAction {
    MarkAsIllegalAction() {
      super(CodeInsightBundle.messagePointer("mark.dependency.illegal.text"),
              CodeInsightBundle.messagePointer("mark.dependency.illegal.text"), AllIcons.Actions.Lightning);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final PackageDependenciesNode leftNode = myLeftTree.getSelectedNode();
      final PackageDependenciesNode rightNode = myRightTree.getSelectedNode();
      if (leftNode != null && rightNode != null) {
        boolean hasDirectDependencies = myTransitiveBorder == 0;
        if (myTransitiveBorder > 0) {
          final Set<PsiFile> searchIn = getSelectedScope(myLeftTree);
          final Set<PsiFile> searchFor = getSelectedScope(myRightTree);
          for (MyDependenciesBuilder builder : myBuilders) {
            if (hasDirectDependencies) break;
            for (PsiFile from : searchIn) {
              if (hasDirectDependencies) break;
              for (PsiFile to : searchFor) {
                if (hasDirectDependencies) break;
                final List<List<PsiFile>> paths = builder.findPaths(from, to);
                for (List<PsiFile> path : paths) {
                  if (path.isEmpty()) {
                    hasDirectDependencies = true;
                    break;
                  }
                }
              }
            }
          }
        }
        final PatternDialectProvider provider = PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE);
        assert provider != null;
        PackageSet leftPackageSet = provider.createPackageSet(leftNode, true);
        if (leftPackageSet == null) {
          leftPackageSet = provider.createPackageSet(leftNode, false);
        }
        LOG.assertTrue(leftPackageSet != null);
        PackageSet rightPackageSet = provider.createPackageSet(rightNode, true);
        if (rightPackageSet == null) {
          rightPackageSet = provider.createPackageSet(rightNode, false);
        }
        LOG.assertTrue(rightPackageSet != null);
        if (hasDirectDependencies) {
          DependencyValidationManager.getInstance(myProject)
                  .addRule(new DependencyRule(new NamedScope.UnnamedScope(leftPackageSet),
                          new NamedScope.UnnamedScope(rightPackageSet), true));
          rebuild();
        } else {
          Messages.showErrorDialog(FileDependenciesPanel.this, CodeInsightBundle
                          .message("analyze.dependencies.unable.to.create.rule.error.message", leftPackageSet.getText(), rightPackageSet.getText()),
                  CodeInsightBundle.message("mark.dependency.illegal.text"));
        }
      }
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      presentation.setEnabled(false);
      final PackageDependenciesNode leftNode = myLeftTree.getSelectedNode();
      final PackageDependenciesNode rightNode = myRightTree.getSelectedNode();
      if (leftNode != null && rightNode != null) {
        final PatternDialectProvider provider = PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE);
        assert provider != null;
        presentation.setEnabled((provider.createPackageSet(leftNode, true) != null || provider.createPackageSet(leftNode, false) != null) &&
                (provider.createPackageSet(rightNode, true) != null || provider.createPackageSet(rightNode, false) != null));
      }
    }
  }

  private final class ChooseScopeTypeAction extends ComboBoxAction {
    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(final JComponent button) {
      final DefaultActionGroup group = new DefaultActionGroup();
      for (final PatternDialectProvider provider : PatternDialectProvider.EP_NAME.getExtensionList()) {
        group.add(new AnAction(provider.getDisplayName()) {
          @Override
          public void actionPerformed(@NotNull final AnActionEvent e) {
            mySettings.SCOPE_TYPE = provider.getShortName();
            DependencyUISettings.getInstance().SCOPE_TYPE = provider.getShortName();
            rebuild();
          }
        });
      }
      return group;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      super.update(e);
      final PatternDialectProvider provider = PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE);
      assert provider != null;
      e.getPresentation().setText(provider.getDisplayName());
      e.getPresentation().setIcon(provider.getIcon());
    }
  }
}
