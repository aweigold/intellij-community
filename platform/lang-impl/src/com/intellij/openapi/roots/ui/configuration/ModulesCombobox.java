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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.SortedComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
//todo[nik] use this class where possible
public class ModulesCombobox extends ComboBox {
  private final SortedComboBoxModel<Module> myModel;

  public ModulesCombobox() {
    this(new SortedComboBoxModel<Module>(ModulesAlphaComparator.INSTANCE));
  }

  private ModulesCombobox(final SortedComboBoxModel<Module> model) {
    super(model);
    myModel = model;
    new ComboboxSpeedSearch(this){
      @Override
      protected String getElementText(Object element) {
        if (element instanceof Module) {
          return ((Module)element).getName();
        } else if (element == null) {
          return "";
        }
        return super.getElementText(element);
      }
    };
    setRenderer(new ListCellRendererWrapper<Module>() {
      @Override
      public void customize(JList list, Module value, int index, boolean selected, boolean hasFocus) {
        if (value != null) {
          setText(value.getName());
          setIcon(ModuleType.get(value).getIcon());
        }
        else {
          setText("[none]");
        }
      }
    });
  }

  public void fillModules(@NotNull Project project) {
    fillModules(project, null);
  }

  public void fillModules(@NotNull Project project, final @Nullable ModuleType moduleType) {
    myModel.clear();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (moduleType == null || moduleType.equals(ModuleType.get(module))) {
        myModel.add(module);
      }
    }
  }

  public void setSelectedModule(@Nullable Module module) {
    myModel.setSelectedItem(module);
  }

  @Nullable
  public Module getSelectedModule() {
    return myModel.getSelectedItem();
  }
}
