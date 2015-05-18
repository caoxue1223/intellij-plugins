package com.jetbrains.lang.dart.dart_style;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.FormatterTestCase;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.DartLanguage;
import com.jetbrains.lang.dart.util.DartTestUtils;

import java.io.*;
import java.util.List;
import java.util.regex.*;

public class DartStyleTest extends FormatterTestCase {

  protected String getFileExtension() {
    return DartFileType.DEFAULT_EXTENSION;
  }
  protected String getTestDataPath() {
    return DartTestUtils.BASE_TEST_DATA_PATH;
  }
  protected String getBasePath() {
    return "dart_style";
  }

  public void test1() throws Exception {
    runTestsInDirectory("comments");
  }

  /// Run tests defined in "*.unit" and "*.stmt" files inside directory [name].
  void runTestsInDirectory(String name) throws Exception {
    Pattern indentPattern = Pattern.compile("^\\(indent (\\d+)\\)\\s*");

    File dir = new File(new File(getTestDataPath(), getBasePath()), name);
    for (File entry : dir.listFiles()) {
      if (!entry.getName().endsWith(".stmt") && !entry.getName().endsWith(".unit")) {
        continue;
      }
      String[] lines = FileUtil.loadLines(entry).toArray(new String[0]);

      // The first line may have a "|" to indicate the page width.
      int pageWidth = 0;
      int i = 0;
      if (lines[0].endsWith("|")) {
        pageWidth = lines[0].indexOf("|");
        i = 1;
      }
      while (i < lines.length) {
        String description = lines[i++].replaceAll(">>>", "").trim();

        // Let the test specify a leading indentation. This is handy for
        // regression tests which often come from a chunk of nested code.
        int leadingIndent = 0;
        Matcher matcher = indentPattern.matcher(description);
        if (matcher.matches()) {
          // The test specifies it in spaces, but the formatter expects levels.
          leadingIndent = Integer.parseInt(matcher.group(1)) / 2;
          description = description.substring(matcher.end());
        }

        if (description == "") {
          description = "line " + (i + 1);
        } else {
          description = "line " + (i + 1) + ": " + description;
        }

        String input = "";
        while (!lines[i].startsWith("<<<")) {
          input += lines[i++] + "\n";
        }

        String expectedOutput = "";
        while (++i < lines.length && !lines[i].startsWith(">>>")) {
          expectedOutput += lines[i] + "\n";
        }

        boolean isCompilationUnit = entry.getName().endsWith(".unit");
        SourceCode inputCode = extractSelection(input, isCompilationUnit);
        SourceCode expected = extractSelection(expectedOutput, isCompilationUnit);
        final CommonCodeStyleSettings settings = getSettings(DartLanguage.INSTANCE);
        settings.RIGHT_MARGIN = pageWidth;
        doTextTest(inputCode.text, expected.text);
      }
    }
  }

  /// Given a source string that contains ‹ and › to indicate a selection, returns
  /// a [SourceCode] with the text (with the selection markers removed) and the
  /// correct selection range.
  private SourceCode extractSelection(String source, boolean isCompilationUnit) {
    int start = source.indexOf("‹");
    source = source.replaceAll("‹", "");

    int end = source.indexOf("›");
    source = source.replaceAll("›", "");

    return new SourceCode(source, isCompilationUnit, start == -1 ? 0 : start, end == -1 ? 0 : end - start);
  }

  static class SourceCode {
    String text;
    boolean isCompilationUnit;
    int selectionStart, selectionLength;

    SourceCode(String content, boolean isCU, int start, int len) {
      this.text = content;
      this.isCompilationUnit = isCU;
      this.selectionStart = start;
      this.selectionLength = len;
    }
  }
}
