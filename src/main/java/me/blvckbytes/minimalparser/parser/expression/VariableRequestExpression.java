package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.error.UnknownVariableError;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public class VariableRequestExpression extends AExpression {

  private final int row, col;
  private final String name;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    Supplier<Object> variable = context.getVariables().get(name);

    // Variable not available in this context
    if (variable == null)
      throw new UnknownVariableError(row, col);

    return variable.get();
  }

  @Override
  public String toString() {
    return "VariableRequestExpression (\n" +
      "row=" + row + ",\n" +
      "col=" + col + ",\n" +
      "name='" + name + '\'' + "\n" +
    ')';
  }
}
