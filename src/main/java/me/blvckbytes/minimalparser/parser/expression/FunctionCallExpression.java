package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.error.UnknownFunctionError;
import me.blvckbytes.minimalparser.functions.AExpressionFunction;

@Getter
@AllArgsConstructor
public class FunctionCallExpression extends AExpression {

  private final int row, col;
  private final String functionName;
  private final Object[] args;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    AExpressionFunction function = context.getFunctions().get(functionName);

    // Function not available in this context
    if (function == null)
      throw new UnknownFunctionError(row, col);

    // Evaluate all arguments
    Object[] evaluatedArgs = new Object[args.length];
    for (int i = 0; i < evaluatedArgs.length; i++)
      evaluatedArgs[i] = evaluateExpression(args[i], context, valueInterpreter);

    return function.apply(evaluatedArgs, valueInterpreter);
  }

  @Override
  public String toString() {
    StringBuilder argsString = new StringBuilder("[\n");

    for (Object arg : args)
      argsString.append(arg.toString()).append("\n");

    argsString.append("]");

    return "FunctionCallExpression (\n" +
      "row=" + row + ",\n" +
      "col=" + col + ",\n" +
      "functionName='" + functionName + '\'' + ",\n" +
      "args=" + argsString + "\n" +
    ')';
  }
}
