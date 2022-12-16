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
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpressionTests {

  private static final double MAX_DOUBLE_DELTA = .001;

  private GPEEE evaluator;
  private IEvaluationEnvironment environment;

  @Before
  public void setupGPEEE() {
    evaluator = new GPEEE(null, null);
    environment = new IEvaluationEnvironment() {
      @Override
      public Map<String, AExpressionFunction> getFunctions() {
        return Map.of();
      }

      @Override
      public Map<String, Supplier<Object>> getLiveVariables() {
        return Map.of();
      }

      @Override
      public Map<String, Object> getStaticVariables() {
        return Map.of();
      }

      @Override
      public IValueInterpreter getValueInterpreter() {
        return GPEEE.STD_VALUE_INTERPRETER;
      }
    };
  }

  @Test
  public void test() {
    assertExpression("3^-(1/2)", 1.0 / Math.sqrt(3));
    assertExpression("2^(1/2)", Math.sqrt(2));
  }

  private void assertExpression(String expression, Object result) {
    Object value = evaluator.evaluateExpression(evaluator.parseString(expression), environment);

    if (value instanceof Integer)
      assertEquals(result, value);

    else if (value instanceof Double)
      assertTrue(Math.abs(((Double) result) - ((Double) value)) <= MAX_DOUBLE_DELTA);

    else if (value instanceof String)
      assertEquals(result, value);

    else
      throw new IllegalStateException("Not yet implemented");
  }
}
