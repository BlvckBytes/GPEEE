package me.blvckbytes.minimalparser.parser;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ComparisonOperation {

  GREATER_THAN,
  GREATER_THAN_OR_EQUAL,
  LESS_THAN,
  LESS_THAN_OR_EQUAL,
  EQUAL,
  NOT_EQUAL,
  EQUAL_EXACT,
  NOT_EQUAL_EXACT,

}
