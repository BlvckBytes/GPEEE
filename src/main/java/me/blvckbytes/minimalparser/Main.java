package me.blvckbytes.minimalparser;

import me.blvckbytes.minimalparser.functions.AExpressionFunction;
import me.blvckbytes.minimalparser.functions.AddExpressionFunction;
import me.blvckbytes.minimalparser.functions.IfExpressionFunction;
import me.blvckbytes.minimalparser.parser.ExpressionParser;
import me.blvckbytes.minimalparser.parser.NumberCompare;
import me.blvckbytes.minimalparser.parser.expression.AExpression;
import me.blvckbytes.minimalparser.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) {
    try {
      String input = getResourceFileAsString("input.txt");

      if (input == null) {
        System.err.println("Could not read input file!");
        return;
      }

      IEvaluationContext dummyContext = new IEvaluationContext() {

        @Override
        public Map<String, AExpressionFunction> getFunctions() {
          Map<String, AExpressionFunction> map = new HashMap<>();

          map.put("if", new IfExpressionFunction());
          map.put("add", new AddExpressionFunction());

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

      IValueInterpreter valueInterpreter = new IValueInterpreter() {

        // 0, -0
        private final Pattern LONG_PATTERN = Pattern.compile("^-?\\d+$");

        // 0.0, .0, 0, -0.0, -.0, -0
        private final Pattern FLOAT_PATTERN = Pattern.compile("^-?\\d*\\.?\\d+$");

        /**
         * Decides whether a value is truthy ("true", "yes", "1", > 0)
         * @param input Value in question
         * @return True if truthy, false in all other cases
         */
        @Override
        public boolean isTruthy(@Nullable Object input) {
          if (input == null)
            return false;

          if (input instanceof String) {
            return (
              ((String) input).equalsIgnoreCase("true") ||
                ((String) input).equalsIgnoreCase("yes") ||
                ((String) input).equalsIgnoreCase("1")
            );
          }

          return tryParseNumber(input)
            .filter(n -> NumberCompare.GREATER_THAN.apply(n, 0))
            .isPresent();
        }

        /**
         * Tries to parse a number (internally always a double) from an object by
         * either converting it or parsing the number from a string representation
         * @param input Input value
         * @return Parsed double, available if possible
         */
        @Override
        public Optional<Object> tryParseNumber(@Nullable Object input) {
          if (input == null)
            return Optional.empty();

          if (input instanceof Double || input instanceof Float || input instanceof Long || input instanceof Integer)
            return Optional.of(input);

          if (input instanceof String) {
            if (LONG_PATTERN.matcher((String) input).matches())
              return Optional.of(Long.parseLong((String) input));

            if (FLOAT_PATTERN.matcher((String) input).matches())
              return Optional.of(Double.parseDouble((String) input));
          }

          return Optional.empty();
        }
      };

      Tokenizer tk = new Tokenizer(input);
      ExpressionParser parser = new ExpressionParser(tk);
      AExpression expression = parser.parse();

      if (expression != null) {
        System.out.println(expression);
        System.out.println("RESULT: " + expression.evaluate(dummyContext, valueInterpreter));
      }

      System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Reads given resource file as a string.
   *
   * @param fileName path to the resource file
   * @return the file's contents
   * @throws IOException if read fails for any reason
   */
  static String getResourceFileAsString(String fileName) throws IOException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(fileName)) {
      if (is == null) return null;
      try (InputStreamReader isr = new InputStreamReader(is);
           BufferedReader reader = new BufferedReader(isr)) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
  }
}
