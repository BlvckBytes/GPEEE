package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.AParserError;
import me.blvckbytes.gpeee.parser.expression.AExpression;

public class Main {

  public static void main(String[] args) {
    try {
      ILogger logger = message -> System.out.println("[DEBUG]: " + message);
      GPEEE evaluator = new GPEEE(logger);

      String input = "22 < myFunc(1, 2, 3) and func2(\"hi\", .4, true) or a + -sin(33) - my_var";

      AExpression expression = evaluator.parseString(input);

      if (expression != null) {
        System.out.println(expression.stringify("  ", 0));
        System.out.println("expression=" + expression.expressionify());
      }

      System.out.println("Done!");
    } catch (AParserError err) {
      System.err.println(err.generateWarning());
      throw new IllegalStateException();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
