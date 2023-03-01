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

import lombok.Setter;
import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.functions.std.*;
import me.blvckbytes.gpeee.interpreter.*;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.logging.NullLogger;
import me.blvckbytes.gpeee.parser.Optimizer;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

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

  private final Parser parser;
  private final Interpreter interpreter;
  private final Optimizer optimizer;
  private final ILogger logger;

  @Setter
  private EvaluationEnvironmentBuilder baseEnvironment;

  public GPEEE(@Nullable ILogger logger) {
    this.logger = logger == null ? new NullLogger() : logger;

    this.parser = new Parser(this.logger);
    this.interpreter = new Interpreter(this.logger, this);
    this.optimizer = new Optimizer(this.logger, this.interpreter, this);

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
    if (this.baseEnvironment != null)
      environment = baseEnvironment.build(environment);
    return interpreter.evaluateExpression(expression, environment);
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
    new LenFunction().registerSelf(this);
    new ListOfFunction().registerSelf(this);
    new MapOfFunction().registerSelf(this);
    new SplitFunction().registerSelf(this);
    new PrintFunction().registerSelf(this);
    new TitleCaseFunction().registerSelf(this);
    new MapFunction().registerSelf(this);
    new DateFormatFunction().registerSelf(this);
    new LIndexFunction().registerSelf(this);
    new RIndexFunction().registerSelf(this);
    new SubstringFunction().registerSelf(this);
    new RangeFunction().registerSelf(this);
    new FlattenFunction().registerSelf(this);
  }

  private static IEvaluationEnvironment createEmptyEnvironment() {
    return new IEvaluationEnvironment() {
      @Override
      public Map<String, AExpressionFunction> getFunctions() {
        return Map.of();
      }

      @Override
      public Map<String, Supplier<?>> getLiveVariables() {
        return Map.of();
      }

      @Override
      public Map<String, ?> getStaticVariables() {
        return Map.of();
      }

      @Override
      public IValueInterpreter getValueInterpreter() {
        return STD_VALUE_INTERPRETER;
      }
    };
  }
}
