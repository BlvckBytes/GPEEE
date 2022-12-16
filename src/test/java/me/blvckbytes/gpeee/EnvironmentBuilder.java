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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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

  public @Nullable Object getVariable(String identifier) {
    Object value = this.staticVariables.get(identifier);

    if (value == null) {
      Supplier<Object> supplier = this.liveVariables.get(identifier);

      if (supplier != null)
        value = supplier.get();
    }

    return value;
  }

  public String stringify(Object value) {
    if (value == null)
      return "<null>";

    if (value instanceof String)
      return ((String) value);

    // Transform a map to a list of it's entries
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      value = new ArrayList<>(map.entrySet());
    }

    // Stringify map entries
    if (value instanceof Map.Entry) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
      return "(" + stringify(entry.getKey()) + " -> " + stringify(entry.getValue()) + ")";
    }

    // Stringify collections, arrays are also considered to be collections in this language
    if (value instanceof Collection<?> || value.getClass().isArray()) {
      StringBuilder result = new StringBuilder();

      if (value.getClass().isArray()) {
        for (int i = 0; i < Array.getLength(value); i++)
          result.append(i == 0 ? "" : ", ").append(stringify(Array.get(value, i)));
      }

      else {
        int i = 0;
        for (Object item : ((Collection<?>) value)) {
          result.append(i == 0 ? "" : ", ").append(stringify(item));
          i++;
        }
      }

      return "[" + result + "]";
    }

    return value.toString();
  }

  public Object[] stringifiedPermutations(String identifier) {
    Object value = getVariable(identifier);

    // Transform an array to a list
    if (value != null && value.getClass().isArray()) {
      List<Object> list = new ArrayList<>();
      for (int i = 0; i < Array.getLength(value); i++)
        list.add(Array.get(value, i));
      value = list;
    }

    // Transform a map to a list of it's entries
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      value = new ArrayList<>(map.entrySet());
    }

    // Stringify all permutations of this list
    if (value instanceof List)
      return generatePerm((List<?>) value).stream().map(this::stringify).toArray(String[]::new);

    return new Object[] { stringify(value) };
  }

  public List<List<Object>> generatePerm(List<?> original) {
    if (original.isEmpty())
      return List.of(List.of());

    Object firstElement = original.remove(0);
    List<List<Object>> returnValue = new ArrayList<>();
    List<List<Object>> permutations = generatePerm(original);

    for (List<Object> smallerPermutated : permutations) {
      for (int index = 0; index <= smallerPermutated.size(); index++) {
        List<Object> temp = new ArrayList<>(smallerPermutated);
        temp.add(index, firstElement);
        returnValue.add(temp);
      }
    }

    return returnValue;
  }

  public void launch(Consumer<IExpressionResultValidator> validator) {
    IEvaluationEnvironment env = this.buildEnvironment();

    validator.accept(new IExpressionResultValidator() {

      @Override
      public void validate(String expression, Object[] results) throws AssertionError {
        Object value = evaluator.evaluateExpression(evaluator.parseString(expression), env);

        AssertionError lastThrow = null;
        for (Object result : results) {

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
            if (!(Math.abs(((Double) result) - ((Double) value)) <= MAX_DOUBLE_DELTA)) {
              try {
                assertEquals(result, value);
              } catch (AssertionError e) {
                lastThrow = e;
                continue;
              }
            }

            // Success exit
            return;
          }

          // Compare everything else

          try {
            assertEquals(result, value);
          } catch (AssertionError e) {
            lastThrow = e;
            continue;
          }

          // Success exit
          return;
        }

        // There was no success exit but an error has been thrown, rethrow
        if (lastThrow != null)
          throw lastThrow;
      }

      @Override
      public void validate(String expression, Object result) throws AssertionError {
        validate(expression, new Object[] { result });
      }

      @Override
      public void validateThrows(String expression, Class<? extends RuntimeException> error) throws AssertionError {
        assertThrows(error, () -> evaluator.evaluateExpression(evaluator.parseString(expression), env));
      }
    });

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
