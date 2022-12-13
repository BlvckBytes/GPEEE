package me.blvckbytes.minimalparser.parser.expression;

import ch.obermuhlner.math.big.BigDecimalMath;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.MathOperation;

import java.math.BigDecimal;
import java.math.MathContext;

public class MathExpression extends BinaryExpression {

  private final MathOperation operation;

  public MathExpression(AExpression lhs, AExpression rhs, MathOperation operation) {
    super(lhs, rhs);
    this.operation = operation;
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {

    // TODO: Add proper support for float values
    // TODO: Think ahead about what types external numbers might have...
    BigDecimal lhsV = new BigDecimal(evaluateExpression(lhs, context, valueInterpreter).toString());
    BigDecimal rhsV = new BigDecimal(evaluateExpression(rhs, context, valueInterpreter).toString());

    switch (operation) {
      case ADDITION:
        return lhsV.add(rhsV);

      case SUBTRACTION:
        return lhsV.subtract(rhsV);

      case MULTIPLICATION:
        return lhsV.multiply(rhsV);

      case DIVISION:
        return lhsV.divide(rhsV);

      case MODULO:
        return lhsV.remainder(rhsV);

      case POWER:
        return BigDecimalMath.pow(lhsV, rhsV, MathContext.DECIMAL64);

      default:
        return 0;
    }
  }
}
