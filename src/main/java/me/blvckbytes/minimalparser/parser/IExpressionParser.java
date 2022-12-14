package me.blvckbytes.minimalparser.parser;

import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.expression.AExpression;
import me.blvckbytes.minimalparser.tokenizer.ITokenizer;

@FunctionalInterface
public interface IExpressionParser {

  AExpression apply(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int selfPrecedence) throws AParserError;

}
