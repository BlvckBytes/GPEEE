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

package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.IDependencyRegistry;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.error.*;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.functions.std.IfFunction;
import me.blvckbytes.gpeee.logging.DebugLogLevel;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.FunctionJarLoader;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.functions.std.AStandardFunction;
import me.blvckbytes.gpeee.functions.std.IterCatFunction;
import me.blvckbytes.gpeee.parser.ComparisonOperation;
import me.blvckbytes.gpeee.parser.EqualityOperation;
import me.blvckbytes.gpeee.parser.MathOperation;
import me.blvckbytes.gpeee.parser.expression.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Interpreter implements IStandardFunctionRegistry {

  private final Map<String, AStandardFunction> standardFunctions;
  private final ILogger logger;
  private final FunctionJarLoader loader;

  public Interpreter(ILogger logger, @Nullable String functionFolder) {
    this.logger = logger;
    this.standardFunctions = new HashMap<>();
    this.loader = new FunctionJarLoader();

    this.importStandardFunctions(functionFolder);
  }

  public Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    if (expression == null)
      return null;

    logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating " + expression.getClass().getSimpleName() + ": " + expression.expressionify());

    IValueInterpreter valueInterpreter = environment.getValueInterpreter();

    /////////////////////// Static Values ///////////////////////

    if (expression instanceof LongExpression) {
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate long value");
      return ((LongExpression) expression).getNumber();
    }

    if (expression instanceof DoubleExpression) {
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate double value");
      return ((DoubleExpression) expression).getValue();
    }

    if (expression instanceof LiteralExpression) {
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate literal value");
      return ((LiteralExpression) expression).getValue();
    }

    if (expression instanceof StringExpression) {
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate string value");
      return ((StringExpression) expression).getValue();
    }

    ////////////////////// Variable Values //////////////////////

    if (expression instanceof IdentifierExpression) {
      String symbol = ((IdentifierExpression) expression).getSymbol();

      logger.logDebug(DebugLogLevel.INTERPRETER, "Looking up variable " + symbol);

      Object value = environment.getStaticVariables().get(symbol);
      if (value != null) {
        logger.logDebug(DebugLogLevel.INTERPRETER, "Resolved static variable value: " + value);
        return value;
      }

      Supplier<Object> valueSupplier = environment.getLiveVariables().get(symbol);
      if (valueSupplier != null) {
        value = valueSupplier.get();

        if (value != null) {
          logger.logDebug(DebugLogLevel.INTERPRETER, "Resolved dynamic variable value: " + value);
          return value;
        }
      }

      throw new UndefinedVariableError(((IdentifierExpression) expression));
    }

    ///////////////////////// Functions /////////////////////////

    if (expression instanceof FunctionInvocationExpression) {
      FunctionInvocationExpression functionExpression = (FunctionInvocationExpression) expression;

      // Try to look up the target function in the standard function table first
      AExpressionFunction function = standardFunctions.get(functionExpression.getName().getSymbol());
      if (function == null) {

        // Now, try to look up the target function in the environment's function table
        function = environment.getFunctions().get(functionExpression.getName().getSymbol());
        if (function == null)
          throw new UndefinedFunctionError(functionExpression.getName());
      }

      logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating arguments of function invocation " + functionExpression.getName().getSymbol());

      @Nullable List<ExpressionFunctionArgument> argDefinitions = function.getArguments();

      List<Object> arguments = new ArrayList<>();

      // Argument definitions are available, fill up the argument list
      // with null values to match the number of requested arguments
      if (argDefinitions != null) {
        while (arguments.size() < argDefinitions.size())
          arguments.add(null);
      }

      boolean encounteredNamedArgument = false;
      int debugArgCounter = 0, nonNamedArgCounter = 0;

      // Evaluate and collect all arguments
      for (Tuple<AExpression, @Nullable IdentifierExpression> argument : functionExpression.getArguments()) {
        logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating argument " + (++debugArgCounter));

        Object argumentValue = evaluateExpression(argument.getA(), environment);

        // Argument definitions are available and this argument has a name attached
        if (argDefinitions != null && argument.getB() != null) {
          encounteredNamedArgument = true;

          // Look through all definitions to find a match
          boolean foundMatch = false;
          for (int i = 0; i < argDefinitions.size(); i++) {
            ExpressionFunctionArgument argDefinition = argDefinitions.get(i);
            String name = argument.getB().getSymbol();

            // Argument's identifier is not matching the arg definition name
            if (!argDefinition.getName().equalsIgnoreCase(name))
              continue;

            // Found a name match, set the value at that same index
            logger.logDebug(DebugLogLevel.INTERPRETER, "Matched named argument " + name + " to index " + i);
            arguments.set(i, argumentValue);
            foundMatch = true;
            break;
          }

          // This argument is mapped, continue
          if (foundMatch)
            continue;

          // Could not find a match for this named argument
          throw new UndefinedFunctionArgumentNameError(function, argument.getB());
        }

        // Encountered a non-named argument after encountering a named argument
        if (encounteredNamedArgument)
          throw new NonNamedFunctionArgumentError(argument.getA());

        // Evaluate and collect all arguments
        arguments.set(nonNamedArgCounter++, argumentValue);
      }

      // Let the function validate the arguments of it's invocation before actually performing the call
      function.validateArguments(functionExpression, environment.getValueInterpreter(), arguments);

      // Invoke and return that function's result
      Object result = function.apply(environment, arguments);
      logger.logDebug(DebugLogLevel.INTERPRETER, "Invoked function, result: " + result);
      return result;
    }

    if (expression instanceof CallbackExpression) {
      CallbackExpression callbackExpression = (CallbackExpression) expression;

      logger.logDebug(DebugLogLevel.INTERPRETER, "Setting up the java endpoint for a callback expression");

      // This lambda function will be called by java every time the callback is invoked
      return AExpressionFunction.makeUnchecked((env, args) -> {

        // Copy the static variable table and extend it below
        Map<String, Object> combinedVariables = new HashMap<>(environment.getStaticVariables());

        // Map all identifiers from the callback's signature to a matching java argument in sequence
        // If there are more arguments in the signature than provided by java, they'll just be set to null
        for (int i = 0; i < callbackExpression.getSignature().size(); i++) {
          String variableIdentifier = callbackExpression.getSignature().get(i).getSymbol();
          Object variableValue = i < args.size() ? args.get(i) : null;

          logger.logDebug(DebugLogLevel.INTERPRETER, "Adding " + variableIdentifier + "=" + variableValue + " to a callback's environment");
          combinedVariables.put(variableIdentifier, variableValue);
        }

        logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating a callback's body");

        // Callback expressions are evaluated within their own environment, which extends the current environment
        // by the additional variables coming from the arguments passed by the callback caller
        Object result = evaluateExpression(callbackExpression.getBody(), new IEvaluationEnvironment() {

          @Override
          public Map<String, AExpressionFunction> getFunctions() {
            return environment.getFunctions();
          }

          @Override
          public Map<String, Supplier<Object>> getLiveVariables() {
            return environment.getLiveVariables();
          }

          @Override
          public Map<String, Object> getStaticVariables() {
            return combinedVariables;
          }

          @Override
          public IValueInterpreter getValueInterpreter() {
            return environment.getValueInterpreter();
          }

          @Override
          public IDependencyRegistry getDependencyRegistry() {
            return environment.getDependencyRegistry();
          }
        });

        logger.logDebug(DebugLogLevel.INTERPRETER, "Callback result=" + result);
        return result;
      });
    }

    //////////////////// Binary Expressions /////////////////////

    if (expression instanceof BinaryExpression) {
      logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating LHS and RHS of a binary expression");

      Object lhs = evaluateExpression(((BinaryExpression) expression).getLhs(), environment);
      Object rhs = evaluateExpression(((BinaryExpression) expression).getRhs(), environment);

      if (expression instanceof MathExpression) {
        MathOperation operation = ((MathExpression) expression).getOperation();
        Object result;

        result = valueInterpreter.performMath(lhs, rhs, operation);
        logger.logDebug(DebugLogLevel.INTERPRETER, "Math Operation operation " + operation + " result: " + result);
        return result;
      }

      if (expression instanceof EqualityExpression) {
        EqualityOperation operation = ((EqualityExpression) expression).getOperation();
        boolean result;

        switch (operation) {
          case EQUAL:
            result = valueInterpreter.areEqual(lhs, rhs, false);
            break;

          case NOT_EQUAL:
            result = !valueInterpreter.areEqual(lhs, rhs, false);
            break;

          case EQUAL_EXACT:
            result = valueInterpreter.areEqual(lhs, rhs, true);
            break;

          case NOT_EQUAL_EXACT:
            result = !valueInterpreter.areEqual(lhs, rhs, true);
            break;

          default:
            result = false;
            break;
        }

        logger.logDebug(DebugLogLevel.INTERPRETER, "Equality Operation operation " + operation + " result: " + result);
        return result;
      }

      if (expression instanceof ComparisonExpression) {
        ComparisonOperation operation = ((ComparisonExpression) expression).getOperation();
        int comparisonResult = valueInterpreter.compare(lhs, rhs);
        boolean result;

        switch (operation) {
          case LESS_THAN:
            result = comparisonResult < 0;
            break;

          case GREATER_THAN:
            result = comparisonResult > 0;
            break;

          case LESS_THAN_OR_EQUAL:
            result = comparisonResult <= 0;
            break;

          case GREATER_THAN_OR_EQUAL:
            result = comparisonResult >= 0;
            break;

          default:
            result = false;
            break;
        }

        logger.logDebug(DebugLogLevel.INTERPRETER, "Comparison Operation operation " + operation + " result: " + result);
        return result;
      }

      if (expression instanceof ConjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) && valueInterpreter.asBoolean(rhs);
        logger.logDebug(DebugLogLevel.INTERPRETER, "Conjunction Operation result: " + result);
        return result;
      }

      if (expression instanceof DisjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) || valueInterpreter.asBoolean(rhs);
        logger.logDebug(DebugLogLevel.INTERPRETER, "Disjunction Operation result: " + result);
        return result;
      }

      if (expression instanceof ConcatenationExpression) {
        String result = valueInterpreter.asString(lhs) + valueInterpreter.asString(rhs);
        logger.logDebug(DebugLogLevel.INTERPRETER, "Concatenation Operation result: " + result);
        return result;
      }
    }

    ///////////////////// Unary Expressions /////////////////////

    if (expression instanceof UnaryExpression) {
      logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating input of a unary expression");

      Object input = evaluateExpression(((UnaryExpression) expression).getInput(), environment);

      if (expression instanceof FlipSignExpression) {
        Object result = -1 * (valueInterpreter.hasDecimalPoint(input) ? valueInterpreter.asDouble(input) : valueInterpreter.asLong(input));
        logger.logDebug(DebugLogLevel.INTERPRETER, "Flip Sign Operation result: " + result);
        return result;
      }

      if (expression instanceof InvertExpression) {
        boolean result = !valueInterpreter.asBoolean(input);
        logger.logDebug(DebugLogLevel.INTERPRETER, "Invert Operation result: " + result);
        return result;
      }
    }

    throw new IllegalStateException("Cannot parse unknown expression type " + expression.getClass());
  }

  @Override
  public void register(String name, AStandardFunction function) {
    this.standardFunctions.put(name.toLowerCase(), function);
  }

  /**
   * Call all available standard functions in order to register themselves on this interpreter
   * @param functionFolder Folder to look for standard functions in
   */
  private void importStandardFunctions(@Nullable String functionFolder) {
    new IterCatFunction().registerSelf(this);
    new IfFunction().registerSelf(this);

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
        if (!file.getName().endsWith(".jar"))
          continue;

        try {
          AStandardFunction function = loader.loadFunctionFromFile(file);

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
}