package me.blvckbytes.minimalparser;

import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.functions.IExpressionFunction;
import me.blvckbytes.minimalparser.parser.Parser;
import me.blvckbytes.minimalparser.parser.expression.AExpression;
import me.blvckbytes.minimalparser.tokenizer.Tokenizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public class Main {

  public static void main(String[] args) {
    try {
      IEvaluationContext dummyContext = new IEvaluationContext() {

        @Override
        public Map<String, IExpressionFunction> getFunctions() {
          Map<String, IExpressionFunction> map = new HashMap<>();

          map.put("if", args -> null);

          return map;
        }

        @Override
        public Map<String, Supplier<Object>> getVariables() {
          Map<String, Supplier<Object>> map = new HashMap<>();

          map.put("number_five", () -> 5);
          map.put("hello", () -> "hello world");
          map.put("random", () -> new Random().nextInt(100));

          return map;
        }

        @Override
        public Optional<String> performLookup(String table, String key) {
          Map<String, String> colors = new HashMap<>();
          Map<String, String> symbols = new HashMap<>();

          colors.put("red", "#FF0000");
          colors.put("green", "#00FF00");
          colors.put("blue", "#0000FF");

          symbols.put("dollar", "$");
          symbols.put("at", "@");
          symbols.put("question", "?");

          switch (table.toLowerCase()) {
            case "colors":
              return Optional.ofNullable(colors.get(key));
            case "symbols":
              return Optional.ofNullable(symbols.get(key));
          }

          return Optional.empty();
        }
      };

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
