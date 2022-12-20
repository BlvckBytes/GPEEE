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

import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.error.*;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.logging.DebugLogLevel;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.parser.ComparisonOperation;
import me.blvckbytes.gpeee.parser.EqualityOperation;
import me.blvckbytes.gpeee.parser.MathOperation;
import me.blvckbytes.gpeee.parser.expression.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

public class Interpreter {

  private final ILogger logger;
  private final IStandardFunctionRegistry standardFunctionRegistry;

  public Interpreter(ILogger logger, IStandardFunctionRegistry standardFunctionRegistry) {
    this.logger = logger;
    this.standardFunctionRegistry = standardFunctionRegistry;
  }

  public Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    if (expression == null)
      return null;

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating " + expression.getClass().getSimpleName() + ": " + expression.expressionify());
    //#endif

    IValueInterpreter valueInterpreter = environment.getValueInterpreter();

    /////////////////////// Static Values ///////////////////////

    if (expression instanceof LongExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate long value");
      //#endif
      return ((LongExpression) expression).getNumber();
    }

    if (expression instanceof DoubleExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate double value");
      //#endif
      return ((DoubleExpression) expression).getValue();
    }

    if (expression instanceof LiteralExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate literal value");
      //#endif
      return ((LiteralExpression) expression).getValue();
    }

    if (expression instanceof StringExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Taking the immediate string value");
      //#endif
      return ((StringExpression) expression).getValue();
    }

    ////////////////////// Variable Values //////////////////////

    if (expression instanceof IdentifierExpression)
      return lookupVariable(environment, (IdentifierExpression) expression);

    ///////////////////////// Functions /////////////////////////

