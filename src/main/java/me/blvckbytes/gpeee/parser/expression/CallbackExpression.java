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

import java.util.List;

public class CallbackExpression extends AExpression {

  private final List<IdentifierExpression> signature;

  private AExpression body;

  public CallbackExpression(List<IdentifierExpression> signature, AExpression body, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.signature = signature;
    this.body = body;
  }

  public List<IdentifierExpression> getSignature() {
    return signature;
  }

  public AExpression getBody() {
    return body;
  }

  public void setBody(AExpression body) {
    this.body = body;
  }

  @Override
  public String expressionify() {
    StringBuilder argExpression = new StringBuilder();

    for (int i = 0; i < signature.size(); i++) {
      IdentifierExpression argument = signature.get(i);
      argExpression.append(i == 0 ? "" : ", ").append(argument.getSymbol());
    }

    return (
      TokenType.PARENTHESIS_OPEN.getRepresentation() +
      argExpression +
      TokenType.PARENTHESIS_CLOSE.getRepresentation() +
      " " + TokenType.ARROW.getRepresentation() + " " +
      body.expressionify()
    );
  }
}
