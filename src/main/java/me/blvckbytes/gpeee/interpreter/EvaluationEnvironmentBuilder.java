package me.blvckbytes.gpeee.interpreter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.functions.AExpressionFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EvaluationEnvironmentBuilder {

  private IValueInterpreter valueInterpreter;
  private final Map<String, Object> staticVariables;
  private final Map<String, Supplier<?>> liveVariables;
  private final Map<String, AExpressionFunction> functions;

  public EvaluationEnvironmentBuilder() {
    this.valueInterpreter = GPEEE.STD_VALUE_INTERPRETER;

    this.staticVariables = new HashMap<>();
    this.liveVariables = new HashMap<>();
    this.functions = new HashMap<>();
  }

  public EvaluationEnvironmentBuilder withValueInterpreter(IValueInterpreter valueInterpreter) {
    this.valueInterpreter = valueInterpreter;
    return this;
  }

  public EvaluationEnvironmentBuilder withStaticVariable(String identifier, Object value) {
    this.staticVariables.put(identifier, value);
    return this;
  }

  public EvaluationEnvironmentBuilder withLiveVariable(String identifier, Supplier<?> value) {
    this.liveVariables.put(identifier, value);
    return this;
  }

  public EvaluationEnvironmentBuilder withFunction(String identifier, AExpressionFunction function) {
    this.functions.put(identifier, function);
    return this;
  }

  public EvaluationEnvironmentBuilder duplicate() {
    return new EvaluationEnvironmentBuilder(
      valueInterpreter,
      new HashMap<>(staticVariables),
      new HashMap<>(liveVariables),
      new HashMap<>(functions)
    );
  }

  public IEvaluationEnvironment build() {
    return new IEvaluationEnvironment() {

      @Override
      public Map<String, AExpressionFunction> getFunctions() {
        return functions;
      }

      @Override
      public Map<String, Supplier<?>> getLiveVariables() {
        return liveVariables;
      }

      @Override
      public Map<String, Object> getStaticVariables() {
        return staticVariables;
      }

      @Override
      public IValueInterpreter getValueInterpreter() {
        return valueInterpreter;
      }
    };
  }
}
