package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
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
      public Map<String, Supplier<ExpressionValue>> getLiveVariables() {
        return Map.of();
      }

      @Override
      public Map<String, ExpressionValue> getStaticVariables() {
        return Map.of();
      }
    };
  }

  @Test
  public void test() {
    assertExpression("3^-(1/2)", 1.0 / Math.sqrt(3));
    assertExpression("2^(1/2)", Math.sqrt(2));
  }

  private void assertExpression(String expression, Object result) {
    ExpressionValue value = evaluator.evaluateExpression(evaluator.parseString(expression), environment);

    switch (value.getType()) {
      case INTEGER:
        assertTrue(result instanceof Integer);
        assertEquals(result, value.asInteger());
        break;

      case DOUBLE:
        assertTrue(result instanceof Double);
        Double d = value.asDouble();
        assertNotNull(d);
        assertTrue(Math.abs(((Double) result) - d) <= MAX_DOUBLE_DELTA);
        break;

      case STRING:
        assertTrue(result instanceof String);
        assertEquals(result, value.asString());
        break;

      default:
        throw new IllegalStateException("Not yet implemented");
    }
  }
}
