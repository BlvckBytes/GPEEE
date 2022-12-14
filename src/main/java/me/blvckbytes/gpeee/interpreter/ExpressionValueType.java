package me.blvckbytes.gpeee.interpreter;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ExpressionValueType {

  INTEGER,
  DOUBLE,
  STRING,
  BOOLEAN,
  NULL,
  FUNCTION,
  LIST

}
