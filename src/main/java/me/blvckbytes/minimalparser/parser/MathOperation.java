package me.blvckbytes.minimalparser.parser;

import lombok.AllArgsConstructor;
import me.blvckbytes.minimalparser.IValueInterpreter;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.function.BiFunction;

@AllArgsConstructor
public enum MathOperation {
  ADDITION(BigDecimal::add),
  SUBTRACTION(BigDecimal::subtract),
  MULTIPLICATION(BigDecimal::multiply),
  DIVISION(BigDecimal::divide),
  ;

  private final BiFunction<BigDecimal, BigDecimal, BigDecimal> function;

  public BigDecimal apply(@Nullable Object a, @Nullable Object b, IValueInterpreter interpreter) {
    BigDecimal numberA = interpreter.tryParseNumber(a).orElse(null);
    BigDecimal numberB = interpreter.tryParseNumber(b).orElse(null);

    if (numberA == null)
      numberA = new BigDecimal(0);

    if (numberB == null)
      numberB = new BigDecimal(0);

    return function.apply(numberA, numberB);
  }
}
