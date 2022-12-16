/*
 * MIT License
 *
 * Copyright (c) 2022 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class EnvironmentBuilder {

  private static final double MAX_DOUBLE_DELTA = .00001;

  private final Map<String, Object> staticVariables;
  private final Map<String, Supplier<Object>> liveVariables;
  private final Map<String, AExpressionFunction> functions;
  private final GPEEE evaluator;

  public EnvironmentBuilder() {
    this.evaluator = new GPEEE(null, null);
    this.staticVariables = new HashMap<>();
    this.liveVariables = new HashMap<>();
    this.functions = new HashMap<>();
  }

  public EnvironmentBuilder withStaticVariable(String identifier, Object value) {
    this.staticVariables.put(identifier, value);
    return this;
  }

  public EnvironmentBuilder withLiveVariable(String identifier, Supplier<Object> value) {
    this.liveVariables.put(identifier, value);
    return this;
  }

  public EnvironmentBuilder withFunction(String identifier, AExpressionFunction function) {
    this.functions.put(identifier, function);
    return this;
  }

  public void launch(Consumer<IExpressionResultValidator> validator) {
    IEvaluationEnvironment env = this.buildEnvironment();

    validator.accept(new IExpressionResultValidator() {

      @Override
      public void validate(String expression, Object result) throws AssertionError {
        Object value = evaluator.evaluateExpression(evaluator.parseString(expression), env);

        // Substitute variables marked by {{ and }}
        if (result instanceof String)
          result = substituteVariables((String) result);

        // Integers are longs in this language
        if (result instanceof Integer)
          result = ((Integer) result).longValue();

        // Floats are doubles in this language
        if (result instanceof Float)
          result = ((Float) result).doubleValue();

        // Result had no decimals but was a double, convert to long
        if (result instanceof Double) {
          Double dV = (Double) result;
          if (dV - dV.intValue() == 0)
            result = dV.longValue();
        }

        // Double comparison using max delta
        if (value instanceof Double && result instanceof Double) {
          if (!(Math.abs(((Double) result) - ((Double) value)) <= MAX_DOUBLE_DELTA))
            assertEquals(result, value);
          return;
        }

        // Compare everything else
        assertEquals(result, value);
      }

      @Override
      public void validateThrows(String expression, Class<? extends RuntimeException> error) throws AssertionError {
        assertThrows(error, () -> evaluator.evaluateExpression(evaluator.parseString(expression), env));
      }
    });
  }

  private String substituteVariables(String input) {
    for (Map.Entry<String, Object> staticVariable : staticVariables.entrySet())
      input = input.replace("{{" + staticVariable.getKey() + "}}", staticVariable.getValue().toString());

    for (Map.Entry<String, Supplier<Object>> liveVariable : liveVariables.entrySet())
      input = input.replace("{{" + liveVariable.getKey() + "}}", liveVariable.getValue().get().toString());

    return input;
  }

  private IEvaluationEnvironment buildEnvironment() {
    return new IEvaluationEnvironment() {
      @Override
      public Map<String, AExpressionFunction> getFunctions() {
        return functions;
      }

      @Override
      public Map<String, Supplier<Object>> getLiveVariables() {
        return liveVariables;
      }

      @Override
      public Map<String, Object> getStaticVariables() {
        return staticVariables;
      }

      @Override
      public IValueInterpreter getValueInterpreter() {
        return GPEEE.STD_VALUE_INTERPRETER;
      }

      @Override
      public IDependencyRegistry getDependencyRegistry() {
        return evaluator;
      }
    };
  }
}
