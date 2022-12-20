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
import java.util.*;
import java.util.function.BiFunction;

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

    // A map will be zero if empty and one otherwise
    if (value instanceof Map)
      return ((Map<?, ?>) value).size() == 0 ? 0 : 1;

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

    // Transform a map to a list of it's entries
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      return asString(new ArrayList<>(map.entrySet()));
    }

    // Stringify map entries
    if (value instanceof Map.Entry) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
      return "(" + asString(entry.getKey()) + " -> " + asString(entry.getValue()) + ")";
    }

    if (value instanceof Collection<?> || value.getClass().isArray()) {
      StringBuilder result = new StringBuilder();

      if (value.getClass().isArray()) {
        for (int i = 0; i < Array.getLength(value); i++)
          result.append(i == 0 ? "" : ", ").append(asString(Array.get(value, i)));
      }

      else {
        int i = 0;
        for (Object item : ((Collection<?>) value)) {
          result.append(i == 0 ? "" : ", ").append(asString(item));
          i++;
        }
      }

      return "[" + result + "]";
    }

    return value.toString();
  }

  @Override
  public List<Object> asCollection(@Nullable Object value) {
    // Collections are just wrapped in arraylists
    if (value instanceof Collection<?>)
      return new ArrayList<>((Collection<?>) value);

    // Maps get converted to a list of their entries
    if (value instanceof Map)
      return new ArrayList<>(((Map<?, ?>) value).entrySet());

    List<Object> result = new ArrayList<>();

    // Null corresponds to the empty list
    if (value == null)
      return result;

    // Create a list containing the input value
    result.add(value);
    return result;
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

    // Non-null doesn't equal null in strict mode
    if (strict && (a == null || b == null))
      return false;

    // Both values are of type string, specific rules apply
    if (a instanceof String && b instanceof String) {
      String sA = (String) a, sB = (String) b;

      // In non-strict mode, they are compared ignoring case and whitespace padding
      return strict ? sA.equals(sB) : sA.trim().equalsIgnoreCase(sB.trim());
    }

    // Comparing a string against a number - compare their contents
    // Only works in non-strict mode, of course
    if (!strict && (a instanceof String && b instanceof Number || a instanceof Number && b instanceof String)) {
      String stringValue = a instanceof String ? (String) a : (String) b;
      Object numberValue = a instanceof Number ? a : b;

      // Splice off non-required decimal points for the ease of comparison
      if (!hasDecimalPoint(numberValue))
        numberValue = asLong(numberValue);

      Object stringNumber;
      try {
        if (stringValue.contains("."))
          stringNumber = Double.parseDouble(stringValue);
        else
          stringNumber = Long.parseLong(stringValue);
      }

      // Could not parse the number string, so it's definitely not equal to that value
      catch (Exception e) {
        return false;
      }

      // Compare the string parsed as a double (to allow for max precision)
      return compare(stringNumber, numberValue) == 0;
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

      return doIterablesContainSameItems(cA, cB, strict, (vA, vB) -> areEqual(vA, vB, strict));
    }

    // Both values are a map, compare their type and entry sets
    if (a instanceof Map && b instanceof Map) {
      Map<?, ?> mA = (Map<?, ?>) a, mB = (Map<?, ?>) b;

      // Cannot equal as they have a different number of items
      if (mA.size() != mB.size())
        return false;

      return doIterablesContainSameItems(
        mA.entrySet(), mB.entrySet(), strict,
        (vA, vB) -> {
          Map.Entry<?, ?> eA = (Map.Entry<?, ?>) vA, eB = (Map.Entry<?, ?>) vB;

          // Check equality of the key and the value separately
          return (
            areEqual(eA.getKey(), eB.getKey(), strict) &&
            areEqual(eA.getValue(), eB.getValue(), strict)
          );
        }
      );
    }

    // Both values are an array, compare their type and contents
    if (a != null && a.getClass().isArray() && b != null && b.getClass().isArray()) {

      // Array types mismatch
      if (!a.getClass().getComponentType().equals(b.getClass().getComponentType()))
        return false;

      // instanceof boolean[], byte[], short[], char[], int[], long[], float[], double[], or Object[],

      // Size mismatch
      int lengthA = Array.getLength(a);
      if (lengthA != Array.getLength(b))
        return false;

      return doIterablesContainSameItems(wrapArrayInIterable(a), wrapArrayInIterable(b), strict, (vA, vB) -> areEqual(vA, vB, strict));
    }

    // Fallback: Compare as integers (in non-strict mode now anyways)
    return compare(a, b) == 0;
  }

  /**
   * Wraps an array in an iterable and uses reflect to access
   * it's items as well as it's length value
   * @param array Array to wrap
   * @return Iterable which operates on the passed array
   */
  private Iterable<Object> wrapArrayInIterable(Object array) {
    return () -> new Iterator<>() {

      int i = 0;

      @Override
      public boolean hasNext() {
        return i < Array.getLength(array);
      }

      @Override
      public Object next() {
        return Array.get(array, i++);
      }
    };
  }

  /**
   * Checks whether two iterables contain the same items and also checks for sequence order if in strict mode
   * @param iterableA Iterable A
   * @param iterableB Iterable B
   * @param strict Whether to compare sequence order too
   * @param comparator External value comparator function
   * @return True if all items that are available in A are also available in B
   */
  private boolean doIterablesContainSameItems(
    Iterable<?> iterableA, Iterable<?> iterableB,
    boolean strict, BiFunction<Object, Object, Boolean> comparator
  ) {

    // Check items in order
    if (strict) {
      Iterator<?> iterA = iterableA.iterator(), iterB = iterableB.iterator();

      while (iterA.hasNext()) {
        // Different size
        if (!iterB.hasNext())
          return false;

        // An item mismatched
        if (!comparator.apply(iterA.next(), iterB.next()))
          return false;
      }

      // All items were equal in the right order
      return true;
    }

    // Check items while ignoring order

    // Keep track of matched indices in the B iterator
    // to not match the same "slot" twice
    List<Integer> matchedBIndices = new ArrayList<>();

    for (Object valueA : iterableA) {
      Iterator<?> iterB = iterableB.iterator();
      boolean anyMatched = false;

      int indexB = 0;
      while (iterB.hasNext()) {
        // An item mismatched
        if (!comparator.apply(valueA, iterB.next()) || matchedBIndices.contains(indexB)) {
          ++indexB;
          continue;
        }

        anyMatched = true;
        matchedBIndices.add(indexB);
        break;
      }

      if (!anyMatched)
        return false;
    }

    return true;
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
