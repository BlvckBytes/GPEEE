package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.functions.FExpressionFunction;

import java.util.Map;
import java.util.function.Supplier;

public interface IEvaluationEnvironment {

  /**
   * Mapping identifiers to available functions which an expression may invoke
   */
  Map<String, FExpressionFunction> getFunctions();

  /**
   * Mapping identifiers to available live variables which an expression may resolve
   */
  Map<String, Supplier<Object>> getLiveVariables();

  /**
   * Mapping identifiers to available static variables which an expression may resolve
   */
  Map<String, Object> getStaticVariables();

  /**
   * Get the value interpreter used to interpret values when doing any kind of
   * operation on them which they'd usually not support naturally. Provide null
   * in order to use the standard interpreter.
   *
   * Most of the time, you'd want to provide {@link me.blvckbytes.gpeee.GPEEE#STD_VALUE_INTERPRETER}
   */
  IValueInterpreter getValueInterpreter();

}
