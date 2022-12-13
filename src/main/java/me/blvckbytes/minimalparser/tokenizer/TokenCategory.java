package me.blvckbytes.minimalparser.tokenizer;

/**
 * Categories of token types, defined in the order of tokenization trial. This means
 * that - for example - a keyword should be tried to be tokenized before a value,
 * as that value could be an identifier having the same name as a reserved
 * keyword (which would be illegal).
 */
public enum TokenCategory {
  // Literals and keywords share the same importance, as
  // they should never collide by design
  LITERAL, KEYWORD,

  // Values include identifiers, so they're definitely after reserved words
  VALUE,

  // Operators and symbols share the same importance, as
  // they should never collide by design
  OPERATOR, SYMBOL,

  // Invisible tokens are not of interest to the AST
  INVISIBLE
}
