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
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ExpressionFunctionArgument {

  private final String name, description;
  private final boolean required;
  private final Class<?>[] allowedTypes;

  /**
   * Create a new expression function argument from it's parameters
   * @param name Name of the argument (required, unique)
   * @param description Description of the argument (required), printed on errors
   * @param required Whether this argument is required (present and non-null) or optional (nullable, omittable)
   * @param allowedTypes List of types which this parameter accepts, leave empty to disable type checking
   */
  public ExpressionFunctionArgument(String name, String description, boolean required, Class<?>... allowedTypes) {
    this.name = name;
    this.description = description;
    this.required = required;
    this.allowedTypes = allowedTypes;
  }

  /**
   * Checks whether the passed object matches the argument description embodied by this
   * instance and tries to convert mismatching values using the value interpreter, if possible.
   * @param o Input object to validate
   * @param valueInterpreter Reference to the currently in-use value interpreter for possible auto-conversions
   * @return Tuple of validation state as well as either the looped-through input object or a auto-converted substitution of it
   */
  public Tuple<Boolean, @Nullable Object> checkDescriptionAndPossiblyConvert(@Nullable Object o, IValueInterpreter valueInterpreter) {
    // Argument value is not present but has been required to be
    if (required && o == null)
      return Tuple.of(false, null);

    // No argument types specified, allow everything
    if (allowedTypes.length == 0)
      return Tuple.of(true, o);

    // Argument value is not present but also not required
    if (!required && o == null)
      return Tuple.of(true, null);

    Class<?> type = o.getClass();

    boolean anyAllowedMatches = false;

    for (Class<?> allowedType : allowedTypes) {
      // This allowed type cannot be assigned to the provided type
      if (!allowedType.isAssignableFrom(type)) {

        // Try to automatically convert to this allowed type
        Object convertedSubstitution = tryConvertValue(o, allowedType, valueInterpreter);

        // Could not convert, this type doesn't match either way
        if (convertedSubstitution == null)
          continue;

        // Substitute
        o = convertedSubstitution;
      }

      // The provided type is part of the allowed types and thus passes
      anyAllowedMatches = true;
      break;
    }

    return Tuple.of(anyAllowedMatches, o);
  }

  /**
   * Tries to automatically convert the passed value into an acceptable type
   * @param value Value to try to convert
   * @param targetType Type to try to convert to
   * @param valueInterpreter Reference to the currently in-use value interpreter for possible auto-conversions
   * @return Converted value on success, null on failure
   */
  private @Nullable Object tryConvertValue(@NotNull Object value, Class<?> targetType, IValueInterpreter valueInterpreter) {
    if (targetType == String.class)
      return valueInterpreter.asString(value);

    if (targetType == Long.class)
      return valueInterpreter.asLong(value);

    if (targetType == Double.class)
      return valueInterpreter.asDouble(value);

    if (targetType == Boolean.class)
      return valueInterpreter.asBoolean(value);

    // Convert a map to a list of it's entries
    if (Collection.class.isAssignableFrom(targetType) && Map.class.isAssignableFrom(value.getClass()))
      return new ArrayList<>(((Map<?, ?>) value).entrySet());

    // Cannot auto-convert this type
    return null;
  }

  /**
   * Concatenates all fully qualified type names of the allowed
   * type list in a human-readable format
   */
  public String stringifyAllowedTypes() {
    if (allowedTypes.length == 0) {
      if (required)
        return "<any, non-null>";
      return "<any, nullable>";
    }

    return Arrays.stream(allowedTypes)
      .map(Class::getName)
      .collect(Collectors.joining(" | "));
  }
}
