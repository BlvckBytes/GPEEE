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

import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FunctionInvocationExpression extends AExpression {

  private final IdentifierExpression name;
  private final List<Tuple<AExpression, @Nullable IdentifierExpression>> arguments;
  private final boolean optional;

  public FunctionInvocationExpression(
    IdentifierExpression name, List<Tuple<AExpression, @Nullable IdentifierExpression>> arguments, boolean optional,
    Token head, Token tail, String fullContainingExpression
  ) {
    super(head, tail, fullContainingExpression);

    this.name = name;
    this.arguments = arguments;
    this.optional = optional;
  }

  public IdentifierExpression getName() {
    return name;
  }

  public List<Tuple<AExpression, IdentifierExpression>> getArguments() {
    return arguments;
  }

  public boolean isOptional() {
    return optional;
  }

  @Override
  public String expressionify() {
    StringBuilder argExpression = new StringBuilder();

    for (int i = 0; i < arguments.size(); i++) {
      Tuple<AExpression, @Nullable IdentifierExpression> argument = arguments.get(i);

      argExpression.append(i == 0 ? "" : ", ");

      // Prepend the named argument expression, if available
      if (argument.b != null)
        argExpression.append(argument.b.expressionify()).append(TokenType.ASSIGN.getRepresentation());

      argExpression.append(argument.a.expressionify());
    }

    return (
      name.expressionify() + TokenType.PARENTHESIS_OPEN.getRepresentation() +
      argExpression +
      TokenType.PARENTHESIS_CLOSE.getRepresentation()
    );
  }
}
