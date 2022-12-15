package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ExpressionTests {

  private static final double MAX_DOUBLE_DELTA = .001;

  private GPEEE evaluator;
  private IEvaluationEnvironment environment;

  @Before
  public void setupGPEEE() {
    evaluator = new GPEEE(null);
    environment = new IEvaluationEnvironment() {
      @Override
      public Map<String, FExpressionFunction> getFunctions() {
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
      public @Nullable IValueInterpreter getValueInterpreter() {
        return null;
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
