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
import me.blvckbytes.gpeee.parser.expression.AExpression;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class FullUseExample {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static void main(String[] args) {
    try {
      Logger logger = Logger.getGlobal();
      GPEEE evaluator = new GPEEE(logger);

      AExpression expr = evaluator.parseString("5 * 3 - 2 & \" Hello, world! \" & current_time");
      System.out.println("unoptimized expression: " + expr.expressionify());
      System.out.println("unoptimized AST: " + expr.stringify(" ", 0));

      expr = evaluator.optimizeExpression(expr);
      System.out.println("optimized expression: " + expr.expressionify());
      System.out.println("optimized AST: " + expr.stringify(" ", 0));

      IEvaluationEnvironment env = new IEvaluationEnvironment() {

        @Override
        public Map<String, AExpressionFunction> getFunctions() {
          // Register your functions here
          return Collections.emptyMap();
        }

        @Override
        public Map<String, Supplier<?>> getLiveVariables() {
          // Register your live variables here
          return Collections.singletonMap(
            "current_time", () -> DATE_FORMAT.format(new Date())
          );
        }

        @Override
        public Map<String, Object> getStaticVariables() {
          // Register your static variables here
          return Collections.emptyMap();
        }

        @Override
        public IValueInterpreter getValueInterpreter() {
          // It's advised to just use the standard value interpreter
          return GPEEE.STD_VALUE_INTERPRETER;
        }
      };

      // Prints: 13 Hello, world! 2022-12-21 18:29:47
      System.out.println("result=" + evaluator.evaluateExpression(expr, env));
    }
    catch (AEvaluatorError e) {
      // The message of an AEvaluatorError always contains an excerpt of the input expression
      // as well as a marker with an explanation below it, which is why it's recommended to
      // print it's message separately to not distort it's formatting
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
