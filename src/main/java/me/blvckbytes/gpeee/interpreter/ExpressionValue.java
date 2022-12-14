package me.blvckbytes.gpeee.interpreter;

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

  public ExpressionValue add(ExpressionValue value) {return null;}
  public ExpressionValue subtract(ExpressionValue value) {return null;}
  public ExpressionValue multiply(ExpressionValue value) {return null;}
  public ExpressionValue divide(ExpressionValue value) {return null;}
  public ExpressionValue power(ExpressionValue value) {return null;}
  public ExpressionValue modulo(ExpressionValue value) {return null;}

  public boolean equalsTo(ExpressionValue other, boolean strict) { return false; }

  public int compareTo(ExpressionValue other) { return 0; }

  public ExpressionValue concatenate(ExpressionValue other) { return null; };

  public ExpressionValue or(ExpressionValue other) { return null; };
  public ExpressionValue and(ExpressionValue other) { return null; };

  public ExpressionValue invert() {
    return null;
  }

  public static ExpressionValue fromInteger(Integer value) { return null; }
  public static ExpressionValue fromDouble(Double value) { return null; }
  public static ExpressionValue fromString(String value) { return null; }
  public static ExpressionValue fromBoolean(Boolean value) { return null; }
  public static ExpressionValue fromNull() { return null; }

}
