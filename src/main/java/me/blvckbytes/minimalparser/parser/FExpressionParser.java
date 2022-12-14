package me.blvckbytes.minimalparser.parser;

import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.expression.AExpression;
import me.blvckbytes.minimalparser.tokenizer.ITokenizer;

@FunctionalInterface
public interface FExpressionParser {

  AExpression apply(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int selfPrecedence) throws AParserError;

}
