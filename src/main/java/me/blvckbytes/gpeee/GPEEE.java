package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import me.blvckbytes.gpeee.interpreter.Interpreter;
import me.blvckbytes.gpeee.interpreter.StandardValueInterpreter;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

public class GPEEE implements IExpressionEvaluator {

  public static final IValueInterpreter STD_VALUE_INTERPRETER = new StandardValueInterpreter();

  private final Parser parser;
  private final Interpreter interpreter;
  private final IDebugLogger debugLogger;

  public GPEEE(@Nullable IDebugLogger debugLogger) {
    this.debugLogger = debugLogger == null ? (l, m) -> {} : debugLogger;
    this.parser = new Parser(this.debugLogger);
    this.interpreter = new Interpreter(this.debugLogger);
  }

  @Override
  public AExpression parseString(String input) throws AEvaluatorError {
    return parser.parse(new Tokenizer(this.debugLogger, input));
  }

  @Override
  public Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    return interpreter.evaluateExpression(expression, environment);
  }
}
