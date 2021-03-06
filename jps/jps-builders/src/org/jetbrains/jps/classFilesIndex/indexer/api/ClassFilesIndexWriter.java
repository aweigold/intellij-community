/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.jps.classFilesIndex.indexer.api;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.jps.incremental.CompileContext;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public class ClassFilesIndexWriter<K, V> {
  private final ClassFileIndexer<K, V> myIndexer;
  private final boolean myEmpty;
  protected final ClassFilesIndexStorage<K, V> myIndex;

  protected ClassFilesIndexWriter(final ClassFileIndexer<K, V> indexer, final CompileContext compileContext) {
    myIndexer = indexer;
    final File storageDir = getIndexRoot(compileContext);
    final Set<String> containingFileNames = listFiles(storageDir);
    if (!containingFileNames.contains("version") || !containingFileNames.contains("state")) {
      throw new IllegalStateException("version or state file for index " + indexer.getIndexCanonicalName() + " not found in " + storageDir.getAbsolutePath());
    }
    ClassFilesIndexStorage<K, V> index = null;
    IOException exception = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        index = new ClassFilesIndexStorage<K, V>(storageDir, myIndexer.getKeyDescriptor(), myIndexer.getDataExternalizer());
        break;
      }
      catch (final IOException e) {
        exception = e;
        PersistentHashMap.deleteFilesStartingWith(ClassFilesIndexStorage.getIndexFile(storageDir));
      }
    }
    if (index == null) {
      throw new RuntimeException(exception);
    }
    myIndex = index;
    myEmpty = IndexState.EXIST != IndexState.load(storageDir) || exception != null;
    IndexState.CORRUPTED.save(storageDir);
  }

  private static Set<String> listFiles(final File dir) {
    final String[] containingFileNames = dir.list();
    return containingFileNames == null ? Collections.<String>emptySet() : ContainerUtil.newHashSet(containingFileNames);
  }

  private File getIndexRoot(final CompileContext compileContext) {
    final File rootFile = compileContext.getProjectDescriptor().dataManager.getDataPaths().getDataStorageRoot();
    return ClassFilesIndexStorage.getIndexDir(myIndexer.getIndexCanonicalName(), rootFile);
  }

  public final boolean isEmpty() {
    return myEmpty;
  }

  public final void close(final CompileContext compileContext) {
    try {
      myIndex.close();
      IndexState.EXIST.save(getIndexRoot(compileContext));
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public final void update(final String id, final ClassReader inputData) {
    for (final Map.Entry<K, V> e : myIndexer.map(inputData).entrySet()) {
      myIndex.putData(e.getKey(), e.getValue(), id);
    }
  }
}
