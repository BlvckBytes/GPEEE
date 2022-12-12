package me.blvckbytes.minimalparser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TokenType {

  //=========================================================================//
  //                                  Values                                 //
  //=========================================================================//

  IDENTIFIER(TokenCategory.VALUE, null),

  // -?[0-9]+
  LITERAL_INT(TokenCategory.VALUE, tokenizer -> {
    StringBuilder result = new StringBuilder();

    if (collectInteger(tokenizer, result, false) != CollectorResult.READ_OKAY)
      return null;

    return result.toString();
  }),

  // -?[0-9]*.?[0-9]+
  LITERAL_FLOAT(TokenCategory.VALUE, tokenizer -> {
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
    if (collectInteger(tokenizer, result, true) != CollectorResult.READ_OKAY)
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

  LITERAL_STRING(TokenCategory.VALUE, tokenizer -> {
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

      result.append(c);
    }

    // Strings need to be terminated
    if (!isTerminated) {
      // TODO: Throw descriptive error
      return null;
    }

    return result.toString();
  }),

  //=========================================================================//
  //                                Operators                                //
  //=========================================================================//

  OP_CONCATENATE(TokenCategory.OPERATOR, tokenizer -> tokenizer.nextChar() == '+' ? "+" : null),

  OP_GREATER_THAN(TokenCategory.OPERATOR, tokenizer -> {
    if (tokenizer.nextChar() == '>') {

      // Would be less than or equal
      if (tokenizer.peekNextChar() == '=')
        return null;

      return ">";
    }

    return null;
  }),

  OP_GREATER_THAN_OR_EQUAL(TokenCategory.OPERATOR, tokenizer -> collectSequenceOrNullStr(tokenizer, '>', '=')),

  OP_LESS_THAN(TokenCategory.OPERATOR, tokenizer -> {
    if (tokenizer.nextChar() == '<') {

      // Would be less than or equal
      if (tokenizer.peekNextChar() == '=')
        return null;

      return "<";
    }

    return null;
  }),

  OP_LESS_THAN_OR_EQUAL(TokenCategory.OPERATOR, tokenizer -> collectSequenceOrNullStr(tokenizer, '<', '=')),

  OP_BOOL_OR(TokenCategory.OPERATOR, tokenizer -> collectSequenceOrNullStr(tokenizer, '|', '|')),

  OP_BOOL_AND(TokenCategory.OPERATOR, tokenizer -> collectSequenceOrNullStr(tokenizer, '&', '&')),

  OP_EQUALS(TokenCategory.OPERATOR, tokenizer -> collectSequenceOrNullStr(tokenizer, '=', '=')),

  //=========================================================================//
  //                                 Symbols                                 //
  //=========================================================================//

  SYM_PAREN_OPEN(TokenCategory.SYMBOL, tokenizer -> tokenizer.nextChar() == '(' ? "(" : null),
  SYM_PAREN_CLOSE(TokenCategory.SYMBOL, tokenizer -> tokenizer.nextChar() == ')' ? ")" : null),
  SYM_COMMA(TokenCategory.SYMBOL, tokenizer -> tokenizer.nextChar() == ',' ? "," : null),
  ;

  private final TokenCategory category;
  private final @Nullable FTokenReader tokenReader;

  public static final TokenType[] values;
  public static final TokenType[] nonValueTypes;

  static {
    values = values();
    nonValueTypes = Arrays.stream(values()).filter(type -> type.getCategory() != TokenCategory.VALUE).toArray(TokenType[]::new);
  }

  private static CollectorResult collectInteger(ITokenizer tokenizer, StringBuilder result, boolean stopAtDot) {
    if (!tokenizer.hasNextChar())
      return CollectorResult.NO_NEXT_CHAR;

    char firstChar = tokenizer.nextChar();

    // May start with any digit or a minus sign (negative number)
    if ((firstChar >= '0' && firstChar <= '9') || firstChar == '-')
      result.append(firstChar);
    else {
      tokenizer.undoNextChar();
      return CollectorResult.CHAR_MISMATCH;
    }

    CollectorResult digitResult = collectDigits(tokenizer, result, stopAtDot);

    if (digitResult == CollectorResult.CHAR_MISMATCH)
      return CollectorResult.CHAR_MISMATCH;

    // Negative number started but no digits available
    if (digitResult == CollectorResult.NO_NEXT_CHAR && firstChar == '-')
      return CollectorResult.NO_NEXT_CHAR;

    return CollectorResult.READ_OKAY;
  }

  private static CollectorResult collectDigits(ITokenizer tokenizer, StringBuilder result, boolean stopBeforeDot) {
    if (!tokenizer.hasNextChar())
      return CollectorResult.NO_NEXT_CHAR;

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

        if (wouldFollow(tokenizer, nonValueTypes))
          return CollectorResult.READ_OKAY;

        return CollectorResult.CHAR_MISMATCH;
      }
    }

    return CollectorResult.READ_OKAY;
  }

  private static String collectSequenceOrNullStr(ITokenizer tokenizer, char... sequence) {
    StringBuilder result = new StringBuilder();

    if (collectSequence(tokenizer, result, sequence) != CollectorResult.READ_OKAY)
      return null;

    return result.toString();
  }

  private static CollectorResult collectSequence(ITokenizer tokenizer, StringBuilder result, char... sequence) {
    for (char c : sequence) {
      if (!tokenizer.hasNextChar())
        return CollectorResult.NO_NEXT_CHAR;

      if (tokenizer.nextChar() == c) {
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
      tokenizer.saveState();
      boolean success = type.getTokenReader().apply(tokenizer) != null;
      tokenizer.restoreState();

      if (success)
        return true;
    }

    // None matched
    return false;
  }
}
