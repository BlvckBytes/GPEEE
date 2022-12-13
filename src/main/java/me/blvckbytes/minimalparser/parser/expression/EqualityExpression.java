package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.EqualityOperation;

public class EqualityExpression extends BinaryExpression {

  private final EqualityOperation operation;

  public EqualityExpression(AExpression lhs, AExpression rhs, EqualityOperation operation) {
    super(lhs, rhs);
    this.operation = operation;
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return null;
  }
}
