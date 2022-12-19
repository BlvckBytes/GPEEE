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
import me.blvckbytes.gpeee.functions.FunctionJarLoader;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.functions.std.*;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import me.blvckbytes.gpeee.interpreter.Interpreter;
import me.blvckbytes.gpeee.interpreter.StandardValueInterpreter;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.logging.NullLogger;
import me.blvckbytes.gpeee.parser.Optimizer;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class GPEEE implements IExpressionEvaluator, IStandardFunctionRegistry {

  public static final IValueInterpreter STD_VALUE_INTERPRETER;
  public static final IEvaluationEnvironment EMPTY_ENVIRONMENT;

  static {
    STD_VALUE_INTERPRETER = new StandardValueInterpreter();
    EMPTY_ENVIRONMENT = createEmptyEnvironment();
  }

  private final Map<String, AStandardFunction> standardFunctions;
  private final Map<Class<?>, Object> dependencyMap;

  private final Parser parser;
  private final Interpreter interpreter;
  private final Optimizer optimizer;
  private final ILogger logger;
  private final FunctionJarLoader jarLoader;

  public GPEEE(@Nullable ILogger logger) {
    this.logger = logger == null ? new NullLogger() : logger;

    this.parser = new Parser(this.logger);
    this.interpreter = new Interpreter(this.logger, this);
    this.optimizer = new Optimizer(this.logger, this.interpreter, this);
    this.jarLoader = new FunctionJarLoader();

    this.dependencyMap = new HashMap<>();
    this.standardFunctions = new HashMap<>();

    this.loadStandardFunctions();
  }

  @Override
  public AExpression parseString(String input) throws AEvaluatorError {
    return parser.parse(new Tokenizer(this.logger, input));
  }

  @Override
  public AExpression optimizeExpression(AExpression expression) throws AEvaluatorError {
    return optimizer.optimizeAST(expression);
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

  @Override
  public void register(String name, AStandardFunction function) {
    this.standardFunctions.put(name, function);
  }

  @Override
  public @Nullable AStandardFunction lookup(String name) {
    return this.standardFunctions.get(name);
  }

  /**
   * Loads all locally available standard functions into the local registry
   */
  private void loadStandardFunctions() {
    new IterCatFunction().registerSelf(this);
    new StrFunction().registerSelf(this);
    new KeyFunction().registerSelf(this);
    new ValueFunction().registerSelf(this);
    new BoolFunction().registerSelf(this);
    new ListFunction().registerSelf(this);
  }

  /**
   * Call all available standard functions in order to register themselves on this interpreter
   * @param functionFolder Folder to look for standard functions in
   */
  public void importStandardFunctions(@Nullable String functionFolder) {
    if (functionFolder == null)
      return;

    try {
      File folder = new File(functionFolder);
      File[] contents = folder.listFiles();

      if (contents == null) {
        logger.logError("Could not list files in function folder", null);
        return;
      }

      // Loop all available files in that folder
      for (File file : contents) {

        // Not a jar file, skip
        if (!(file.isFile() && file.getName().endsWith(".jar")))
          continue;

        try {
          AStandardFunction function = jarLoader.loadFunctionFromFile(file);

          if (function == null) {
            logger.logError("Could not load function at " + file.getAbsolutePath(), null);
            continue;
          }

          function.registerSelf(this);
        } catch (Exception e) {
          logger.logError("Could not load function at " + file.getAbsolutePath(), e);
        }
      }
    } catch (Exception e) {
      logger.logError("Could not load functions from folder", e);
    }
  }

  private static IEvaluationEnvironment createEmptyEnvironment() {
    return new IEvaluationEnvironment() {
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
        return Map.of();
      }

      @Override
      public IValueInterpreter getValueInterpreter() {
        return STD_VALUE_INTERPRETER;
      }

      @Override
      public IDependencyRegistry getDependencyRegistry() {
        return new IDependencyRegistry() {

          @Override
          public <T> void registerDependency(Class<? extends T> type, T instance) {}

          @Override
          public <T> @Nullable T tryLookupDependency(Class<? extends T> type) {
            return null;
          }
        };
      }
    };
  }
}
