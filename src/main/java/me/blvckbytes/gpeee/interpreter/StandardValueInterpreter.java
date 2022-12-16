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

package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.parser.MathOperation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

public class StandardValueInterpreter implements IValueInterpreter {

  @Override
  public boolean asBoolean(@Nullable Object value) {
    if (value == null)
      return false;

    if (value instanceof Boolean)
      return ((Boolean) value);

    // Considered true whenever it's integer value is greater than zero
    return compare(value, 0) > 0;
  }

  @Override
  public long asLong(@Nullable Object value) {
    if (value == null)
      return 0;

    if (value instanceof Long)
      return ((Long) value);

    // Is a boolean, true equals one, false zero
    if (value instanceof Boolean)
      return ((Boolean) value) ? 1 : 0;

    // Is any other number, get it's value as a long
    if (value instanceof Number)
      return ((Number) value).longValue();

    // A collection will be zero if empty and one otherwise
    if (value instanceof Collection)
      return ((Collection<?>) value).size() == 0 ? 0 : 1;

    // An array will be zero if empty and one otherwise
    if (value.getClass().isArray())
      return Array.getLength(value) == 0 ? 0 : 1;

    // A string will be zero if blank and one otherwise
    if (value instanceof String)
      return ((String) value).isBlank() ? 0 : 1;

    // Unknown other value, interpret as zero
    return 0;
  }

  @Override
  public double asDouble(@Nullable Object value) {
    if (value == null)
      return 0;

    if (value instanceof Double)
      return ((Double) value);

    // Is any other number, get it's value as a double
    if (value instanceof Number)
      return ((Number) value).doubleValue();

    // Interpret as long and implicitly cast to a double
    return asLong(value);
  }

  @Override
  public String asString(@Nullable Object value) {
    if (value == null)
      return "<null>";

    if (value instanceof String)
      return ((String) value);

    return value.toString();
  }

  @Override
  public boolean hasDecimalPoint(@Nullable Object value) {
    return (value instanceof Float || value instanceof Double);
  }

  @Override
  public boolean areEqual(@Nullable Object a, @Nullable Object b, boolean strict) {
    // Null always equals itself
    if (a == null && b == null)
      return true;

    // Non-null doesn't equal null
    if (a == null || b == null)
      return false;

    // Both values are of type string, specific rules apply
    if (a instanceof String && b instanceof String) {
      String sA = (String) a, sB = (String) b;

      // In non-strict mode, they are compared ignoring case and whitespace padding
      return strict ? sA.equals(sB) : sA.trim().equalsIgnoreCase(sB.trim());
    }

    // In strict mode, so the value types have to match exactly
    if (strict && a.getClass() != b.getClass())
      return false;

    // Both values are a collection, compare their type and contents
    if (a instanceof Collection && b instanceof Collection) {
      Collection<?> cA = (Collection<?>) a, cB = (Collection<?>) b;

      // Cannot equal as they have a different number of items
      if (cA.size() != cB.size())
        return false;

      Iterator<?> iterA = cA.iterator(), iterB = cB.iterator();

      // It's safe to loop both in order as they have the same size
      while (iterA.hasNext()) {
        // An item mismatched
        if (!areEqual(iterA.next(), iterB.next(), strict))
          return false;
      }

      // All checks passed, these collections equal
      return true;
    }

    // Both values are an array, compare their type and contents
    if (a.getClass().isArray() && b.getClass().isArray()) {
      // Array types mismatch
      if (!a.getClass().getComponentType().equals(b.getClass().getComponentType()))
        return false;

      // instanceof boolean[], byte[], short[], char[], int[], long[], float[], double[], or Object[],

      // Size mismatch
      int lengthA = Array.getLength(a);
      if (lengthA != Array.getLength(b))
        return false;

      // Compare items one by one
      for (int i = 0; i < lengthA; i++) {
        // An item has mismatched
        if (!areEqual(Array.get(a, i), Array.get(b, i), strict))
          return false;
      }

      // All checks passed, these arrays equal
      return true;
    }

    // Fallback: Compare as integers (in non-strict mode now anyways)
    return compare(a, b) == 0;
  }

  @Override
  public Object performMath(@Nullable Object a, @Nullable Object b, MathOperation operation) {
    switch (operation) {
      case ADDITION: {
        if (hasDecimalPoint(a) || hasDecimalPoint(b))
          return asDouble(a) + asDouble(b);
        return asLong(a) + asLong(b);
      }

      case SUBTRACTION: {
        if (hasDecimalPoint(a) || hasDecimalPoint(b))
          return asDouble(a) - asDouble(b);
        return asLong(a) - asLong(b);
      }

      case MULTIPLICATION: {
        if (hasDecimalPoint(a) || hasDecimalPoint(b))
          return asDouble(a) * asDouble(b);
        return asLong(a) * asLong(b);
      }

      case DIVISION: {
        if (hasDecimalPoint(a) || hasDecimalPoint(b))
          return asDouble(a) / asDouble(b);

        long lA = asLong(a), lB = asLong(b);

        // Not an even division, use doubles to not truncate the decimal places
        if (lA % lB != 0)
          return (double) lA / (double) lB;

        return lA / lB;
      }

      case MODULO: {
        if (hasDecimalPoint(a) || hasDecimalPoint(b))
          return asDouble(a) % asDouble(b);
        return asLong(a) % asLong(b);
      }

      case POWER: {
        if (hasDecimalPoint(a) || hasDecimalPoint(b))
          return Math.pow(asDouble(a), asDouble(b));
        return (long) Math.pow(asDouble(a), asDouble(b));
      }

      // Unknown operation
      default:
        return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public int compare(@Nullable Object a, @Nullable Object b) {
    // Both values are a comparable already
    // Their types match, so the vanilla comparison method can be used safely
    if (a instanceof Comparable && b instanceof Comparable && a.getClass() == b.getClass())
      return ((Comparable<Object>) a).compareTo(b);

    // Doubles cannot be reduced to integers in order to not distort the comparison, so the other value
    // has to be interpreted as a double to allow for same-type comparison
    if (hasDecimalPoint(a) || hasDecimalPoint(b))
      return Double.compare(asDouble(a), asDouble(b));

    // The fallback is to always just compare whole numbers
    return Long.compare(asLong(a), asLong(b));
  }
}
