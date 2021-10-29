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

package com.intellij.packageDependencies.ui;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.scopeView.nodes.BasePsiNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

public class MyFileTreeModelBuilder {
    private static final Logger LOG = Logger.getInstance(MyFileTreeModelBuilder.class);

    public static final Key<Integer> FILE_COUNT = Key.create("FILE_COUNT");
    private final ProjectFileIndex myFileIndex;
    private final Project myProject;

    private final boolean myShowModuleGroups;
    private final boolean myShowModules;

    private final boolean myFlattenPackages;
    private final boolean myCompactEmptyMiddlePackages;
    private boolean myShowFiles;
    private final Marker myMarker;
    private final boolean myAddUnmarkedFiles;
    private final PackageDependenciesNode myRoot;
    private final Map<DependencyType, Map<VirtualFile, DirectoryNode>> myModuleDirNodes = new HashMap<>();
    private final Map<DependencyType, Map<Module, ModuleNode>> myModuleNodes = new HashMap<>();
    private final Map<DependencyType, Map<String, ModuleGroupNode>> myModuleGroupNodes = new HashMap<>();
    private final ModuleGrouper myGrouper;
    private GeneralGroupNode myExternalNode;

    private GeneralGroupNode mForwardDependenciesNode;
    private GeneralGroupNode mBackwardDependenciesNode;
    private GeneralGroupNode mCycleDependenciesNode;

    private int myScannedFileCount = 0;
    private int myTotalFileCount = 0;
    private int myMarkedFileCount = 0;

    private JTree myTree;
    protected final VirtualFile myBaseDir;
    protected VirtualFile[] myContentRoots;

    public MyFileTreeModelBuilder(@NotNull Project project, Marker marker, DependenciesPanel.DependencyPanelSettings settings) {
        myProject = project;
        myBaseDir = myProject.getBaseDir();
        myContentRoots = ProjectRootManager.getInstance(myProject).getContentRoots();
        final boolean multiModuleProject = ModuleManager.getInstance(myProject).getModules().length > 1;
        myShowModules = settings.UI_SHOW_MODULES && multiModuleProject;
        myGrouper = ModuleGrouper.instanceFor(project);
        final ProjectViewDirectoryHelper directoryHelper = ProjectViewDirectoryHelper.getInstance(myProject);
        myFlattenPackages = directoryHelper.supportsFlattenPackages() && settings.UI_FLATTEN_PACKAGES;
        myCompactEmptyMiddlePackages = directoryHelper.supportsHideEmptyMiddlePackages() && settings.UI_COMPACT_EMPTY_MIDDLE_PACKAGES;
        myShowFiles = settings.UI_SHOW_FILES;
        myShowModuleGroups = settings.UI_SHOW_MODULE_GROUPS && multiModuleProject;
        myMarker = marker;
        myAddUnmarkedFiles = !settings.UI_FILTER_LEGALS;
        myRoot = new RootNode(myProject);
        myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        myModuleNodes.put(DependencyType.FORWARD, new HashMap<>());
        myModuleNodes.put(DependencyType.BACKWARD, new HashMap<>());
        myModuleNodes.put(DependencyType.CYCLE, new HashMap<>());

        myModuleGroupNodes.put(DependencyType.FORWARD, new HashMap<>());
        myModuleGroupNodes.put(DependencyType.BACKWARD, new HashMap<>());
        myModuleGroupNodes.put(DependencyType.CYCLE, new HashMap<>());

        myModuleDirNodes.put(DependencyType.FORWARD, new HashMap<>());
        myModuleDirNodes.put(DependencyType.BACKWARD, new HashMap<>());
        myModuleDirNodes.put(DependencyType.CYCLE, new HashMap<>());

        mForwardDependenciesNode = new GeneralGroupNode("Forward dependencies", AllIcons.General.ArrowRight, myProject);
        mBackwardDependenciesNode = new GeneralGroupNode("Backward dependencies", AllIcons.General.ArrowLeft, myProject);
        mCycleDependenciesNode = new GeneralGroupNode("Cycle", AllIcons.General.Error, myProject);

        myRoot.add(mForwardDependenciesNode);
        myRoot.add(mBackwardDependenciesNode);
    }

