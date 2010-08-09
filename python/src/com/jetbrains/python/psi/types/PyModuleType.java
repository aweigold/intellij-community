package com.jetbrains.python.psi.types;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.jetbrains.python.PyNames;
import com.jetbrains.python.psi.AccessDirection;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyQualifiedExpression;
import com.jetbrains.python.psi.impl.PyBuiltinCache;
import com.jetbrains.python.psi.resolve.ResolveImportUtil;
import com.jetbrains.python.psi.resolve.VariantsProcessor;
import com.jetbrains.python.toolbox.Maybe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.jetbrains.python.psi.resolve.ResolveImportUtil.ROLE_IN_IMPORT.NONE;
// .impl looks impure

/**
 * @author yole
 */
public class PyModuleType implements PyType { // Modules don't descend from object
  private final PsiFile myModule;

  protected static Set<String> ourPossibleFields;

  static {
    ourPossibleFields = new HashSet<String>();
    ourPossibleFields.add("__name__");
    ourPossibleFields.add("__file__");
    ourPossibleFields.add("__path__");
    ourPossibleFields.add("__doc__");
    ourPossibleFields.add("__dict__");
    ourPossibleFields = Collections.unmodifiableSet(ourPossibleFields);
  }

  public PyModuleType(PsiFile source) {
    myModule = source;
  }

  public PsiFile getModule() {
    return myModule;
  }

  @Nullable
  public List<? extends PsiElement> resolveMember(final String name, AccessDirection direction) {
    //return PyResolveUtil.treeWalkUp(new PyResolveUtil.ResolveProcessor(name), myModule, null, null);
    final PsiElement result = ResolveImportUtil.resolveChild(myModule, name, null, false);
    if (result != null) return new SmartList<PsiElement>(result);
    return Collections.emptyList();
  }


  /**
   * @return a list of submodules of this module, either files or dirs, for easier naming; may contain filenames
   *         not suitable for import.
   */
  @NotNull
  public List<PsiFileSystemItem> getSubmodulesList() {
    List<PsiFileSystemItem> result = new ArrayList<PsiFileSystemItem>();

    if (PyNames.INIT_DOT_PY.equals(myModule.getName())) { // our module is a dir, not a single file
      PsiDirectory mydir = myModule.getContainingDirectory();
      if (mydir != null) { // just in case
        // file modules
        for (PsiFile f : mydir.getFiles()) {
          final String filename = f.getName();
          if (f instanceof PyFile && !filename.equals(PyNames.INIT_DOT_PY)) result.add(f);
        }
        // dir modules
        for (PsiDirectory dir : mydir.getSubdirectories()) {
          if (dir.findFile(PyNames.INIT_DOT_PY) instanceof PyFile) result.add(dir);
        }
      }
    }
    return result;
  }

  public Object[] getCompletionVariants(String completionPrefix, PyExpression expressionHook, ProcessingContext context) {
    Set<String> names_already = context.get(CTX_NAMES);
    List<Object> result = new ArrayList<Object>();
    ResolveImportUtil.ROLE_IN_IMPORT role = ResolveImportUtil.getRoleInImport(expressionHook.getReference());
    if (role == NONE) { // when not inside import, add regular attributes
      final VariantsProcessor processor = new VariantsProcessor(expressionHook);
      myModule.processDeclarations(processor, ResolveState.initial(), null, expressionHook);
      if (names_already != null) {
        for (LookupElement le : processor.getResultList()) {
          String name = le.getLookupString();
          if (!names_already.contains(name)) {
            result.add(le);
            names_already.add(name);
          }
        }
      }
      else {
        result.addAll(processor.getResultList());
      }
    }
    else /*if (role == AS_MODULE)*/ { // when being imported, add submodules
      for (PsiFileSystemItem pfsi : getSubmodulesList()) {
        String s = pfsi.getName();
        int pos = s.lastIndexOf('.'); // it may not contain a dot, except in extension; cut it off.
        if (pos > 0) s = s.substring(0, pos);
        if (!PyNames.isIdentifier(s)) continue;
        if (names_already != null) {
          if (names_already.contains(s)) {
            continue;
          }
          else {
            names_already.add(s);
          }
        }
        result.add(LookupElementBuilder.create(pfsi, s).setPresentableText(s));
      }
    }
    return result.toArray();
  }

  public String getName() {
    PsiFile mod = getModule();
    if (mod != null) {
      return mod.getName();
    }
    else {
      return null;
    }
  }

  @Override
  public boolean isBuiltin() {
    return true;
  }

  @NotNull
  public static Set<String> getPossibleInstanceMembers() {
    return ourPossibleFields;
  }

}
