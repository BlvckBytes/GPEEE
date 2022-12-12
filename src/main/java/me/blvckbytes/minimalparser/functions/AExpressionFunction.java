package me.blvckbytes.minimalparser.functions;

import me.blvckbytes.minimalparser.IValueInterpreter;

public abstract class AExpressionFunction {

  public abstract Object apply(Object[] args, IValueInterpreter valueInterpreter);

  protected Object indexOrNull(Object[] args, int index) {
    return index < args.length ? args[index] : null;
  }
}
