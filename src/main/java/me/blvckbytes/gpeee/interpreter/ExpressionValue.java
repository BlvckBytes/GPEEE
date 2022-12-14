package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.functions.FExpressionFunction;

public class ExpressionValue {

  /*
    Can be:
    - Integer
    - Double
    - String
    - List<Integer>
    - List<Double>
    - List<String>
    - Callback
  */

  /*
    When comparing:

    Equality:
    - EQUAL: a == b || str(a).lower().strip() == str(b).lower().strip()
    - EQUAL_EXACT: a == b || str(a) == str(b)

    Math, Comparison:
    Empty String, Empty List: 0
    Non-Empty String, Non-Empty List, Callback: 1

    Negation:
    True: False
    False: True
    <= 0: 1
    > 0: 0
    String, List, Callback: noop

    Boolean Operation:
    >0: True
    <=0: False
    Empty String, Empty List: False
    Non-Empty String, Non-Empty List, Callback: True

    Flip Sign:
    Integer, Double: *-1
    Others: noop
   */

  /**
   * This wrapper class can only be instantiated by using it's static constructor functions.
   */
  private ExpressionValue() {}

  //=========================================================================//
  //                                   Math                                  //
  //=========================================================================//

  public ExpressionValue add(ExpressionValue value) {
    return null;
  }

  public ExpressionValue subtract(ExpressionValue value) {
    return null;
  }

  public ExpressionValue multiply(ExpressionValue value) {
    return null;
  }

  public ExpressionValue divide(ExpressionValue value) {
    return null;
  }

  public ExpressionValue power(ExpressionValue value) {
    return null;
  }

  public ExpressionValue modulo(ExpressionValue value) {
    return null;
  }

  //=========================================================================//
  //                                 Equality                                //
  //=========================================================================//

  public boolean equalsTo(ExpressionValue other, boolean strict) {
    return false;
  }

  //=========================================================================//
  //                                Comparison                               //
  //=========================================================================//

  public int compareTo(ExpressionValue other) {
    return 0;
  }

  //=========================================================================//
  //                               Concatenation                             //
  //=========================================================================//

  public ExpressionValue concatenate(ExpressionValue other) {
    return null;
  }

  //=========================================================================//
  //                               Boolean Logic                             //
  //=========================================================================//

  public ExpressionValue or(ExpressionValue other) {
    return null;
  }

  public ExpressionValue and(ExpressionValue other) {
    return null;
  }

  public ExpressionValue invert() {
    return null;
  }

  //=========================================================================//
  //                                 Convert To                              //
  //=========================================================================//

  public Integer asInteger() {
    return null;
  }

  public Double asDouble() {
    return null;
  }

  public String asString() {
    return null;
  }

  public Boolean asBoolean() {
    return null;
  }

  public FExpressionFunction asFunction() {
    return null;
  }

  //=========================================================================//
  //                                 Type Info                               //
  //=========================================================================//

  public boolean isInteger() {
    return false;
  }

  public boolean isDouble() {
    return false;
  }

  public boolean isString() {
    return false;
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isFunction() {
    return false;
  }

  public boolean isNull() {
    return false;
  }

  //=========================================================================//
  //                                Convert From                             //
  //=========================================================================//

  public static ExpressionValue fromInteger(Integer value) {
    return null;
  }

  public static ExpressionValue fromDouble(Double value) {
    return null;
  }

  public static ExpressionValue fromString(String value) {
    return null;
  }

  public static ExpressionValue fromBoolean(Boolean value) {
    return null;
  }

  public static ExpressionValue fromNull() {
    return null;
  }

  public static ExpressionValue fromFunction(FExpressionFunction function) {
    return null;
  }
}