    public void setTree(DnDAwareTree tree) {
        myTree = tree;
    }

    public static synchronized TreeModel createTreeModel(Project project, boolean showProgress, Set<VirtualFile> forwardFiles, Set<VirtualFile> backwardFiles, Set<VirtualFile> cycleFiles, Marker marker, DependenciesPanel.DependencyPanelSettings settings) {
        return new MyFileTreeModelBuilder(project, marker, settings).build(forwardFiles, backwardFiles, cycleFiles, showProgress);
    }

    private void countFiles(Project project) {
        final Integer fileCount = project.getUserData(FILE_COUNT);
        if (fileCount == null) {
            myFileIndex.iterateContent(fileOrDir -> {
                if (!fileOrDir.isDirectory()) {
                    counting();
                }
                return true;
            });
            project.putUserData(FILE_COUNT, myTotalFileCount);
        } else {
            myTotalFileCount = fileCount.intValue();
        }
    }

    public static void clearCaches(Project project) {
        project.putUserData(FILE_COUNT, null);
    }

    private void counting() {
        myTotalFileCount++;
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
            update(indicator, true, -1);
        }
    }

    private static void update(ProgressIndicator indicator, boolean indeterminate, double fraction) {
        if (indicator instanceof PanelProgressIndicator) {
            ((PanelProgressIndicator) indicator).update(getScanningPackagesMessage(), indeterminate, fraction);
        } else {
            if (fraction != -1) {
                indicator.setFraction(fraction);
            }
        }
    }

    private TreeModel build(Set<VirtualFile> forwardFiles, Set<VirtualFile> backwardFiles, Set<VirtualFile> cycleFiles, boolean showProgress) {
        myShowFiles = true;

        if (cycleFiles != null && !cycleFiles.isEmpty()) {
            myRoot.add(mCycleDependenciesNode);
        }
        Runnable buildingRunnable = () -> {
            for (final VirtualFile file : forwardFiles) {
                if (file != null) {
                    ReadAction.run(() -> buildFileNode(file, null, DependencyType.FORWARD));
                }
            }
            for (final VirtualFile file : backwardFiles) {
                if (file != null) {
                    ReadAction.run(() -> buildFileNode(file, null, DependencyType.BACKWARD));
                }
            }
            for (final VirtualFile file : cycleFiles) {
                if (file != null) {
                    ReadAction.run(() -> buildFileNode(file, null, DependencyType.CYCLE));
                }
            }
        };

        if (showProgress) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(buildingRunnable, CodeInsightBundle
                    .message("package.dependencies.build.process.title"), false, myProject);
        } else {
            buildingRunnable.run();
        }

        TreeUtil.sortRecursively(mForwardDependenciesNode, new DependencyNodeComparator());
        return new TreeModel(myRoot, myTotalFileCount, myMarkedFileCount);
    }

    private PackageDependenciesNode buildFileNode(VirtualFile file, PackageDependenciesNode lastParent, DependencyType dependencyType) {
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (file == null || !file.isValid()) return null;
        if (indicator != null) {
            update(indicator, false, ((double) myScannedFileCount++) / myTotalFileCount);
        }


        boolean isMarked = myMarker != null && myMarker.isMarked(file);
        if (isMarked) myMarkedFileCount++;
        if (isMarked || myAddUnmarkedFiles) {
            PackageDependenciesNode dirNode = !myCompactEmptyMiddlePackages && lastParent != null ? lastParent : getFileParentNode(file, dependencyType);

            if (myShowFiles) {
                FileNode fileNode = new FileNode(file, myProject, isMarked);
                dirNode.add(fileNode);
            } else {
                dirNode.addFile(file, isMarked);
            }
            return dirNode;
        }
        return null;
    }

    public @NotNull PackageDependenciesNode getFileParentNode(VirtualFile file, DependencyType dependencyType) {
        LOG.assertTrue(file != null);
        final VirtualFile containingDirectory = file.getParent();
        return getModuleDirNode(containingDirectory, myFileIndex.getModuleForFile(file), null, dependencyType);
    }

    public static PackageDependenciesNode @Nullable [] findNodeForPsiElement(PackageDependenciesNode parent, PsiElement element) {
        final Set<PackageDependenciesNode> result = new HashSet<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            final TreeNode treeNode = parent.getChildAt(i);
            if (treeNode instanceof PackageDependenciesNode) {
                final PackageDependenciesNode node = (PackageDependenciesNode) treeNode;
                if (element instanceof PsiDirectory && node.getPsiElement() == element) {
                    return new PackageDependenciesNode[]{node};
                }
                if (element instanceof PsiFile) {
                    PsiFile psiFile = null;
                    if (node instanceof BasePsiNode) {
                        psiFile = ((BasePsiNode) node).getContainingFile();
                    } else if (node instanceof FileNode) { //non java files
                        psiFile = ((PsiFile) node.getPsiElement());
                    }
                    if (psiFile != null && Comparing.equal(psiFile.getVirtualFile(), ((PsiFile) element).getVirtualFile())) {
                        result.add(node);
                    }
                }
            }
        }
        return result.isEmpty() ? null : result.toArray(new PackageDependenciesNode[0]);
    }

    private PackageDependenciesNode getModuleDirNode(VirtualFile virtualFile, Module module, DirectoryNode childNode, DependencyType dependencyType) {
        if (virtualFile == null) {
            return getModuleNode(module, dependencyType);
        }

        PackageDependenciesNode directoryNode = myModuleDirNodes.get(dependencyType).get(virtualFile);
        if (directoryNode != null) {
            if (myCompactEmptyMiddlePackages) {
                final DirectoryNode nestedNode = ((DirectoryNode) directoryNode).getCompactedDirNode();
                if (nestedNode != null) { //decompact
                    boolean expand = false;
                    if (myTree != null) {
                        expand = !myTree.isCollapsed(new TreePath(directoryNode.getPath()));
                    }
                    DirectoryNode parentWrapper = nestedNode.getWrapper();
                    while (parentWrapper.getWrapper() != null) {
                        parentWrapper = parentWrapper.getWrapper();
                    }
                    for (int i = parentWrapper.getChildCount() - 1; i >= 0; i--) {
                        nestedNode.add((MutableTreeNode) parentWrapper.getChildAt(i));
                    }
                    ((DirectoryNode) directoryNode).setCompactedDirNode(null);
                    parentWrapper.add(nestedNode);
                    nestedNode.removeUpReference();
                    if (myTree != null && expand) {
                        final Runnable expandRunnable = () -> myTree.expandPath(new TreePath(nestedNode.getPath()));
                        SwingUtilities.invokeLater(expandRunnable);
                    }
                    return parentWrapper;
                }
                if (directoryNode.getParent() == null) {    //find first node in tree
                    DirectoryNode parentWrapper = ((DirectoryNode) directoryNode).getWrapper();
                    if (parentWrapper != null) {
                        while (parentWrapper.getWrapper() != null) {
                            parentWrapper = parentWrapper.getWrapper();
                        }
                        return parentWrapper;
                    }
                }
            }
            return directoryNode;
        }

        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        final VirtualFile sourceRoot = fileIndex.getSourceRootForFile(virtualFile);
        final VirtualFile contentRoot = fileIndex.getContentRootForFile(virtualFile);

        directoryNode = new DirectoryNode(virtualFile, myProject, myCompactEmptyMiddlePackages, myFlattenPackages, myBaseDir,
                myContentRoots);
        myModuleDirNodes.get(dependencyType).put(virtualFile, (DirectoryNode) directoryNode);

        final VirtualFile directory = virtualFile.getParent();
        if (!myFlattenPackages && directory != null) {
            if (myCompactEmptyMiddlePackages && !Comparing.equal(sourceRoot, virtualFile) && !Comparing.equal(contentRoot, virtualFile)) {//compact
                ((DirectoryNode) directoryNode).setCompactedDirNode(childNode);
            }
            if (fileIndex.getModuleForFile(directory) == module) {
                DirectoryNode parentDirectoryNode = myModuleDirNodes.get(dependencyType).get(directory);
                if (parentDirectoryNode != null
                        || !myCompactEmptyMiddlePackages
                        || (sourceRoot != null && VfsUtilCore.isAncestor(directory, sourceRoot, false) && fileIndex.getSourceRootForFile(directory) != null)
                        || Comparing.equal(directory, contentRoot)) {
                    getModuleDirNode(directory, module, (DirectoryNode) directoryNode, dependencyType).add(directoryNode);
                } else {
                    directoryNode = getModuleDirNode(directory, module, (DirectoryNode) directoryNode, dependencyType);
                }
            } else {
                getModuleNode(module, dependencyType).add(directoryNode);
            }
        } else {
            if (Comparing.equal(contentRoot, virtualFile)) {
                getModuleNode(module, dependencyType).add(directoryNode);
            } else {
                final VirtualFile root;
                if (!Comparing.equal(sourceRoot, virtualFile) && sourceRoot != null) {
                    root = sourceRoot;
                } else if (contentRoot != null) {
                    root = contentRoot;
                } else {
                    root = null;
                }
                if (root != null) {
                    getModuleDirNode(root, module, null, dependencyType).add(directoryNode);
                } else {
                    if (myExternalNode == null) {
                        myExternalNode = new GeneralGroupNode("External Dependencies", AllIcons.Nodes.PpLibFolder, myProject);
                        myRoot.add(myExternalNode);
                    }

                    myExternalNode.add(directoryNode);
                }
            }
        }

        return directoryNode;
    }


    @Nullable
    private PackageDependenciesNode getModuleNode(Module module, DependencyType dependencyType) {
        GeneralGroupNode mainNode = getMainNode(dependencyType);
        if (module == null || !myShowModules) {
            return mainNode;
        }
        ModuleNode node = myModuleNodes.get(dependencyType).get(module);
        if (node != null) return node;
        node = new ModuleNode(module, myShowModuleGroups ? myGrouper : null);
        final List<String> groupPath = myGrouper.getGroupPath(module);
        if (groupPath.isEmpty()) {
            myModuleNodes.get(dependencyType).put(module, node);
            mainNode.add(node);
            return node;
        }
        myModuleNodes.get(dependencyType).put(module, node);
        if (myShowModuleGroups) {
            getParentModuleGroup(groupPath, dependencyType).add(node);
        } else {
            mainNode.add(node);
        }
        return node;
    }

    private GeneralGroupNode getMainNode(DependencyType dependencyType) {
        if (dependencyType == DependencyType.FORWARD) {
            return mForwardDependenciesNode;
        } else if (dependencyType == DependencyType.BACKWARD) {
            return mBackwardDependenciesNode;
        } else if (dependencyType == DependencyType.CYCLE) {
            return mCycleDependenciesNode;
        } else {
            return myExternalNode;
        }
    }

    private PackageDependenciesNode getParentModuleGroup(List<String> groupPath, DependencyType dependencyType) {
        final String key = StringUtil.join(groupPath, "");
        ModuleGroupNode groupNode = myModuleGroupNodes.get(dependencyType).get(key);
        if (groupNode == null) {
            groupNode = new ModuleGroupNode(new ModuleGroup(groupPath), myProject);
            myModuleGroupNodes.get(dependencyType).put(key, groupNode);
            getMainNode(dependencyType).add(groupNode);
        }
        if (groupPath.size() > 1) {
            final PackageDependenciesNode node = getParentModuleGroup(groupPath.subList(0, groupPath.size() - 1), dependencyType);
            node.add(groupNode);
        }
        return groupNode;
    }

    public static @NlsContexts.ProgressText String getScanningPackagesMessage() {
        return CodeInsightBundle.message("package.dependencies.build.progress.text");
    }
}
