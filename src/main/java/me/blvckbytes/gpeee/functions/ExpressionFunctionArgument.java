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

package me.blvckbytes.gpeee.functions;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class ExpressionFunctionArgument {

  private final String name;
  private final boolean required;
  private final Class<?>[] allowedTypes;

  public ExpressionFunctionArgument(String name, boolean required, Class<?>... allowedTypes) {
    this.name = name;
    this.required = required;
    this.allowedTypes = allowedTypes;
  }

  public boolean describesObject(@Nullable Object o) {
    // Argument value is not present but also not required
    if (!required && o == null)
      return true;

    // Required fields do not accept null values
    if (o == null)
      return false;

    Class<?> type = o.getClass();

    boolean anyAllowedMatches = false;

    // TODO: Automatically convert convertible objects to the required argument type

    for (Class<?> allowedType : allowedTypes) {
      // This allowed type cannot be assigned to the provided type
      if (!allowedType.isAssignableFrom(type))
        continue;

      // The provided type is part of the allowed types and thus passes
      anyAllowedMatches = true;
      break;
    }

    return anyAllowedMatches;
  }

  public String stringifyAllowedTypes() {
    return Arrays.stream(allowedTypes)
      .map(Class::getName)
      .collect(Collectors.joining("|"));
  }
}
