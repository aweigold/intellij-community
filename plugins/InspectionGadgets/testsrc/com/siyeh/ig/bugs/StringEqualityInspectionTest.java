package com.siyeh.ig.bugs;

import com.siyeh.ig.IGInspectionTestCase;

public class StringEqualityInspectionTest extends IGInspectionTestCase {

  public void test() throws Exception {
    doTest("com/siyeh/igtest/bugs/string_equality", new StringEqualityInspection());
  }
}