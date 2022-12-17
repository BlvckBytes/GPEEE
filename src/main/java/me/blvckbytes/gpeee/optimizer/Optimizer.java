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

package me.blvckbytes.gpeee.optimizer;

import lombok.AllArgsConstructor;
import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.interpreter.Interpreter;
import me.blvckbytes.gpeee.logging.DebugLogLevel;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.parser.LiteralType;
import me.blvckbytes.gpeee.parser.expression.*;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@AllArgsConstructor
public class Optimizer {

  private final ILogger logger;
  private final Interpreter interpreter;

  /**
   * Optimizes an AST by evaluating static expressions ahead of time
   * @param expression Expression (root node of AST) to optimize
   * @return Optimized result
   */
  public AExpression optimizeAST(AExpression expression) {
    logger.logDebug(DebugLogLevel.OPTIMIZER, "Starting to optimize the expression " + expression.expressionify());
    return optimizeASTSub(expression, null);
  }

  private AExpression optimizeASTSub(AExpression expression, @Nullable Consumer<AExpression> substituteParent) throws AEvaluatorError {

    if (expression instanceof ABinaryExpression) {
      logger.logDebug(DebugLogLevel.OPTIMIZER, "Encountered a binary expression");

      ABinaryExpression binary = (ABinaryExpression) expression;
      boolean lhsIs = isImmediatelyResolvable(binary.getLhs()), rhsIs = isImmediatelyResolvable(binary.getRhs());

      // Both sides of the binary expression can be resolved immediately
      if (lhsIs && rhsIs) {
        AExpression result = wrapValue(binary.getLhs(), interpreter.evaluateExpression(binary, GPEEE.EMPTY_ENVIRONMENT));
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Resolved expression, value: " + result.expressionify());

        // No parent to call - this is already the root node
        if (substituteParent == null) {
          logger.logDebug(DebugLogLevel.OPTIMIZER, "Responding with result as root node");
          return result;
        }

        // Substitute the parent node for the new resulting node
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Substituting result using provided callback");
        substituteParent.accept(result);
        return null;
      }

      // Not yet resolvable, try to optimize both LHS and RHS

      if (!lhsIs) {
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Trying to optimize it's lhs");
        optimizeASTSub(binary.getLhs(), binary::setLhs);
      }

      if (!rhsIs) {
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Trying to optimize it's rhs");
        optimizeASTSub(binary.getRhs(), binary::setRhs);
      }

      rhsIs = isImmediatelyResolvable(binary.getRhs());

      // Check if it's now resolvable, if so - run through the optimizer again
      if (isImmediatelyResolvable(binary.getLhs()) && rhsIs) {
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Is now resolvable, optimizing whole expression");
        return optimizeASTSub(binary, substituteParent);
      }

      // This expression's RHS is resolvable AND the LHS of this expression is also a binary expression
      if (rhsIs && binary.getClass() == binary.getLhs().getClass()) {
        ABinaryExpression lhsBinary = (ABinaryExpression) binary.getLhs();

        if (
          // AND their operators equal
          binary.operatorEquals(lhsBinary) &&
          // AND the LHS' RHS is also resolvable
          isImmediatelyResolvable(lhsBinary.getRhs())
        ) {
          logger.logDebug(DebugLogLevel.OPTIMIZER, "Going to combine expression's RHS with LHS' RHS");
          // binary.RHS and lhsBinary.RHS are solvable, operations are equal
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

          logger.logDebug(DebugLogLevel.OPTIMIZER, "Done, optimized out expression's resolvable RHS");
          return binary;
        }
      }

      // Not resolvable
      logger.logDebug(DebugLogLevel.OPTIMIZER, "Whole expression is not resolvable");
      return binary;
    }

    if (expression instanceof AUnaryExpression) {
      logger.logDebug(DebugLogLevel.OPTIMIZER, "Encountered a unary expression");

      AUnaryExpression unary = (AUnaryExpression) expression;

      if (isImmediatelyResolvable(unary.getInput())) {
        AExpression result = wrapValue(unary.getInput(), interpreter.evaluateExpression(unary, GPEEE.EMPTY_ENVIRONMENT));
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Resolved expression, value: " + result.expressionify());

        // No parent to call - this is already the root node
        if (substituteParent == null) {
          logger.logDebug(DebugLogLevel.OPTIMIZER, "Responding with result as root node");
          return result;
        }

        // Substitute the parent node for the new resulting node
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Substituting result using provided callback");
        substituteParent.accept(result);
        return null;
      }

      // Try to optimize the unary expression input
      logger.logDebug(DebugLogLevel.OPTIMIZER, "Trying to optimize it's input");
      optimizeASTSub(unary.getInput(), unary::setInput);

      // Check if it's now resolvable, if so - run through the optimizer again
      if (isImmediatelyResolvable(unary.getInput())) {
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Is now resolvable, optimizing whole expression");
        return optimizeASTSub(unary, substituteParent);
      }

      // Not resolvable
      logger.logDebug(DebugLogLevel.OPTIMIZER, "Whole expression is not resolvable");
      return unary;
    }

    if (expression instanceof FunctionInvocationExpression) {
      logger.logDebug(DebugLogLevel.OPTIMIZER, "Encountered a function invocation expression");

      FunctionInvocationExpression invocation = (FunctionInvocationExpression) expression;

      // Try to optimize each argument one by one
      for (int i = 0; i < invocation.getArguments().size(); i++) {
        logger.logDebug(DebugLogLevel.OPTIMIZER, "Trying to optimize function argument " + (i + 1));
        Tuple<AExpression, @Nullable IdentifierExpression> argument = invocation.getArguments().get(i);
        optimizeASTSub(argument.getA(), argument::setA);
      }

      // Function invocations will never be optimized away
      return expression;
    }

    // Expression type cannot be optimized
    logger.logDebug(DebugLogLevel.OPTIMIZER, "Cannot optimize node " + expression.getClass().getSimpleName());
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

    if (value instanceof Long)
      return new LongExpression(((Long) value), previous.getHead(), previous.getTail(), previous.getFullContainingExpression());

    if (value instanceof Double)
      return new DoubleExpression(((Double) value), previous.getHead(), previous.getTail(), previous.getFullContainingExpression());

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
