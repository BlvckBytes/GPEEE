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

package me.blvckbytes.gpeee.parser.expression;

import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class IndexExpression extends ABinaryExpression {

  private final boolean optional;

  public IndexExpression(AExpression target, AExpression input, boolean optional, Token head, Token tail, String fullContainingExpression) {
    super(target, input, head, tail, fullContainingExpression);

    this.optional = optional;
  }

  public boolean isOptional() {
    return optional;
  }

  @Override
  public String expressionify() {
    return lhs.expressionify() +
    TokenType.BRACKET_OPEN.getRepresentation() +
    rhs.expressionify() +
    TokenType.BRACKET_CLOSE.getRepresentation();
  }

  @Override
  public boolean canBeCombinedToOptimize(ABinaryExpression other) {
    // Index expressions never equal one another, as they're dependent on
    // the exact order of operation and cannot be optimized away at all
    return false;
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    // Null, as expressionify is overridden
    return null;
  }
}
