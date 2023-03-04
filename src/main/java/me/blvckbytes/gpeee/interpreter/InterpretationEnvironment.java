package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.functions.AExpressionFunction;

import java.util.HashMap;
import java.util.Map;

public class InterpretationEnvironment {

  private final Map<String, AExpressionFunction> functions;
  private final Map<String, Object> variables;

  public InterpretationEnvironment() {
    this.functions = new HashMap<>();
    this.variables = new HashMap<>();
  }

  public Map<String, AExpressionFunction> getFunctions() {
    return functions;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }
}
