package me.blvckbytes.minimalparser.functions;

import me.blvckbytes.minimalparser.IValueInterpreter;

public class EqualsExpressionFunction extends AExpressionFunction {

  @Override
  public Object apply(Object[] args, IValueInterpreter valueInterpreter) {
    String a = String.valueOf(indexOrNull(args, 0));
    String b = String.valueOf(indexOrNull(args, 1));

    if (valueInterpreter.isTruthy(indexOrNull(args, 2)))
      return a.equalsIgnoreCase(b);

    return a.equals(b);
  }
}
