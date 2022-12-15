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

import me.blvckbytes.gpeee.error.AEvaluatorError;
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

  // TODO:
  /*
    Create some kind of utility-set to properly interface between expressions and java without having
    to perform a thousand checks for null or for I/O types.

    A function builder would be cool (but how would that look like?)

    Come up with an easy-to-use testing environment and create a hell load of tests for all kind of cases

    Run tests on the github repo
   */

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
    }
    catch (AEvaluatorError e) {
      System.err.println(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
