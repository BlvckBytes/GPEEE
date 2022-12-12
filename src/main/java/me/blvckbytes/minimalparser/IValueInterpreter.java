package me.blvckbytes.minimalparser;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface IValueInterpreter {

  boolean isTruthy(@Nullable Object input);

  Optional<Object> tryParseNumber(@Nullable Object input);


}
