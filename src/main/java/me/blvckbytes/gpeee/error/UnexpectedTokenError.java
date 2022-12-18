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

import me.blvckbytes.gpeee.tokenizer.ITokenizer;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UnexpectedTokenError extends AEvaluatorError {

  public UnexpectedTokenError(ITokenizer tokenizer, @Nullable Token actual, TokenType... expected) {
    super(
      actual == null ? tokenizer.getCurrentRow() : actual.getRow(),
      actual == null ? tokenizer.getCurrentCol() : actual.getCol(),
      tokenizer.getRawText(),
      "Expected token " + formatTokenNames(expected) + ", found " + (actual == null ? "nothing" : actual.getType().name())
    );
  }

  private static String formatTokenNames(TokenType[] tokens) {
    if (tokens.length == 0)
      return "EOF";

    return Arrays.stream(tokens)
      .map(tk -> tk.getRepresentation() == null ? tk.name() : tk.getRepresentation())
      .collect(Collectors.joining(" | "));
  }
}
