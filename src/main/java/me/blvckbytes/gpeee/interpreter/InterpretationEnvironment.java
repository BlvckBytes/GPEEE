package me.blvckbytes.gpeee.interpreter;

import lombok.Getter;
import me.blvckbytes.gpeee.functions.AExpressionFunction;

import java.util.HashMap;
import java.util.Map;

@Getter
public class InterpretationEnvironment {

  private final Map<String, AExpressionFunction> functions;
  private final Map<String, Object> variables;

  public InterpretationEnvironment() {
    this.functions = new HashMap<>();
    this.variables = new HashMap<>();
  }
}
