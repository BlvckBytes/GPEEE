package me.blvckbytes.minimalparser.functions;

import me.blvckbytes.minimalparser.IValueInterpreter;

public class IfExpressionFunction extends AExpressionFunction {

  @Override
  public Object apply(Object[] args, IValueInterpreter valueInterpreter) {
    return valueInterpreter.isTruthy(indexOrNull(args, 0)) ? indexOrNull(args, 1) : indexOrNull(args, 2);
  }
}
