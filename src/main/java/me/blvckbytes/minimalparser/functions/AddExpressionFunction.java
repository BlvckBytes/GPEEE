package me.blvckbytes.minimalparser.functions;

import me.blvckbytes.minimalparser.IValueInterpreter;

import java.util.Optional;

public class AddExpressionFunction extends AExpressionFunction {

  @Override
  public Object apply(Object[] args, IValueInterpreter valueInterpreter) {
    Optional<Object> numberA = valueInterpreter.tryParseNumber(indexOrNull(args, 0));
    Optional<Object> numberB = valueInterpreter.tryParseNumber(indexOrNull(args, 1));

    // TODO: Implement
    return -1;
  }
}
