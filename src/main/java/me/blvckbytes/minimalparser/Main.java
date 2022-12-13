package me.blvckbytes.minimalparser;

import com.google.common.primitives.Primitives;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.functions.AExpressionFunction;
import me.blvckbytes.minimalparser.functions.IfExpressionFunction;
import me.blvckbytes.minimalparser.parser.ExpressionParser;
import me.blvckbytes.minimalparser.parser.expression.AExpression;
import me.blvckbytes.minimalparser.tokenizer.Tokenizer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Main {

  public static void main(String[] args) {
    try {
      IEvaluationContext dummyContext = new IEvaluationContext() {

        @Override
        public Map<String, AExpressionFunction> getFunctions() {
          Map<String, AExpressionFunction> map = new HashMap<>();

          map.put("if", new IfExpressionFunction());

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

//          return tryParseNumber(input)
//            .filter(n -> NumberCompare.GREATER_THAN.apply(n, 0, this))
//            .isPresent();
          // FIXME
          return false;
        }

        /**
         * Tries to parse a number (internally always a BigDecimal) from an object by
         * either converting it or parsing the number from a string representation
         * @param input Input value
         * @return Parsed BigDecimal, available if possible
         */
        @Override
        public Optional<BigDecimal> tryParseNumber(@Nullable Object input) {
          // Null should fall back to zero
          if (input == null)
            input = 0;

          BigDecimal number = null;

          // Try to find a matching constructor
          try {
            Constructor<?>[] constructors = BigDecimal.class.getConstructors();

            for (Constructor<?> constructor : constructors) {
              // Constructor can take it's type, instantiate
              if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0] == Primitives.unwrap(input.getClass()))
                number = (BigDecimal) constructor.newInstance(input);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }

          // Try to parse from it's string representation if the constructor selection failed
          try {
            if (number == null)
              number = new BigDecimal(String.valueOf(input));
          }

          // Cannot parse
          catch (NumberFormatException ignored) {}

          return Optional.empty();
        }
      };

      ILogger logger = new ILogger() {

        @Override
        public void logDebug(String message) {
          System.out.println("[DEBUG]: " + message);
        }

        @Override
        public void logError(String message) {
          System.err.println(message);
        }
      };

      Tokenizer tk = new Tokenizer(logger, "2^(-1/2)");
      ExpressionParser parser = new ExpressionParser(logger, tk);

      AExpression expression;
      try {
        expression = parser.parse();
      } catch (AParserError err) {
        System.err.println(err.generateWarning(tk.getRawText()));
        throw new IllegalStateException();
      }

      if (expression != null) {
        System.out.println(expression.stringify("  ", 0));
        System.out.println("result=" + expression.evaluate(dummyContext, valueInterpreter));
      }

      System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
