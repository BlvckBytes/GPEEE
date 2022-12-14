package me.blvckbytes.minimalparser.functions;

import me.blvckbytes.minimalparser.interpreter.ExpressionValue;

@FunctionalInterface
public interface IExpressionFunction {

  /**
   * Called whenever a function call to the registered corresponding
   * identifier is performed within an expression
   * @param args Arguments supplied by the invocation
   * @return Return value of this function
   */
  ExpressionValue apply(ExpressionValue[] args);

}
