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

package me.blvckbytes.gpeee.tokenizer;

import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class TokenizerState {

  public int row, col, charIndex;
  public Stack<Integer> colStack;
  public @Nullable Token currentToken, previousToken;

  public TokenizerState() {
    this.colStack = new Stack<>();
  }

  public TokenizerState copy() {
    TokenizerState copy = new TokenizerState();

    copy.row = this.row;
    copy.col = this.col;
    copy.charIndex = this.charIndex;
    copy.colStack = this.colStack;
    copy.currentToken = this.currentToken;
    copy.previousToken = this.previousToken;

    return copy;
  }
}
