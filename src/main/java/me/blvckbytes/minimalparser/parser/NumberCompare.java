package me.blvckbytes.minimalparser.parser;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

@AllArgsConstructor
public enum NumberCompare {

  GREATER_THAN((a, b) -> invokeComparable(a, b) > 0),
  GREATER_THAN_OR_EQUAL((a, b) -> invokeComparable(a, b) >= 0),
  LESS_THAN((a, b) -> invokeComparable(a, b) < 0),
  LESS_THAN_OR_EQUAL((a, b) -> invokeComparable(a, b) <= 0),
  EQUAL((a, b) -> invokeComparable(a, b) == 0)
  ;

  private final BiFunction<@Nullable Object, @Nullable Object, Boolean> function;

  private static final List<Class<?>> numberTypePrecedence = List.of(
    Double.class, Float.class, Long.class, Integer.class
  );

  public boolean apply(@Nullable Object a, @Nullable Object b) {
    if (a == null)
       a = 0;

    if (b == null)
      b = 0;

    // Cannot compare properly, at least do a string compare
    if (!(numberTypePrecedence.contains(a.getClass()) || numberTypePrecedence.contains(b.getClass())))
      return String.valueOf(a.getClass()).equals(String.valueOf(b.getClass()));

    // Decide on common class type in order to not loose any information
    Class<?> targetClass = numberTypePrecedence.get(
      Math.max(
        numberTypePrecedence.indexOf(a.getClass()),
        numberTypePrecedence.indexOf(a.getClass())
      )
    );

    // Lift the lower of the two up
    try {
      if (!targetClass.isInstance(a))
        a = targetClass.getConstructor(String.class).newInstance(String.valueOf(a));

      if (!targetClass.isInstance(b))
        b = targetClass.getConstructor(String.class).newInstance(String.valueOf(b));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return function.apply(a, b);
  }

  private static int invokeComparable(Object a, Object b) {
    try {
      return (int) Comparable.class.getMethod("compareTo", Object.class).invoke(a, b);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }
}
