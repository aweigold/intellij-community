/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ui.popup.util;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
* Created with IntelliJ IDEA.
* User: zajac
* Date: 5/6/12
* Time: 2:04 AM
* To change this template use File | Settings | File Templates.
*/
public class DetailViewImpl extends JPanel implements DetailView, UserDataHolder {
  private final Project myProject;
  private Editor myEditor;

  private ItemWrapper myWrapper;

  private JPanel myDetailPanel;
  private JBScrollPane myDetailScrollPanel;

  private JPanel myDetailPanelWrapper;
  private RangeHighlighter myHighlighter;
  private PreviewEditorState myEditorState = PreviewEditorState.EMPTY;
  private JComponent myParentComponent;
  private final JLabel myLabel = new JLabel("", SwingConstants.CENTER);


  public void setDoneRunnable(Runnable doneRunnable, JComponent parent) {
    myParentComponent = parent;
    myDoneRunnable = doneRunnable;
  }

  private Runnable myDoneRunnable;

  public DetailViewImpl(Project project) {
    super(new BorderLayout());
    myProject = project;
    setPreferredSize(new Dimension(700, 400));
    myLabel.setVerticalAlignment(SwingConstants.CENTER);
  }

  @Override
  public void clearEditor() {
    if (getEditor() != null) {
      clearHightlighting();
      remove(getEditor().getComponent());
      EditorFactory.getInstance().releaseEditor(getEditor());
      myEditorState = PreviewEditorState.EMPTY;
      setEditor(null);
      repaint();
    }
  }

  @Override
  public void setCurrentItem(@Nullable ItemWrapper wrapper) {
    myWrapper = wrapper;
  }

  @Override
  public PreviewEditorState getEditorState() {
    return myEditorState;
  }

  @Override
  public ItemWrapper getCurrentItem() {
    return myWrapper;
  }

  @Override
  public boolean hasEditorOnly() {
    return false;
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (ScreenUtil.isStandardAddRemoveNotify(this))
      clearEditor();
  }

  @Override
  public Editor getEditor() {
    return myEditor;
  }

  public void setEditor(Editor editor) {
    myEditor = editor;
  }

  @Override
  public void navigateInPreviewEditor(PreviewEditorState editorState) {
    myEditorState = editorState;

    final VirtualFile file = editorState.getFile();
    final LogicalPosition positionToNavigate = editorState.getNavigate();
    final TextAttributes lineAttributes = editorState.getAttributes();
    Document document = FileDocumentManager.getInstance().getDocument(file);
    Project project = myProject;

    clearEditor();
    remove(myLabel);
    if (document != null) {
      if (getEditor() == null || getEditor().getDocument() != document) {
        setEditor(EditorFactory.getInstance().createViewer(document, project));

        final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(file, scheme, project);

        ((EditorEx)getEditor()).setFile(file);
        ((EditorEx)getEditor()).setHighlighter(highlighter);

        getEditor().getSettings().setAnimatedScrolling(false);
        getEditor().getSettings().setRefrainFromScrolling(false);
        getEditor().getSettings().setLineNumbersShown(true);
        getEditor().getSettings().setFoldingOutlineShown(false);

        add(getEditor().getComponent(), BorderLayout.CENTER);
      }

      if (positionToNavigate != null) {
        getEditor().getCaretModel().moveToLogicalPosition(positionToNavigate);
        validate();
        getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER);
      }

      getEditor().setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));

      clearHightlighting();
      if (lineAttributes != null && positionToNavigate != null) {
        myHighlighter = getEditor().getMarkupModel().addLineHighlighter(positionToNavigate.line, HighlighterLayer.SELECTION - 1,
                                                                        lineAttributes);
      }
    }
    else {
      myLabel.setText("Navigate to selected " + (file.isDirectory() ? "directory " : "file ") + "in Project View");
      add(myLabel, BorderLayout.CENTER);
      validate();
    }
  }


  private void clearHightlighting() {
    if (myHighlighter != null) {
      getEditor().getMarkupModel().removeHighlighter(myHighlighter);
      myHighlighter = null;
    }
  }

  @Override
  public JPanel getPropertiesPanel() {
    return myDetailPanel;
  }

  @Override
  public void setPropertiesPanel(@Nullable final JPanel panel) {
    if (panel == myDetailPanel) return;

    if (panel != null) {
      if (myDetailPanelWrapper == null) {
        myDetailPanelWrapper = new JPanel(new GridLayout(1, 1));
        myDetailPanelWrapper.setBorder(IdeBorderFactory.createEmptyBorder(5, 30, 5, 5));
        myDetailPanelWrapper.add(panel);

        add(myDetailPanelWrapper, BorderLayout.NORTH);
      } else {
        myDetailPanelWrapper.removeAll();
        myDetailPanelWrapper.add(panel);
      }
    }
    else {
      myDetailPanelWrapper.removeAll();
      myLabel.setText("Nothing to show");
      add(myLabel, BorderLayout.CENTER);
    }
    myDetailPanel = panel;
    revalidate();
  }

  final UserDataHolderBase myDataHolderBase = new UserDataHolderBase();

  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return myDataHolderBase.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    myDataHolderBase.putUserData(key, value);
  }
}
