package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.functions.IExpressionFunction;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.expression.AExpression;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Main {

  public static void main(String[] args) {
    try {
      Consumer<String> debugLogger = message -> System.out.println("[DEBUG]: " + message);
      GPEEE evaluator = new GPEEE(debugLogger);

      String input = "foreach(items, (index) -> \"index=\" & index, \"\\n\")";

      AExpression expression = evaluator.parseString(input);

      System.out.println(expression.stringify("  ", 0));

      System.out.println("expression=" + expression.expressionify());

      IEvaluationEnvironment env = new IEvaluationEnvironment() {

        @Override
        public Map<String, IExpressionFunction> getFunctions() {
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

      System.out.println("result=" + evaluator.evaluateExpression(expression, env));

      System.out.println("Done!");
    } catch (AEvaluatorError err) {
      System.out.println(err.getMessage());
      throw new IllegalStateException();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