    if (expression instanceof FunctionInvocationExpression) {
      FunctionInvocationExpression functionExpression = (FunctionInvocationExpression) expression;
      String name = functionExpression.getName().getSymbol().toLowerCase(Locale.ROOT);

      // Try to look up the target function in the standard function table first
      AExpressionFunction function = this.standardFunctionRegistry.lookup(name);
      if (function == null) {

        // Now, try to look up the target function in the environment's function table
        function = environment.getFunctions().get(name);
        if (function == null)
          throw new UndefinedFunctionError(functionExpression.getName());
      }

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating arguments of function invocation " + functionExpression.getName().getSymbol());
      //#endif

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
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating argument " + (++debugArgCounter));
        //#endif

        Object argumentValue = evaluateExpression(argument.getA(), environment);

        // Argument definitions are available and this argument has a name attached
        if (argDefinitions != null && argument.getB() != null) {
          encounteredNamedArgument = true;

          // Look through all definitions to find a match
          boolean foundMatch = false;
          for (int i = 0; i < argDefinitions.size(); i++) {
            ExpressionFunctionArgument argDefinition = argDefinitions.get(i);
            String argName = argument.getB().getSymbol();

            // Argument's identifier is not matching the arg definition name
            if (!argDefinition.getName().equalsIgnoreCase(argName))
              continue;

            // Found a name match, set the value at that same index
            //#if mvn.project.property.production != "true"
            logger.logDebug(DebugLogLevel.INTERPRETER, "Matched named argument " + argName + " to index " + i);
            //#endif
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

        // No definitions provided, just add to the list (variadic of unchecked type)
        if (argDefinitions == null) {

          // If there are no definitions provided by the function, named arguments should throw
          // as they cannot be possibly matched with anything and should thus be omitted
          IdentifierExpression argNameExpression = argument.getB();
          if (argNameExpression != null)
            throw new UndefinedFunctionArgumentNameError(function, argNameExpression);

          arguments.add(argumentValue);
        }

        // Set at the next non-named index (before named can occur)
        else if (nonNamedArgCounter < arguments.size())
          arguments.set(nonNamedArgCounter++, argumentValue);
      }

      // Let the function validate the arguments of it's invocation before actually performing the call
      function.validateArguments(functionExpression, environment.getValueInterpreter(), arguments);

      // Invoke and return that function's result
      Object result = function.apply(environment, arguments);

      // Throw an exception based on the error description object, now that the expression ref is available
      if (result instanceof FunctionInvocationError) {
        FunctionInvocationError error = (FunctionInvocationError) result;
        throw new InvalidFunctionInvocationError(functionExpression, error.getArgumentIndex(), error.getMessage());
      }

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Invoked function, result: " + result);
      //#endif
      return result;
    }

    if (expression instanceof CallbackExpression) {
      CallbackExpression callbackExpression = (CallbackExpression) expression;

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Setting up the java endpoint for a callback expression");
      //#endif

      // This lambda function will be called by java every time the callback is invoked
      return AExpressionFunction.makeUnchecked((env, args) -> {

        // Copy the static variable table and extend it below
        Map<String, Object> combinedVariables = new HashMap<>(environment.getStaticVariables());

        // Map all identifiers from the callback's signature to a matching java argument in sequence
        // If there are more arguments in the signature than provided by java, they'll just be set to null
        for (int i = 0; i < callbackExpression.getSignature().size(); i++) {
          String variableIdentifier = callbackExpression.getSignature().get(i).getSymbol();
          Object variableValue = i < args.size() ? args.get(i) : null;

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogLevel.INTERPRETER, "Adding " + variableIdentifier + "=" + variableValue + " to a callback's environment");
          //#endif
          combinedVariables.put(variableIdentifier, variableValue);
        }

        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating a callback's body");
        //#endif

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
        });

        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Callback result=" + result);
        //#endif
        return result;
      });
    }

    /////////////////////// Control Flow ////////////////////////

    if (expression instanceof IfThenElseExpression) {
      IfThenElseExpression ifExpression = (IfThenElseExpression) expression;

      // Evaluate the if statement's condition expression
      Object condition = evaluateExpression(ifExpression.getCondition(), environment);

      // Interpret the result as a boolean and evaluate the body accordingly
      if (environment.getValueInterpreter().asBoolean(condition))
        return evaluateExpression(ifExpression.getPositiveBody(), environment);

      return evaluateExpression(ifExpression.getNegativeBody(), environment);
    }

    /////////////////////// Member Access ////////////////////////

    if (expression instanceof MemberAccessExpression) {
      MemberAccessExpression memberExpression = (MemberAccessExpression) expression;

      // Look up the container's value
      Object value = evaluateExpression(memberExpression.getLhs(), environment);
      AExpression access = memberExpression.getRhs();

      String fieldName;

      // Already an identifier, use it's symbol
      if (access instanceof IdentifierExpression)
        fieldName = ((IdentifierExpression) access).getSymbol();

      // Evaluate the name expression as a string
      else
        fieldName = valueInterpreter.asString(evaluateExpression(access, environment));

      // Cannot access any members of null
      if (value == null)
        throw new UnknownMemberError(memberExpression, null, fieldName);

      // Look through all available fields within the container
      for (Field f : value.getClass().getDeclaredFields()) {
        // Not the target field
        if (!f.getName().equalsIgnoreCase(fieldName))
          continue;

        try {
          f.setAccessible(true);
          return f.get(value);
        } catch (Exception e) {
          logger.logError("Could not access an object's member", e);
          return "<error>";
        }
      }

      // Found no field with the required name
      throw new UnknownMemberError(memberExpression, value, fieldName);
    }

    //////////////////// Binary Expressions /////////////////////

    if (expression instanceof ABinaryExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating LHS and RHS of a binary expression");
      //#endif

      Object lhs = evaluateExpression(((ABinaryExpression) expression).getLhs(), environment);
      Object rhs = evaluateExpression(((ABinaryExpression) expression).getRhs(), environment);

      if (expression instanceof MathExpression) {
        MathOperation operation = ((MathExpression) expression).getOperation();
        Object result;

        result = valueInterpreter.performMath(lhs, rhs, operation);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Math Operation operation " + operation + " result: " + result);
        //#endif
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

        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Equality Operation operation " + operation + " result: " + result);
        //#endif
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

        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Comparison Operation operation " + operation + " result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof ConjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) && valueInterpreter.asBoolean(rhs);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Conjunction Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof DisjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) || valueInterpreter.asBoolean(rhs);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Disjunction Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof ConcatenationExpression) {
        String result = valueInterpreter.asString(lhs) + valueInterpreter.asString(rhs);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Concatenation Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof IndexExpression) {
        if (lhs instanceof List) {
          List<?> list = (List<?>) lhs;
          int key = (int) valueInterpreter.asLong(rhs);
          Object result = key < list.size() ? list.get(key) : null;

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogLevel.INTERPRETER, "Indexing a list at " + key + ": " + result);
          //#endif

          return result;
        }

        if (lhs != null && lhs.getClass().isArray()) {
          int key = (int) valueInterpreter.asLong(rhs);
          Object result = key < Array.getLength(lhs) ? Array.get(lhs, key) : null;

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogLevel.INTERPRETER, "Indexing an array at " + key + ": " + result);
          //#endif

          return result;
        }

        if (lhs instanceof Map) {
          Map<?, ?> map = (Map<?, ?>) lhs;
          String key = valueInterpreter.asString(rhs);
          Object result = map.get(key);

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogLevel.INTERPRETER, "Indexing a map at " + key + ": " + result);
          //#endif

          return result;
        }

        // Cannot index this type of value
        throw new NonIndexableValueError((IndexExpression) expression, lhs);
      }
    }

    ///////////////////// Unary Expressions /////////////////////

    if (expression instanceof AUnaryExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Evaluating input of a unary expression");
      //#endif

      Object input = evaluateExpression(((AUnaryExpression) expression).getInput(), environment);

      if (expression instanceof FlipSignExpression) {
        Object result;

        if (valueInterpreter.hasDecimalPoint(input))
          result = -1 * valueInterpreter.asDouble(input);
        else
          result = -1 * valueInterpreter.asLong(input);

        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Flip Sign Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof InvertExpression) {
        boolean result = !valueInterpreter.asBoolean(input);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.INTERPRETER, "Invert Operation result: " + result);
        //#endif
        return result;
      }
    }

    throw new IllegalStateException("Cannot parse unknown expression type " + expression.getClass());
  }

  /**
   * Tries to look up a variable within the provided environment based on an identifier
   * @param environment Environment to look in
   * @param identifier Identifier to look up
   * @return Variable value
   * @throws UndefinedVariableError A variable with that identifier does not exist within the environment
   */
  private Object lookupVariable(IEvaluationEnvironment environment, IdentifierExpression identifier) throws UndefinedVariableError {
    String symbol = identifier.getSymbol().toLowerCase(Locale.ROOT);

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.INTERPRETER, "Looking up variable " + symbol);
    //#endif

    if (environment.getStaticVariables().containsKey(symbol)) {
      Object value = environment.getStaticVariables().get(symbol);

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Resolved static variable value: " + value);
      //#endif

      return value;
    }

    Supplier<Object> valueSupplier = environment.getLiveVariables().get(symbol);
    if (valueSupplier != null) {
      Object value = valueSupplier.get();

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.INTERPRETER, "Resolved dynamic variable value: " + value);
      //#endif

      return value;
    }

    throw new UndefinedVariableError(identifier);
  }
}