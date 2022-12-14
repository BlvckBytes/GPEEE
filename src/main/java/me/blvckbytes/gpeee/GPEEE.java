package me.blvckbytes.gpeee;

import lombok.Setter;
import me.blvckbytes.gpeee.error.AParserError;
import me.blvckbytes.gpeee.interpreter.AInterpreterError;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;

public class GPEEE implements IExpressionEvaluator {

  private final Parser parser;
  private final ILogger logger;

  public GPEEE(ILogger logger) {
    this.logger = logger;
    this.parser = new Parser(this.logger);
  }

  @Override
  public AExpression parseString(String input) throws AParserError {
    return parser.parse(new Tokenizer(logger, input));
  }

  @Override
  public ExpressionValue evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AInterpreterError {
    // TODO: Implement
    return null;
  }
}
