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

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

@Getter
@AllArgsConstructor
public abstract class AExpression {

  @StringifyExclude
  private final Token head, tail;

  @StringifyExclude
  private final String fullContainingExpression;

  public abstract String expressionify();

  public String stringify(String indentWidth, int indentLevel) throws Exception {
    String indent = indentWidth.repeat(indentLevel);
    StringBuilder result = new StringBuilder(getClass().getSimpleName() + " {\n");

    Class<?> currentClass = getClass();
    boolean firstField = true;

    while (currentClass != Object.class) {
      for (Field f : currentClass.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(StringifyExclude.class))
          continue;

        if (!f.trySetAccessible())
          continue;

        if (!firstField)
          result.append(",\n");

        Object value = f.get(this);

        result
          .append(indent)
          .append(indentWidth)
          .append(f.getName())
          .append('=');

        if (value instanceof AExpression)
          result.append(((AExpression) value).stringify(indentWidth, indentLevel + 1));
        else if (value instanceof List) {
          List<?> valueList = (List<?>) value;

          StringBuilder listBuilder = new StringBuilder("[\n");

          for (Object item : valueList) {
            if (item instanceof AExpression) {
              listBuilder.append(indent)
                .append(indentWidth.repeat(2))
                .append(((AExpression) item).stringify(indentWidth, indentLevel + 2))
                .append('\n');
            } else {
              listBuilder.append(indent)
                .append(indentWidth.repeat(2))
                .append(item.toString())
                .append('\n');
            }
          }

          result.append(listBuilder).append(indent).append(indentWidth).append("]");
        }
        else
          result.append(value);

        firstField = false;
      }

      currentClass = currentClass.getSuperclass();
    }

    if (!firstField)
      result.append("\n");

    result.append(indent).append("}");

    return result.toString();
  }
}
