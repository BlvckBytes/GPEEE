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

/**
 * Categories of token types, defined in the order of tokenization trial. This means
 * that - for example - a keyword should be tried to be tokenized before a value,
 * as that value could be an identifier having the same name as a reserved
 * keyword (which would be illegal).
 */
public enum TokenCategory {
  // Literals and keywords share the same importance, as
  // they should never collide by design
  LITERAL, KEYWORD,

  // Values include identifiers, so they're definitely after reserved words
  VALUE,

  // Operators and symbols share the same importance, as
  // they should never collide by design
  OPERATOR, SYMBOL,

  // Invisible tokens are not of interest to the AST
  INVISIBLE
}
