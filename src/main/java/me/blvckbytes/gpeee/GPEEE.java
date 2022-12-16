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
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import me.blvckbytes.gpeee.interpreter.Interpreter;
import me.blvckbytes.gpeee.interpreter.StandardValueInterpreter;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.logging.NullLogger;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GPEEE implements IExpressionEvaluator {

  public static final IValueInterpreter STD_VALUE_INTERPRETER = new StandardValueInterpreter();

  private final Map<Class<?>, Object> dependencyMap;
  private final Parser parser;
  private final Interpreter interpreter;
  private final ILogger logger;

  public GPEEE(@Nullable ILogger logger, @Nullable String functionFolder) {
    this.logger = logger == null ? new NullLogger() : logger;
    this.parser = new Parser(this.logger);
    this.interpreter = new Interpreter(this.logger, functionFolder);
    this.dependencyMap = new HashMap<>();
  }

  @Override
  public AExpression parseString(String input) throws AEvaluatorError {
    return parser.parse(new Tokenizer(this.logger, input));
  }

  @Override
  public Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    return interpreter.evaluateExpression(expression, environment);
  }

  @Override
  public<T> void registerDependency(Class<? extends T> type, T instance) {
    this.dependencyMap.put(type, instance);
  }

  @Override
  @SuppressWarnings("unchecked")
  public<T> @Nullable T tryLookupDependency(Class<? extends T> type) {
    Object result = this.dependencyMap.get(type);

    // Just making sure!
    if (!type.isAssignableFrom(result.getClass())) {
      this.dependencyMap.remove(type);
      return null;
    }

    return (T) result;
  }
}
