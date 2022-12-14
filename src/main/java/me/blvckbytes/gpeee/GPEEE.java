package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AParserError;
import me.blvckbytes.gpeee.interpreter.AInterpreterError;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.Interpreter;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

public class GPEEE implements IExpressionEvaluator {

  private final Parser parser;
  private final Interpreter interpreter;
  private final IDebugLogger logger;

  public GPEEE(@Nullable IDebugLogger logger) {
    this.logger = logger == null ? m -> {} : logger;
    this.parser = new Parser(logger);
    this.interpreter = new Interpreter(logger);
  }

  @Override
  public AExpression parseString(String input) throws AParserError {
    return parser.parse(new Tokenizer(logger, input));
  }

  @Override
  public ExpressionValue evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AInterpreterError {
    return interpreter.evaluateExpression(expression, environment);
  }
}
