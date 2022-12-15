package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import me.blvckbytes.gpeee.parser.expression.AExpression;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Main {

  public static void main(String[] args) {
    try {
      URL resource = Thread.currentThread().getContextClassLoader().getResource("input.txt");

      if (resource == null)
        throw new IllegalStateException();

      String input = String.join("\n", Files.readAllLines(Paths.get(resource.toURI()), StandardCharsets.UTF_8));
      IDebugLogger debugLogger = (level, message) -> System.out.println("[DEBUG] [" + level + "]: " + message);
      GPEEE evaluator = new GPEEE(debugLogger);

      AExpression expression = evaluator.parseString(input);

      System.out.println(expression.stringify("  ", 0));
      System.out.println("expression=" + expression.expressionify());

      IEvaluationEnvironment env = new IEvaluationEnvironment() {

        @Override
        public Map<String, FExpressionFunction> getFunctions() {
          // iter_cat(items, (it, ind) -> (..), "separator", "no items fallback")
          return Map.of(
            "iter_cat", (env, args) -> {
              // Invalid call: Not enough arguments provided
              if (args.size() < 3)
                return null;

              // Invalid call: Cannot iterate over non-collections
              if (!(args.get(0) instanceof Collection))
                return null;

              // Invalid call: Cannot invoke a non-function type
              if (!(args.get(1) instanceof FExpressionFunction))
                return null;

              FExpressionFunction formatter = (FExpressionFunction) args.get(1);

              Collection<?> items = (Collection<?>) args.get(0);
              String separator = env.getValueInterpreter().asString(args.get(2));

              // Loop all items
              StringBuilder result = new StringBuilder();

              int c = 0;
              for (Object item : items) {
                result.append(result.length() == 0 ? "" : separator).append(
                  formatter.apply(env, List.of(item, c++))
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
        public IValueInterpreter getValueInterpreter() {
          return GPEEE.STD_VALUE_INTERPRETER;
        }
      };

      System.out.println("result=" + evaluator.evaluateExpression(expression, env));

      System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
