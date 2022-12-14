package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.Interpreter;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class GPEEE implements IExpressionEvaluator {

  private final Parser parser;
  private final Interpreter interpreter;
  private final Consumer<String> debugLogger;

  public GPEEE(@Nullable Consumer<String> debugLogger) {
    this.debugLogger = debugLogger == null ? m -> {} : debugLogger;
    this.parser = new Parser(debugLogger);
    this.interpreter = new Interpreter();
  }

  @Override
  public AExpression parseString(String input) throws AEvaluatorError {
    return parser.parse(new Tokenizer(debugLogger, input));
  }

  @Override
  public ExpressionValue evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    return interpreter.evaluateExpression(expression, environment);
  }
}
