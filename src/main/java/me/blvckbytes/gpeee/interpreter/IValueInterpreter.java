package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.parser.MathOperation;

public interface IValueInterpreter {

  boolean asBoolean(Object value);

  long asLong(Object value);

  double asDouble(Object value);

  String asString(Object value);

  boolean hasDecimalPoint(Object value);

  int compare(Object a, Object b);

  boolean areEqual(Object a, Object b, boolean strict);

  Object performMath(Object a, Object b, MathOperation operation);

}
