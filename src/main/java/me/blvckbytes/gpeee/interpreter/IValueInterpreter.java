package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.parser.MathOperation;
import org.jetbrains.annotations.Nullable;

public interface IValueInterpreter {

  boolean asBoolean(@Nullable Object value);

  long asLong(@Nullable Object value);

  double asDouble(@Nullable Object value);

  String asString(@Nullable Object value);

  boolean hasDecimalPoint(@Nullable Object value);

  int compare(@Nullable Object a, @Nullable Object b);

  boolean areEqual(@Nullable Object a, @Nullable Object b, boolean strict);

  Object performMath(@Nullable Object a, @Nullable Object b, MathOperation operation);

}
