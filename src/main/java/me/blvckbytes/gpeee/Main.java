package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AParserError;
import me.blvckbytes.gpeee.parser.Parser;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.Tokenizer;

public class Main {

  public static void main(String[] args) {
    try {
      ILogger logger = message -> System.out.println("[DEBUG]: " + message);

      Tokenizer tk = new Tokenizer(logger, "22 < myFunc(1, 2, 3) and func2(\"hi\", .4, true) or a + -sin(33) - my_var");
      Parser parser = new Parser(logger);

      AExpression expression;
      try {
        expression = parser.parse(tk);
      } catch (AParserError err) {
        System.err.println(err.generateWarning(tk.getRawText()));
        throw new IllegalStateException();
      }

      if (expression != null) {
        System.out.println(expression.stringify("  ", 0));
        System.out.println("expression=" + expression.expressionify());
      }

      System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
