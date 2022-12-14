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

package me.blvckbytes.gpeee.parser;

import lombok.AllArgsConstructor;
import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.functions.std.AStandardFunction;
import me.blvckbytes.gpeee.interpreter.Interpreter;
import me.blvckbytes.gpeee.logging.DebugLogSource;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.parser.expression.*;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@AllArgsConstructor
public class Optimizer {

  private final ILogger logger;
  private final Interpreter interpreter;
  private final IStandardFunctionRegistry standardFunctionRegistry;

  /**
   * Optimizes an AST by evaluating static expressions ahead of time
   * @param expression Expression (root node of AST) to optimize
   * @return Optimized result
   */
  public AExpression optimizeAST(AExpression expression) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.OPTIMIZER, "Starting to optimize the expression " + expression.expressionify());
    //#endif
    return optimizeASTSub(expression, null);
  }

  private AExpression optimizeASTSub(AExpression expression, @Nullable Consumer<AExpression> substituteParent) throws AEvaluatorError {

    if (expression instanceof ProgramExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Encountered a program expression");
      //#endif

      // Optimize all lines individually
      ProgramExpression program = (ProgramExpression) expression;
      program.getLines().replaceAll(aExpression -> optimizeASTSub(aExpression, null));

      return expression;
    }

    if (expression instanceof ABinaryExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Encountered a binary expression");
      //#endif

      ABinaryExpression binary = (ABinaryExpression) expression;
      boolean lhsIs = isImmediatelyResolvable(binary.getLhs()), rhsIs = isImmediatelyResolvable(binary.getRhs());

      // Both sides of the binary expression can be resolved immediately
      if (lhsIs && rhsIs) {
        AExpression result = wrapValue(binary.getLhs(), interpreter.evaluateExpression(binary, GPEEE.EMPTY_ENVIRONMENT));
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Resolved expression, value: " + result.expressionify());
        //#endif

        // No parent to call - this is already the root node
        if (substituteParent == null) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.OPTIMIZER, "Responding with result as root node");
          //#endif
          return result;
        }

        // Substitute the parent node for the new resulting node
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Substituting result using provided callback");
        //#endif
        substituteParent.accept(result);
        return null;
      }

      // Not yet resolvable, try to optimize both LHS and RHS

      if (!lhsIs) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Trying to optimize it's lhs");
        //#endif
        optimizeASTSub(binary.getLhs(), binary::setLhs);
      }

      if (!rhsIs) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Trying to optimize it's rhs");
        //#endif
        optimizeASTSub(binary.getRhs(), binary::setRhs);
      }

      rhsIs = isImmediatelyResolvable(binary.getRhs());

      // Check if it's now resolvable, if so - run through the optimizer again
      if (isImmediatelyResolvable(binary.getLhs()) && rhsIs) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Is now resolvable, optimizing whole expression");
        //#endif
        return optimizeASTSub(binary, substituteParent);
      }

      // This expression's RHS is resolvable AND the LHS of this expression is also a binary expression
      if (rhsIs && binary.getClass() == binary.getLhs().getClass()) {
        ABinaryExpression lhsBinary = (ABinaryExpression) binary.getLhs();

        if (
          // AND they can be combined (same operators, order of operation doesn't matter)
          binary.canBeCombinedToOptimize(lhsBinary) &&
          // AND the LHS' RHS is also resolvable
          isImmediatelyResolvable(lhsBinary.getRhs())
        ) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.OPTIMIZER, "Going to combine expression's RHS with LHS' RHS");
          //#endif
          // binary.RHS and lhsBinary.RHS are solvable, operators are combinable
          // Example situation: (<lhsBinary>) + binary.RHS

          // Substitute into lhsBinary, evaluate, set as binary.RHS and then substitute lhsBinary for
          // it's unresolvable lhs while both RHS' values have been combined into binary's RHS

          // store lhsBinary.LHS
          AExpression lhsBinaryLhsSave = lhsBinary.getLhs();

          // move lhsBinary.RHS into lhsBinary.LHS (shifting to the left)
          lhsBinary.setLhs(lhsBinary.getRhs());

          // Substitute the "outer" resolvable RHS in
          lhsBinary.setRhs(binary.getRhs());

          // evaluate lhsBinary, now that it's made up of two resolvable values
          AExpression result = wrapValue(binary, interpreter.evaluateExpression(lhsBinary, GPEEE.EMPTY_ENVIRONMENT));

          // Set the new outer RHS
          binary.setRhs(result);

          // Substitute lhsBinary for lhsBinary's LHS
          binary.setLhs(lhsBinaryLhsSave);

          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.OPTIMIZER, "Done, optimized out expression's resolvable RHS");
          //#endif
          return binary;
        }
      }

      // Not resolvable
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Whole expression is not resolvable");
      //#endif
      return binary;
    }

    if (expression instanceof AUnaryExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Encountered a unary expression");
      //#endif

      AUnaryExpression unary = (AUnaryExpression) expression;

      if (isImmediatelyResolvable(unary.getInput())) {
        AExpression result = wrapValue(unary.getInput(), interpreter.evaluateExpression(unary, GPEEE.EMPTY_ENVIRONMENT));
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Resolved expression, value: " + result.expressionify());
        //#endif

        // No parent to call - this is already the root node
        if (substituteParent == null) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.OPTIMIZER, "Responding with result as root node");
          //#endif
          return result;
        }

        // Substitute the parent node for the new resulting node
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Substituting result using provided callback");
        //#endif
        substituteParent.accept(result);
        return null;
      }

      // Try to optimize the unary expression input
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Trying to optimize it's input");
      //#endif
      optimizeASTSub(unary.getInput(), unary::setInput);

      // Check if it's now resolvable, if so - run through the optimizer again
      if (isImmediatelyResolvable(unary.getInput())) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Is now resolvable, optimizing whole expression");
        //#endif
        return optimizeASTSub(unary, substituteParent);
      }

      // Not resolvable
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Whole expression is not resolvable");
      //#endif
      return unary;
    }

    if (expression instanceof CallbackExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Encountered a callback expression");
      //#endif

      CallbackExpression callback = (CallbackExpression) expression;

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Trying to optimize callback body");
      //#endif

      // Try to optimize it's body
      optimizeASTSub(callback.getBody(), callback::setBody);
    }

    if (expression instanceof FunctionInvocationExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Encountered a function invocation expression");
      //#endif

      FunctionInvocationExpression invocation = (FunctionInvocationExpression) expression;
      String name = invocation.getName().getSymbol();

      // Try to optimize each argument one by one
      boolean allArgsResolvable = true;
      for (int i = 0; i < invocation.getArguments().size(); i++) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Trying to optimize function argument " + (i + 1));
        //#endif
        Tuple<AExpression, @Nullable IdentifierExpression> argument = invocation.getArguments().get(i);
        optimizeASTSub(argument.getA(), argument::setA);

        // Argument cannot be resolved, even after optimization
        if (!isImmediatelyResolvable(argument.getA()))
          allArgsResolvable = false;
      }

      // This invocation targets a standard function which is available at the time of optimization
      // And it only returns a primary result in all cases
      // And all arguments are immediately resolvable, so this is in effect another "static value"
      AStandardFunction standardFunction = standardFunctionRegistry.lookup(name);
      if (standardFunction != null && standardFunction.returnsPrimaryResult() && allArgsResolvable) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Evaluating std-function call to " + name + " with all resolvable arguments");
        //#endif
        return wrapValue(invocation, interpreter.evaluateExpression(invocation, GPEEE.EMPTY_ENVIRONMENT));
      }

      // Function invocation cannot be optimized away
      return expression;
    }

    if (expression instanceof IfThenElseExpression) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.OPTIMIZER, "Encountered a if then else expression");
      //#endif

      IfThenElseExpression ifExpression = (IfThenElseExpression) expression;

      // Condition not resolvable, try to optimize
      if (!isImmediatelyResolvable(ifExpression.getCondition())) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Trying to optimize it's condition");
        //#endif
        optimizeASTSub(ifExpression.getCondition(), ifExpression::setCondition);
      }

      // Condition is resolvable now, resolve
      if (isImmediatelyResolvable(ifExpression.getCondition())) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.OPTIMIZER, "Condition has been evaluated, substituting body");
        //#endif

        Object condition = interpreter.evaluateExpression(ifExpression.getCondition(), GPEEE.EMPTY_ENVIRONMENT);
        AExpression result = GPEEE.STD_VALUE_INTERPRETER.asBoolean(condition) ? ifExpression.getPositiveBody() : ifExpression.getNegativeBody();

        // Result is not resolvable, try to optimize
        if (!isImmediatelyResolvable(result)) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.OPTIMIZER, "Trying to optimize it's result");
          //#endif
          result = optimizeASTSub(result, null);
        }

        // Result is resolvable now, resolve before substituting
        if (isImmediatelyResolvable(result))
          return wrapValue(result, interpreter.evaluateExpression(result, GPEEE.EMPTY_ENVIRONMENT));

        // Result cannot be evaluated further
        return result;
      }
    }

    // Expression type cannot be optimized
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.OPTIMIZER, "Cannot optimize node " + expression.getClass().getSimpleName());
    //#endif
    return expression;
  }

  /**
   * Wrap a bare object - as received by an evaluation call - back into an AST node
   * @param previous Expression which has been evaluated, used for debug information
   * @param value Value to wrap
   * @return Wrapped value
   */
  private AExpression wrapValue(AExpression previous, @Nullable Object value) {
    if (value == null)
      return new LiteralExpression(LiteralType.NULL, previous.getHead(), previous.getTail(), previous.getFullContainingExpression());

    if (value instanceof Boolean) {
      return new LiteralExpression(
        ((Boolean) value) ? LiteralType.TRUE : LiteralType.FALSE,
        previous.getHead(), previous.getTail(), previous.getFullContainingExpression()
      );
    }

    if (value instanceof String)
      return new StringExpression(((String) value), previous.getHead(), previous.getTail(), previous.getFullContainingExpression());

    if (value instanceof Long || value instanceof Integer)
      return new LongExpression(((Number) value).longValue(), previous.getHead(), previous.getTail(), previous.getFullContainingExpression());

    if (value instanceof Double || value instanceof Float)
      return new DoubleExpression(((Number) value).doubleValue(), previous.getHead(), previous.getTail(), previous.getFullContainingExpression());

    throw new IllegalStateException("Unimplemented value type encountered");
  }

  /**
   * Checks whether a given expression can be immediately resolved
   * without altering it's runtime behavior
   * @param expression Expression in question
   */
  private boolean isImmediatelyResolvable(AExpression expression) {
    return (
      (expression instanceof DoubleExpression) ||
      (expression instanceof LongExpression) ||
      (expression instanceof StringExpression) ||
      (expression instanceof LiteralExpression)
    );
  }
}
