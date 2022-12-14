package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Main {

  public static void main(String[] args) {
    try {
      Consumer<String> debugLogger = message -> System.out.println("[DEBUG]: " + message);
      GPEEE evaluator = new GPEEE(debugLogger);

      String input = """
        "my prefix: " & iter_cat(my_items, (it, ind) -> "(" & ind & " -> " & it & ")", "|", "no items available")
      """.trim();

      AExpression expression = evaluator.parseString(input);

      System.out.println(expression.stringify("  ", 0));

      System.out.println("expression=" + expression.expressionify());

      IEvaluationEnvironment env = new IEvaluationEnvironment() {

        @Override
        public Map<String, FExpressionFunction> getFunctions() {
          // iter_cat(items, (it, ind) -> (..), "separator", "no items fallback")
          return Map.of(
            "iter_cat", args -> {
              // Not enough arguments provided
              if (args.size() < 3)
                return ExpressionValue.NULL;

              @Nullable FExpressionFunction formatter = args.get(1).asFunction();

              // Needs to provide a function as the mapper parameter
              if (formatter == null)
                return ExpressionValue.NULL;

              List<ExpressionValue> items = args.get(0).interpretAsList();
              String separator = args.get(2).interpretAsString();

              // Loop all items
              StringBuilder result = new StringBuilder();
              for (int i = 0; i < items.size(); i++) {
                result.append(i == 0 ? "" : separator).append(
                  formatter
                    .apply(List.of(items.get(i), ExpressionValue.fromInteger(i)))
                    .interpretAsString()
                );
              }

              // No items available but a fallback string has been supplied
              if (items.size() == 0 && args.size() >= 4)
                return ExpressionValue.fromString(args.get(3).interpretAsString());

              // Respond with the built-up result
              return ExpressionValue.fromString(result.toString());
            }
          );
        }

        @Override
        public Map<String, Supplier<ExpressionValue>> getLiveVariables() {
          return Map.of();
        }

        @Override
        public Map<String, ExpressionValue> getStaticVariables() {
          Map<String, ExpressionValue> vars = new HashMap<>();

          vars.put("my_items", ExpressionValue.fromListAutoWrap(List.of(
            1, 3, 5, 21, 49
          )));

          vars.put("no_items", ExpressionValue.EMPTY_LIST);

          return vars;
        }
      };

      System.out.println("result=" + evaluator.evaluateExpression(expression, env));

      System.out.println("Done!");
    } catch (AEvaluatorError err) {
      System.err.println(err.getMessage());
      throw new IllegalStateException();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
