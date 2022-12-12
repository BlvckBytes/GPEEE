package me.blvckbytes.minimalparser;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Optional;

public interface IValueInterpreter {

  boolean isTruthy(@Nullable Object input);

  Optional<BigDecimal> tryParseNumber(@Nullable Object input);


}
