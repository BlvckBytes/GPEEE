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
import me.blvckbytes.gpeee.parser.MathOperation;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
public class MathExpression extends ABinaryExpression {

  private final MathOperation operation;

  public MathExpression(AExpression lhs, AExpression rhs, MathOperation operation, Token head, Token tail, String fullContainingExpression) {
    super(lhs, rhs, head, tail, fullContainingExpression);
    this.operation = operation;
  }

  @Override
  public boolean canBeCombinedToOptimize(ABinaryExpression other) {
    if (other instanceof MathExpression)
      return ((MathExpression) other).getOperation().equals(operation);
    return false;
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    switch (operation) {
      case ADDITION:
        return TokenType.PLUS.getRepresentation();
      case SUBTRACTION:
        return TokenType.MINUS.getRepresentation();
      case MULTIPLICATION:
        return TokenType.MULTIPLICATION.getRepresentation();
      case DIVISION:
        return TokenType.DIVISION.getRepresentation();
      case MODULO:
        return TokenType.MODULO.getRepresentation();
      case POWER:
        return TokenType.EXPONENT.getRepresentation();
      default:
        return null;
    }
  }
}
