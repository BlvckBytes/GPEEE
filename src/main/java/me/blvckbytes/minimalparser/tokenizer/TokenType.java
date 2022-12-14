package me.blvckbytes.minimalparser.tokenizer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.error.UnterminatedStringError;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

@Getter
@AllArgsConstructor
public enum TokenType {

  //=========================================================================//
  //                                 Literals                                //
  //=========================================================================//

  TRUE(TokenCategory.LITERAL, "true", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, "true".toCharArray())),
  FALSE(TokenCategory.LITERAL, "false", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, "false".toCharArray())),
  NULL(TokenCategory.LITERAL, "null", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, "null".toCharArray())),

  //=========================================================================//
  //                                  Values                                 //
  //=========================================================================//

  IDENTIFIER(TokenCategory.VALUE, null, tokenizer -> {
    StringBuilder result = new StringBuilder();

    char firstChar = tokenizer.nextChar();

    // Identifiers always start with letters
    if (!isIdentifierChar(firstChar, true))
      return null;

    result.append(firstChar);

    // Collect until no more identifier chars remain
    while (tokenizer.hasNextChar() && isIdentifierChar(tokenizer.peekNextChar(), false))
      result.append(tokenizer.nextChar());

    return result.toString();
  }),

  // -?[0-9]+
  INT(TokenCategory.VALUE, null, tokenizer -> {
    StringBuilder result = new StringBuilder();

    if (collectDigits(tokenizer, result, false) != CollectorResult.READ_OKAY)
      return null;

    return result.toString();
  }),

  // -?[0-9]*.?[0-9]+
  FLOAT(TokenCategory.VALUE, null, tokenizer -> {
    StringBuilder result = new StringBuilder();

    // Shorthand 0.x notation
    if (tokenizer.peekNextChar() == '.') {
      result.append('0');
      result.append(tokenizer.nextChar());

      // Collect as many digits as possible
      if (collectDigits(tokenizer, result, false) != CollectorResult.READ_OKAY)
        return null;

      return result.toString();
    }

    // A float starts out like an integer
    if (collectDigits(tokenizer, result, true) != CollectorResult.READ_OKAY)
      return null;

    // Missing decimal point
    if (!tokenizer.hasNextChar() || tokenizer.nextChar() != '.')
      return null;

    result.append('.');

    // Collect as many digits as possible
    if (collectDigits(tokenizer, result, false) != CollectorResult.READ_OKAY)
      return null;

    return result.toString();
  }),

  STRING(TokenCategory.VALUE, null, tokenizer -> {
    int startRow = tokenizer.getCurrentRow(), startCol = tokenizer.getCurrentCol();

    // String start marker not found
    if (tokenizer.nextChar() != '"')
      return null;

    StringBuilder result = new StringBuilder();

    boolean isTerminated = false;
    while (tokenizer.hasNextChar()) {
      char c = tokenizer.nextChar();

      if (c == '"') {
        Character previous = tokenizer.previousChar();

        // Escaped double quote character, collect
        if (previous != null && previous == '\\') {
          result.append(c);
          continue;
        }

        isTerminated = true;
        break;
      }

      if (c == 's') {
        Character previous = tokenizer.previousChar();

        // Escaped s character, substitute for single quote
        if (previous != null && previous == '\\') {
          result.deleteCharAt(result.length() - 1);
          result.append('\'');
          continue;
        }
      }

      result.append(c);
    }

    // Strings need to be terminated
    if (!isTerminated)
      throw new UnterminatedStringError(startRow, startCol);

    return result.toString();
  }),

  //=========================================================================//
  //                                Operators                                //
  //=========================================================================//

  EXPONENT(TokenCategory.OPERATOR, "^", tokenizer -> tokenizer.nextChar() == '^' ? "^" : null),
  MULTIPLICATION(TokenCategory.OPERATOR, "*", tokenizer -> tokenizer.nextChar() == '*' ? "*" : null),
  DIVISION(TokenCategory.OPERATOR, "/", tokenizer -> tokenizer.nextChar() == '/' ? "/" : null),
  MODULO(TokenCategory.OPERATOR, "%", tokenizer -> tokenizer.nextChar() == '%' ? "%" : null),
  PLUS(TokenCategory.OPERATOR, "+", tokenizer -> tokenizer.nextChar() == '+' ? "+" : null),
  MINUS(TokenCategory.OPERATOR, "-", tokenizer -> tokenizer.nextChar() == '-' ? "-" : null),

  GREATER_THAN(TokenCategory.OPERATOR, ">", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, '=', '>')),
  GREATER_THAN_OR_EQUAL(TokenCategory.OPERATOR, ">=", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, '>', '=')),
  LESS_THAN(TokenCategory.OPERATOR, "<", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, '=', '<')),
  LESS_THAN_OR_EQUAL(TokenCategory.OPERATOR, "<=", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, '<', '=')),
  VALUE_EQUALS(TokenCategory.OPERATOR, "==", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, '=', '=', '=')),
  VALUE_NOT_EQUALS(TokenCategory.OPERATOR, "!=", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, '=', '!', '=')),
  VALUE_EQUALS_EXACT(TokenCategory.OPERATOR, "===", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, '=', '=', '=')),
  VALUE_NOT_EQUALS_EXACT(TokenCategory.OPERATOR, "!==", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, '!', '=', '=')),

  CONCATENATE(TokenCategory.OPERATOR, "&", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, '&')),

  BOOL_NOT(TokenCategory.KEYWORD, "not", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, "not".toCharArray())),
  BOOL_AND(TokenCategory.KEYWORD, "and", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, "and".toCharArray())),
  BOOL_OR(TokenCategory.KEYWORD, "or", tokenizer -> tryCollectSequenceWithNextCheck(tokenizer, null, "or".toCharArray())),

  //=========================================================================//
  //                                 Symbols                                 //
  //=========================================================================//

  PARENTHESIS_OPEN(TokenCategory.SYMBOL, "(", tokenizer -> tokenizer.nextChar() == '(' ? "(" : null),
  PARENTHESIS_CLOSE(TokenCategory.SYMBOL, ")", tokenizer -> tokenizer.nextChar() == ')' ? ")" : null),
  BRACKET_OPEN(TokenCategory.SYMBOL, "[", tokenizer -> tokenizer.nextChar() == ']' ? "]" : null),
  BRACKET_CLOSE(TokenCategory.SYMBOL, "]", tokenizer -> tokenizer.nextChar() == '[' ? "[" : null),
  COMMA(TokenCategory.SYMBOL, ",", tokenizer -> tokenizer.nextChar() == ',' ? "," : null),

  //=========================================================================//
  //                                Invisible                                //
  //=========================================================================//

  COMMENT(TokenCategory.INVISIBLE, null, tokenizer -> {
    StringBuilder result = new StringBuilder();

    if (tokenizer.nextChar() != '#')
      return null;

    while (tokenizer.hasNextChar() && tokenizer.peekNextChar() != '\n')
      result.append(tokenizer.nextChar());

    return result.toString();
  }),
  ;

  private final TokenCategory category;
  private final String representation;
  private final @Nullable FTokenReader tokenReader;

  public static final TokenType[] valuesInTrialOrder;
  public static final TokenType[] nonValueTypes;
  public static final TokenType[] valueTypes;

  static {
    valuesInTrialOrder =
      Arrays.stream(values())
      .sorted(Comparator.comparingInt(tt -> tt.getCategory().ordinal()))
      .toArray(TokenType[]::new);

    nonValueTypes = Arrays.stream(values())
      .filter(type -> type.getCategory() != TokenCategory.VALUE)
      .toArray(TokenType[]::new);

    valueTypes = Arrays.stream(values())
      .filter(type -> type.getCategory() == TokenCategory.VALUE)
      .toArray(TokenType[]::new);
  }

  private static CollectorResult collectDigits(ITokenizer tokenizer, StringBuilder result, boolean stopBeforeDot) {
    if (!tokenizer.hasNextChar())
      return CollectorResult.NO_NEXT_CHAR;

    int initialLength = result.length();

    while (tokenizer.hasNextChar()) {
      char c = tokenizer.nextChar();

      // Collect as many digits as possible
      if (c >= '0' && c <= '9')
        result.append(c);

      // Whitespace or newline stops the number notation
      else if (tokenizer.isConsideredWhitespace(c) || c == '\n')
        break;

      else if (c == '.' && stopBeforeDot) {
        tokenizer.undoNextChar();
        break;
      }

      else {
        tokenizer.undoNextChar();

        if (result.length() - initialLength > 0 && wouldFollow(tokenizer, nonValueTypes))
          return CollectorResult.READ_OKAY;

        return CollectorResult.CHAR_MISMATCH;
      }
    }

    return CollectorResult.READ_OKAY;
  }

  private static @Nullable String tryCollectSequenceWithNextCheck(ITokenizer tokenizer, @Nullable Character notNext, char... sequence) {
    StringBuilder result = new StringBuilder();

    if (collectSequence(tokenizer, result, sequence) != CollectorResult.READ_OKAY)
      return null;

    if (notNext != null && tokenizer.hasNextChar() && tokenizer.peekNextChar() == notNext)
      return null;

    return result.toString();
  }

  private static char charToLowerCase(char input) {
    // Upper case character, convert to lowercase by shifting over 32 places
    if (input >= 'A' && input <= 'Z')
      input += 32;
    return input;
  }

  private static CollectorResult collectSequence(ITokenizer tokenizer, StringBuilder result, char... sequence) {
    for (char c : sequence) {
      if (!tokenizer.hasNextChar())
        return CollectorResult.NO_NEXT_CHAR;

      if (charToLowerCase(tokenizer.nextChar()) == charToLowerCase(c)) {
        result.append(c);
        continue;
      }

      return CollectorResult.CHAR_MISMATCH;
    }

    return CollectorResult.READ_OKAY;
  }

  private static boolean wouldFollow(ITokenizer tokenizer, TokenType... types) {
    for (TokenType type : types) {
      FTokenReader reader = type.getTokenReader();

      // Non-implemented tokens cannot follow
      if (reader == null)
        continue;

      // Simulate a token read trial
      tokenizer.saveState(false);
      boolean success = type.getTokenReader().apply(tokenizer) != null;
      tokenizer.restoreState(false);

      if (success)
        return true;
    }

    // None matched
    return false;
  }

  /**
   * Checks whether a given character is within the range of allowed
   * characters to make up an identifier token
   * @param c Character in question
   * @param isFirst Whether it's the first char of the token (special rules apply)
   * @return True if allowed, false otherwise
   */
  private static boolean isIdentifierChar(char c, boolean isFirst) {
    return (
      (c >= 'a' && c <= 'z') ||
      (c >= 'A' && c <= 'Z') ||
      // Underscores as well as numbers aren't allowed as the first character
      (!isFirst && (c == '_' || c >= '0' && c <= '9'))
    );
  }
}
