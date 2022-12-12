package me.blvckbytes.minimalparser.parser;

import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.expression.AExpression;
import me.blvckbytes.minimalparser.tokenizer.ITokenizer;
import me.blvckbytes.minimalparser.tokenizer.Token;

public class ExpressionParser {

  private final ITokenizer tokenizer;

  public ExpressionParser(ITokenizer tokenizer) {
    this.tokenizer = tokenizer;
  }

  public AExpression parse() throws AParserError {
    while (true) {
      try {
        Token curr = tokenizer.nextToken();

        if (curr == null)
          break;

        System.out.println(curr);
      } catch (AParserError err) {
        System.err.println(err.generateWarning(tokenizer.getRawText()));
        break;
      }
    }

    // TODO: Implement
    return null;
  }
}
