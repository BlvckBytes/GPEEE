package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
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
                return null;

              @Nullable FExpressionFunction formatter = (FExpressionFunction) args.get(1);

              // Needs to provide a function as the mapper parameter
              if (formatter == null)
                return null;

              List<?> items = (List<?>) args.get(0);
              String separator = (String) args.get(2);

              // Loop all items
              StringBuilder result = new StringBuilder();
              for (int i = 0; i < items.size(); i++) {
                result.append(i == 0 ? "" : separator).append(
                  formatter.apply(List.of(items.get(i), i))
                );
              }

              // No items available but a fallback string has been supplied
              if (items.size() == 0 && args.size() >= 4)
                return args.get(3);

              // Respond with the built-up result
              return result.toString();
            }
          );
        }

        @Override
        public Map<String, Supplier<Object>> getLiveVariables() {
          return Map.of();
        }

        @Override
        public Map<String, Object> getStaticVariables() {
          Map<String, Object> vars = new HashMap<>();

          vars.put("my_items", List.of(1, 3, 5, 21, 49));
          vars.put("no_items", List.of());

          return vars;
        }

        @Override
        public @Nullable IValueInterpreter getValueInterpreter() {
          return null;
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
