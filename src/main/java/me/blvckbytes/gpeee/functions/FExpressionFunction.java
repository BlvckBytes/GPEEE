package me.blvckbytes.gpeee.functions;

import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;

import java.util.List;

@FunctionalInterface
public interface FExpressionFunction {

  /**
   * Called whenever a function call to the registered corresponding
   * identifier is performed within an expression
   * @param environment A reference to the current environment
   * @param args Arguments supplied by the invocation
   * @return Return value of this function
   */
  Object apply(IEvaluationEnvironment environment, List<Object> args);

}
