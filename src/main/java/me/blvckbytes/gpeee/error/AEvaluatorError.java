/*
 * MIT License
 *
 * Copyright (c) 2022 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.gpeee.error;

import lombok.Getter;

@Getter
public abstract class AEvaluatorError extends RuntimeException {

  public AEvaluatorError(int row, int col, String rawInput, String text) {
    super(generateWarning(row, col, rawInput, text));
  }

  private static String generateWarning(int row, int col, String rawInput, String text) {
    StringBuilder result = new StringBuilder();
    String targetLine = rawInput.split("\n")[row];

    // Split text on newlines to support properly indented multi-line prints
    String[] textLines = text.split("\n");

    String lineNumber = (row + 1) + ": ";

    // Append the marker with the first line next to it
    result
      // Show the target line with it's line number
      .append(lineNumber).append(targetLine).append("\n")
      // Draw an indicator under the target column and print the text next to it
      .append(" ".repeat(col + lineNumber.length())).append("^").append(" ").append(textLines[0]).append("\n");

    // Append remaining lines (if any) right below the text next to the marker
    for (int i = 1; i < textLines.length; i++)
      result.append(" ".repeat(col + lineNumber.length() + 2)).append(textLines[i]).append("\n");

    return result.toString();
  }
}
