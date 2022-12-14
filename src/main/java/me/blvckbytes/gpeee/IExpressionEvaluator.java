package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.expression.AExpression;

public interface IExpressionEvaluator {

  /**
   * Parses an input string into an abstract syntax tree (AST) to be evaluated
   * later on and possibly be reused for multiple evaluations with multiple environments.
   * @param input Input to parse
   * @return Root node of the AST
   * @throws AEvaluatorError Error during the parsing process
   */
  AExpression parseString(String input) throws AEvaluatorError;

  /**
   * Evaluates a previously parsed expression within a provided evaluation environment.
   * @param expression Expression to evaluate
   * @return Resulting expression value
   * @throws AEvaluatorError Error during the interpretation process
   */
  ExpressionValue evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError;

}
