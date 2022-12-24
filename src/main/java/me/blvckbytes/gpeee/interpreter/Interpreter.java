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
import me.blvckbytes.gpeee.logging.DebugLogSource;
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

    // Every expression evaluation starts out with a fresh interpretation environment
    // State is NOT kept between evaluation sessions
    return evaluateExpressionSub(expression, environment, new InterpretationEnvironment());
  }

  public Object evaluateExpressionSub(
    AExpression expression,
    IEvaluationEnvironment evaluationEnvironment,
    InterpretationEnvironment interpretationEnvironment
  ) throws AEvaluatorError {

    if (expression == null)
      return null;

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.INTERPRETER, "Evaluating " + expression.getClass().getSimpleName() + ": " + expression.expressionify());
    //#endif

    IValueInterpreter valueInterpreter = evaluationEnvironment.getValueInterpreter();

    //////////////////////// Entry Point ////////////////////////

    if (expression instanceof ProgramExpression) {
      ProgramExpression program = (ProgramExpression) expression;

      Object lastValue = null;
      for (int i = 0; i < program.getLines().size(); i++) {
        AExpression line = program.getLines().get(i);

        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.INTERPRETER, "Processing program line " + (i + 1));
        //#endif

        lastValue = evaluateExpressionSub(line, evaluationEnvironment, interpretationEnvironment);
      }

      // The return value of a program is the return value of it's last line
      return lastValue;
    }

    /////////////////////// Static Values ///////////////////////

    if (expression instanceof LongExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Taking the immediate long value");
      //#endif
      return ((LongExpression) expression).getNumber();
    }

    if (expression instanceof DoubleExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Taking the immediate double value");
      //#endif
      return ((DoubleExpression) expression).getValue();
    }

    if (expression instanceof LiteralExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Taking the immediate literal value");
      //#endif
      return ((LiteralExpression) expression).getValue();
    }

    if (expression instanceof StringExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Taking the immediate string value");
      //#endif
      return ((StringExpression) expression).getValue();
    }

    ////////////////////// Variable Values //////////////////////

    if (expression instanceof IdentifierExpression)
      return lookupVariable(evaluationEnvironment, interpretationEnvironment, (IdentifierExpression) expression);

    ///////////////////////// Functions /////////////////////////

    if (expression instanceof FunctionInvocationExpression) {
      FunctionInvocationExpression functionExpression = (FunctionInvocationExpression) expression;
      AExpressionFunction function = lookupFunction(evaluationEnvironment, interpretationEnvironment, functionExpression.getName());

      // Function does not exist within the current environment
      if (function == null) {

        // Was an optional call, respond with null
        if (functionExpression.isOptional()) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Function " + functionExpression.getName().getSymbol() + " not found, returning null (optional call)");
          //#endif
          return null;
        }

        throw new UndefinedFunctionError(functionExpression.getName());
      }

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Evaluating arguments of function invocation " + functionExpression.getName().getSymbol());
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
        logger.logDebug(DebugLogSource.INTERPRETER, "Evaluating argument " + (++debugArgCounter));
        //#endif

        Object argumentValue = evaluateExpressionSub(argument.getA(), evaluationEnvironment, interpretationEnvironment);

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
            logger.logDebug(DebugLogSource.INTERPRETER, "Matched named argument " + argName + " to index " + i);
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
      function.validateArguments(functionExpression, evaluationEnvironment.getValueInterpreter(), arguments);

      // Invoke and return that function's result
      Object result = function.apply(evaluationEnvironment, arguments);

      // Throw an exception based on the error description object, now that the expression ref is available
      if (result instanceof FunctionInvocationError) {
        FunctionInvocationError error = (FunctionInvocationError) result;
        throw new InvalidFunctionInvocationError(functionExpression, error.getArgumentIndex(), error.getMessage());
      }

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Invoked function, result: " + result);
      //#endif
      return result;
    }

    if (expression instanceof CallbackExpression) {
      CallbackExpression callbackExpression = (CallbackExpression) expression;

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Setting up the java endpoint for a callback expression");
      //#endif

      // This lambda function will be called by java every time the callback is invoked
      return new AExpressionFunction() {
        @Override
        public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
          // Copy the static variable table and extend it below
          Map<String, Object> combinedVariables = new HashMap<>(environment.getStaticVariables());

          // Map all identifiers from the callback's signature to a matching java argument in sequence
          // If there are more arguments in the signature than provided by java, they'll just be set to null
          for (int i = 0; i < callbackExpression.getSignature().size(); i++) {
            String variableIdentifier = callbackExpression.getSignature().get(i).getSymbol();
            Object variableValue = i < args.size() ? args.get(i) : null;

            //#if mvn.project.property.production != "true"
            logger.logDebug(DebugLogSource.INTERPRETER, "Adding " + variableIdentifier + "=" + variableValue + " to a callback's environment");
            //#endif
            combinedVariables.put(variableIdentifier, variableValue);
          }

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Evaluating a callback's body");
          //#endif

          // Callback expressions are evaluated within their own environment, which extends the current environment
          // by the additional variables coming from the arguments passed by the callback caller
          Object result = evaluateExpressionSub(callbackExpression.getBody(), new IEvaluationEnvironment() {

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
          }, interpretationEnvironment);

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Callback result=" + result);
          //#endif
          return result;
        }

        @Override
        public @Nullable List<ExpressionFunctionArgument> getArguments() {
          return null;
        }
      };
    }

    /////////////////////// Control Flow ////////////////////////

    if (expression instanceof IfThenElseExpression) {
      IfThenElseExpression ifExpression = (IfThenElseExpression) expression;

      // Evaluate the if statement's condition expression
      Object condition = evaluateExpressionSub(ifExpression.getCondition(), evaluationEnvironment, interpretationEnvironment);

      // Interpret the result as a boolean and evaluate the body accordingly
      if (evaluationEnvironment.getValueInterpreter().asBoolean(condition))
        return evaluateExpressionSub(ifExpression.getPositiveBody(), evaluationEnvironment, interpretationEnvironment);

      return evaluateExpressionSub(ifExpression.getNegativeBody(), evaluationEnvironment, interpretationEnvironment);
    }

    /////////////////////// Member Access ////////////////////////

    if (expression instanceof MemberAccessExpression) {
      MemberAccessExpression memberExpression = (MemberAccessExpression) expression;

      // Look up the container's value
      Object value = evaluateExpressionSub(memberExpression.getLhs(), evaluationEnvironment, interpretationEnvironment);
      AExpression access = memberExpression.getRhs();

      String fieldName;

      // Already an identifier, use it's symbol
      if (access instanceof IdentifierExpression)
        fieldName = ((IdentifierExpression) access).getSymbol();

      // Evaluate the name expression as a string
      else
        fieldName = valueInterpreter.asString(evaluateExpressionSub(access, evaluationEnvironment, interpretationEnvironment));

      // Cannot access any members of null
      if (value == null) {

        // Optional access, respond with null
        if (memberExpression.isOptional())
          return null;

        throw new UnknownMemberError(memberExpression, null, fieldName);
      }

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

      // Optional access, respond with null
      if (memberExpression.isOptional())
        return null;

      // Found no field with the required name
      throw new UnknownMemberError(memberExpression, value, fieldName);
    }

    //////////////////// Binary Expressions /////////////////////

    if (expression instanceof ABinaryExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Evaluating LHS and RHS of a binary expression");
      //#endif

      Object rhs = evaluateExpressionSub(((ABinaryExpression) expression).getRhs(), evaluationEnvironment, interpretationEnvironment);

      // Try to execute an assignment expression before evaluating the LHS value
      // of the binary expression, which would end up in a variable lookup
      if (expression instanceof AssignmentExpression) {
        String identifier = ((IdentifierExpression) ((ABinaryExpression) expression).getLhs()).getSymbol();
        boolean isFunction = rhs instanceof AExpressionFunction;

        // Is not a function, check for existing variable names before adding
        if (!isFunction) {
          if (
            evaluationEnvironment.getLiveVariables().containsKey(identifier) ||
            evaluationEnvironment.getStaticVariables().containsKey(identifier) ||
            interpretationEnvironment.getVariables().containsKey(identifier)
          ) {
            throw new IdentifierInUseError((IdentifierExpression) ((AssignmentExpression) expression).getLhs());
          }

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Storing variable " + identifier + " within the interpretation environment");
          //#endif

          interpretationEnvironment.getVariables().put(identifier, rhs);
        }

        // Is a function, check for existing function names before adding
        else {
          if (
            standardFunctionRegistry.lookup(identifier) != null ||
            evaluationEnvironment.getFunctions().containsKey(identifier) ||
            interpretationEnvironment.getFunctions().containsKey(identifier)
          ) {
            throw new IdentifierInUseError((IdentifierExpression) ((AssignmentExpression) expression).getLhs());
          }

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Storing function " + identifier + " within the interpretation environment");
          //#endif

          interpretationEnvironment.getFunctions().put(identifier, (AExpressionFunction) rhs);
        }

        // Assignments always return their assigned type
        return rhs;
      }

      Object lhs = evaluateExpressionSub(((ABinaryExpression) expression).getLhs(), evaluationEnvironment, interpretationEnvironment);

      if (expression instanceof MathExpression) {
        MathOperation operation = ((MathExpression) expression).getOperation();
        Object result;

        result = valueInterpreter.performMath(lhs, rhs, operation);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.INTERPRETER, "Math Operation operation " + operation + " result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof NullCoalesceExpression) {
        NullCoalesceExpression nullCoalesce = (NullCoalesceExpression) expression;
        Object inputValue = evaluateExpressionSub(nullCoalesce.getLhs(), evaluationEnvironment, interpretationEnvironment);

        // Input value is non-null, return that
        if (inputValue != null)
          return inputValue;

        // Return the provided fallback value
        return evaluateExpressionSub(nullCoalesce.getRhs(), evaluationEnvironment, interpretationEnvironment);
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
        logger.logDebug(DebugLogSource.INTERPRETER, "Equality Operation operation " + operation + " result: " + result);
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
        logger.logDebug(DebugLogSource.INTERPRETER, "Comparison Operation operation " + operation + " result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof ConjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) && valueInterpreter.asBoolean(rhs);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.INTERPRETER, "Conjunction Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof DisjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) || valueInterpreter.asBoolean(rhs);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.INTERPRETER, "Disjunction Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof ConcatenationExpression) {
        String result = valueInterpreter.asString(lhs) + valueInterpreter.asString(rhs);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.INTERPRETER, "Concatenation Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof IndexExpression) {
        IndexExpression indexExpression = (IndexExpression) expression;

        if (lhs instanceof List) {
          List<?> list = (List<?>) lhs;
          int key = (int) valueInterpreter.asLong(rhs);
          int listLength = list.size();

          // Not a valid list index
          if (key >= listLength) {

            // Index is optional, respond with null
            if (indexExpression.isOptional())
              return null;

            throw new InvalidIndexError(indexExpression, key, listLength);
          }

          Object result = list.get(key);

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Indexing a list at " + key + ": " + result);
          //#endif

          return result;
        }

        if (lhs != null && lhs.getClass().isArray()) {
          int key = (int) valueInterpreter.asLong(rhs);
          int arrayLength = Array.getLength(lhs);

          // Not a valid array index
          if (key >= arrayLength) {

            // Index is optional, respond with null
            if (indexExpression.isOptional())
              return null;

            throw new InvalidIndexError(indexExpression, key, arrayLength);
          }

          Object result = Array.get(lhs, key);

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Indexing an array at " + key + ": " + result);
          //#endif

          return result;
        }

        if (lhs instanceof Map) {
          Map<?, ?> map = (Map<?, ?>) lhs;
          String key = valueInterpreter.asString(rhs);

          // Not a valid map member
          if (!map.containsKey(key)) {

            // Index is optional, respond with null
            if (indexExpression.isOptional())
              return null;

            throw new InvalidMapKeyError(indexExpression, key);
          }

          Object result = map.get(key);

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.INTERPRETER, "Indexing a map at " + key + ": " + result);
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
      logger.logDebug(DebugLogSource.INTERPRETER, "Evaluating input of a unary expression");
      //#endif

      Object input = evaluateExpressionSub(((AUnaryExpression) expression).getInput(), evaluationEnvironment, interpretationEnvironment);

      if (expression instanceof FlipSignExpression) {
        Object result;

        if (valueInterpreter.hasDecimalPoint(input))
          result = -1 * valueInterpreter.asDouble(input);
        else
          result = -1 * valueInterpreter.asLong(input);

        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.INTERPRETER, "Flip Sign Operation result: " + result);
        //#endif
        return result;
      }

      if (expression instanceof InvertExpression) {
        boolean result = !valueInterpreter.asBoolean(input);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.INTERPRETER, "Invert Operation result: " + result);
        //#endif
        return result;
      }
    }

    throw new IllegalStateException("Cannot parse unknown expression type " + expression.getClass());
  }

  /**
   * Tries to look up a function within the provided environments based on an identifier
   * @param evaluationEnvironment Evaluation environment to look in
   * @param interpretationEnvironment Interpretation environment to look in
   * @param identifier Identifier to look up
   * @return Function value
   */
  private @Nullable AExpressionFunction lookupFunction(
    IEvaluationEnvironment evaluationEnvironment,
    InterpretationEnvironment interpretationEnvironment,
    IdentifierExpression identifier
  ) {
    String symbol = identifier.getSymbol().toLowerCase(Locale.ROOT);

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.INTERPRETER, "Looking up function " + symbol);
    //#endif

    AExpressionFunction stdFunction = standardFunctionRegistry.lookup(symbol);
    if (stdFunction != null) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Resolved standard function");
      //#endif
      return stdFunction;
    }

    if (evaluationEnvironment.getFunctions().containsKey(symbol)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Resolved environment function");
      //#endif
      return evaluationEnvironment.getFunctions().get(symbol);
    }

    if (interpretationEnvironment.getFunctions().containsKey(symbol)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Resolved interpretation function");
      //#endif
      return interpretationEnvironment.getFunctions().get(symbol);
    }

    return null;
  }

  /**
   * Tries to look up a variable within the provided environments based on an identifier
   * @param evaluationEnvironment Evaluation environment to look in
   * @param interpretationEnvironment Interpretation environment to look in
   * @param identifier Identifier to look up
   * @return Variable value
   * @throws UndefinedVariableError A variable with that identifier does not exist within the environments
   */
  private Object lookupVariable(
    IEvaluationEnvironment evaluationEnvironment,
    InterpretationEnvironment interpretationEnvironment,
    IdentifierExpression identifier
  ) throws UndefinedVariableError {
    String symbol = identifier.getSymbol().toLowerCase(Locale.ROOT);

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.INTERPRETER, "Looking up variable " + symbol);
    //#endif

    if (evaluationEnvironment.getStaticVariables().containsKey(symbol)) {
      Object value = evaluationEnvironment.getStaticVariables().get(symbol);

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Resolved static variable value: " + value);
      //#endif

      return value;
    }

    Supplier<Object> valueSupplier = evaluationEnvironment.getLiveVariables().get(symbol);
    if (valueSupplier != null) {
      Object value = valueSupplier.get();

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Resolved dynamic variable value: " + value);
      //#endif

      return value;
    }

    if (interpretationEnvironment.getVariables().containsKey(symbol)) {
      Object value = interpretationEnvironment.getVariables().get(symbol);

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.INTERPRETER, "Resolved interpretation environment variable value: " + value);
      //#endif

      return value;
    }

    throw new UndefinedVariableError(identifier);
  }
}