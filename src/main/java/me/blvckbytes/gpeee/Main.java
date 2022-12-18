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
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import me.blvckbytes.gpeee.logging.DebugLogLevel;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Main {

  // TODO:
  /*
    FINISH TEST CASES!
    Come up with an easy-to-use testing environment and create a hell load of tests for all kind of cases

    Somehow linking live generated javadoc into the readme?

    Think about how functions in separate jars would be tested... Each in their own project with their own artifact when compiling?
    That would... work, I guess?

    // Test named parameter with callback: name=() -> <expr>

    if <expr> <expr> else <expr>
    maybe a member-operator? (really think this one through)

    more std functions: listof, mapof
   */

  public static void main(String[] args) {
    try {
      String input = String.join("\n", Files.readAllLines(Path.of(System.getProperty("user.home"), "Desktop/input.txt")));

      ILogger logger = new ILogger() {
        @Override
        public void logDebug(DebugLogLevel level, String message) {
          System.out.println("[DEBUG] [" + level + "]: " + message);
        }

        @Override
        public void logError(String message, @Nullable Exception error) {
          System.err.println(message);

          if (error != null)
            error.printStackTrace();
        }
      };

      GPEEE evaluator = new GPEEE(logger);

      long startTime = System.nanoTime();
      AExpression unoptimized = evaluator.parseString(input);
      long endTime = System.nanoTime();
      double parsingDuration = (endTime - startTime) / Math.pow(10, 6);

      input = Arrays.stream(input.split("\n"))
        .map(String::trim)
        .filter(line -> !(line.isBlank() || line.startsWith("#")))
        .findFirst()
        .orElseThrow();

      String unoptimizedExpr = unoptimized.expressionify();
      System.out.println("input=" + input);
      System.out.println("unoptimized=" + unoptimizedExpr);
      System.out.println(unoptimized.stringify("  ", 0));

      startTime = System.nanoTime();
      AExpression optimized = evaluator.optimizeExpression(unoptimized);
      endTime = System.nanoTime();
      double optimizingDuration = (endTime - startTime) / Math.pow(10, 6);

      System.out.println(optimized.stringify("  ", 0));
      System.out.println("optimized=" + optimized.expressionify());

      IEvaluationEnvironment env = new IEvaluationEnvironment() {

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
          Map<String, Object> vars = new HashMap<>();

          vars.put("my_items", List.of(1, 3, 5, 21, 49));
          vars.put("no_items", List.of());
          vars.put("my_number", 40);
          vars.put("my_map", Map.of(
            "red", "#FF0000",
            "green", "#00FF00",
            "blue", "#0000FF"
          ));

          return vars;
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

      startTime = System.nanoTime();
      Object result = evaluator.evaluateExpression(unoptimized, env);
      endTime = System.nanoTime();
      double evaluationDuration = (endTime - startTime) / Math.pow(10, 6);

      System.out.println("input=" + input);
      System.out.println("unoptimized=" + unoptimizedExpr);
      System.out.println("optimized=" + optimized.expressionify());
      System.out.println("result=" + result);
      System.out.println("Done! Evaluation " + evaluationDuration + "ms, optimization " + optimizingDuration + "ms, parsing " + parsingDuration + "ms");
    }
    catch (AEvaluatorError e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
