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

import me.blvckbytes.gpeee.parser.expression.AExpression;
import org.jetbrains.annotations.Nullable;

public class UnknownMemberError extends AEvaluatorError {

  public UnknownMemberError(AExpression access, @Nullable Object value, String target) {
    super(
      access.getHead().getRow(),
      access.getHead().getCol(),
      access.getFullContainingExpression(),
      "The object of type >" +
      (value == null ? "<null>" : value.getClass().getName()) +
      "< owns no member called >" + target + "<"
    );
  }
}
