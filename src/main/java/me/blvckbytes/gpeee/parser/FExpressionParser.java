package me.blvckbytes.gpeee.parser;

import me.blvckbytes.gpeee.error.AParserError;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.tokenizer.ITokenizer;

@FunctionalInterface
public interface FExpressionParser {

  AExpression apply(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int selfPrecedence) throws AParserError;

}
