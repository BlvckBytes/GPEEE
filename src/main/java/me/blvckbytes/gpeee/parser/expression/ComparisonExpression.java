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

import lombok.Getter;
import me.blvckbytes.gpeee.parser.ComparisonOperation;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
public class ComparisonExpression extends ABinaryExpression {

  private final ComparisonOperation operation;

  public ComparisonExpression(AExpression lhs, AExpression rhs, ComparisonOperation operation, Token head, Token tail, String fullContainingExpression) {
    super(lhs, rhs, head, tail, fullContainingExpression);

    this.operation = operation;
  }

  @Override
  public boolean canBeCombinedToOptimize(ABinaryExpression other) {
    if (other instanceof ComparisonExpression)
      return ((ComparisonExpression) other).getOperation().equals(operation);
    return false;
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    switch (operation) {
      case LESS_THAN:
        return TokenType.LESS_THAN.getRepresentation();
      case GREATER_THAN:
        return TokenType.GREATER_THAN.getRepresentation();
      case LESS_THAN_OR_EQUAL:
        return TokenType.LESS_THAN_OR_EQUAL.getRepresentation();
      case GREATER_THAN_OR_EQUAL:
        return TokenType.GREATER_THAN_OR_EQUAL.getRepresentation();
      default:
        return null;
    }
  }
}
