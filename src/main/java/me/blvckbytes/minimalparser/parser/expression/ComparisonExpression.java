package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.ComparisonOperation;

public class ComparisonExpression extends BinaryExpression {

  private final ComparisonOperation operation;

  public ComparisonExpression(AExpression lhs, AExpression rhs, ComparisonOperation operation) {
    super(lhs, rhs);
    this.operation = operation;
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return null;
  }
}
