package me.blvckbytes.gpeee.interpreter;

import lombok.Getter;
import me.blvckbytes.gpeee.functions.FExpressionFunction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ExpressionValue {

  public static final ExpressionValue NULL = new ExpressionValue(null, ExpressionValueType.NULL);
  public static final ExpressionValue TRUE = new ExpressionValue(true, ExpressionValueType.BOOLEAN);
  public static final ExpressionValue FALSE = new ExpressionValue(false, ExpressionValueType.BOOLEAN);
  public static final ExpressionValue EMPTY_LIST = new ExpressionValue(new ArrayList<>());

  private final @Nullable Object value;
  private final @Nullable List<ExpressionValue> values;

  @Getter
  private final ExpressionValueType type;

  private ExpressionValue(@Nullable Object value, ExpressionValueType type) {
    this.values = null;
    this.value = value;
    this.type = type;
  }

  private ExpressionValue(@Nullable List<ExpressionValue> values) {
    this.values = values;
    this.value = null;
    this.type = ExpressionValueType.LIST;
  }

  @Override
  public String toString() {
    switch (type) {
      default:
      case INTEGER:
      case DOUBLE:
      case STRING:
      case NULL:
      case FUNCTION:
        return value == null ? "null" : value.toString();
      case LIST:
        assert values != null;
        return "[" + values.stream().map(ExpressionValue::toString).collect(Collectors.joining(",")) + "]";
    }
  }

  //=========================================================================//
  //                                   Math                                  //
  //=========================================================================//

  private ExpressionValue doMathOperationWith(
    ExpressionValue value,
    BiFunction<Integer, Integer, Integer> intHandler,
    BiFunction<Double, Double, Double> doubleHandler,
    boolean isDivision
  ) {
    // Doubles result if either one or both operands are doubles
    if (
      (type == ExpressionValueType.DOUBLE && value.type == ExpressionValueType.INTEGER) ||
      (type == ExpressionValueType.INTEGER && value.type == ExpressionValueType.DOUBLE) ||
      (type == ExpressionValueType.DOUBLE && value.type == ExpressionValueType.DOUBLE)
    )
      return new ExpressionValue(doubleHandler.apply(interpretAsDouble(), value.interpretAsDouble()), ExpressionValueType.DOUBLE);

    Integer self = interpretAsInteger(), other = value.interpretAsInteger();

    if (isDivision) {

      // It's okay to collapse all meanings of the division by zero
      // into this single case for the purpose of this language
      if (other == 0)
        other = 1;

      // Would not divide evenly
      if ((type == ExpressionValueType.INTEGER && value.type == ExpressionValueType.INTEGER) && (self % other != 0))
        return new ExpressionValue(doubleHandler.apply(self.doubleValue(), other.doubleValue()), ExpressionValueType.DOUBLE);
    }

    // Handle as integers in all other cases
    return new ExpressionValue(intHandler.apply(self, other), ExpressionValueType.INTEGER);
  }

  public ExpressionValue add(ExpressionValue value) {
    return doMathOperationWith(value, Integer::sum, Double::sum, false);
  }

  public ExpressionValue subtract(ExpressionValue value) {
    return doMathOperationWith(value, (a, b) -> a - b, (a, b) -> a - b, false);
  }

  public ExpressionValue multiply(ExpressionValue value) {
    return doMathOperationWith(value, (a, b) -> a * b, (a, b) -> a * b, false);
  }

  public ExpressionValue divide(ExpressionValue value) {
    return doMathOperationWith(value, (a, b) -> a / b, (a, b) -> a / b, true);
  }

  public ExpressionValue power(ExpressionValue value) {
    return doMathOperationWith(value, (a, b) -> (int) Math.pow(a, b), Math::pow, false);
  }

  public ExpressionValue modulo(ExpressionValue value) {
    return doMathOperationWith(value, (a, b) -> a % b, (a, b) -> a % b, false);
  }

  //=========================================================================//
  //                                 Equality                                //
  //=========================================================================//

  public boolean equalsTo(ExpressionValue other, boolean strict) {
    // Both are lists and need to be compared by size and elements
    if (type == ExpressionValueType.LIST && other.type == ExpressionValueType.LIST) {
      List<ExpressionValue> valuesSelf = interpretAsList();
      List<ExpressionValue> valuesOther = other.interpretAsList();

      // Reference matches
      if (valuesOther == valuesSelf)
        return true;

      // Size mismatch, elements cannot equal
      if (valuesOther.size() != valuesSelf.size())
        return false;

      for (int i = 0; i < valuesOther.size(); i++) {
        // Element mismatch
        if (!valuesSelf.get(i).equalsTo(valuesOther.get(i), strict))
          return false;
      }

      // All elements matched
      return true;
    }

    // Null always equals itself
    if (type == null && other.type == null)
      return true;

    if (strict) {
      // Type mismatch
      if (type != other.type)
        return false;

      // Compare numbers
      if (type == ExpressionValueType.INTEGER || type == ExpressionValueType.DOUBLE)
        return compareTo(other) == 0;

      // Compare strings case-sensitive
      if (type == ExpressionValueType.STRING)
        return interpretAsString().equals(other.interpretAsString());

      // Unknown case
      return false;
    }

    // Compare numbers if both sides are numbers
    if (
      (type == ExpressionValueType.INTEGER || type == ExpressionValueType.DOUBLE) &&
      (other.type == ExpressionValueType.INTEGER || other.type == ExpressionValueType.DOUBLE)
    )
      return compareTo(other) == 0;

    // Compare as strings case-insensitive as well as trimmed
    return interpretAsString().trim().equals(other.interpretAsString().trim());
  }

  //=========================================================================//
  //                                Comparison                               //
  //=========================================================================//

  public int compareTo(ExpressionValue other) {
    if (type == ExpressionValueType.DOUBLE || other.type == ExpressionValueType.DOUBLE)
      return interpretAsDouble().compareTo(other.interpretAsDouble());
    return interpretAsInteger().compareTo(other.interpretAsInteger());
  }

  //=========================================================================//
  //                               Concatenation                             //
  //=========================================================================//

  public ExpressionValue concatenate(ExpressionValue other) {
    return new ExpressionValue(interpretAsString() + other.interpretAsString(), ExpressionValueType.STRING);
  }

  //=========================================================================//
  //                               Boolean Logic                             //
  //=========================================================================//

  public ExpressionValue or(ExpressionValue other) {
    return new ExpressionValue(interpretAsBoolean() || other.interpretAsBoolean(), ExpressionValueType.BOOLEAN);
  }

  public ExpressionValue and(ExpressionValue other) {
    return new ExpressionValue(interpretAsBoolean() && other.interpretAsBoolean(), ExpressionValueType.BOOLEAN);
  }

  public ExpressionValue invert() {
    return new ExpressionValue(!interpretAsBoolean(), ExpressionValueType.BOOLEAN);
  }

  //=========================================================================//
  //                                Interpret As                             //
  //=========================================================================//

  public Integer interpretAsInteger() {
    if (value == null)
      return 0;

    if (type == ExpressionValueType.LIST) {
      assert values != null;
      return values.size() > 0 ? 1 : 0;
    }

    if (type == ExpressionValueType.INTEGER)
      return (Integer) value;

    if (type == ExpressionValueType.DOUBLE)
      return ((Double) value).intValue();

    if (type == ExpressionValueType.STRING)
      return ((String) value).isEmpty() ? 0 : 1;

    if (type == ExpressionValueType.BOOLEAN)
      return ((Boolean) value) ? 0 : 1;

    return 0;
  }

  public Double interpretAsDouble() {
    if (value == null)
      return 0D;

    if (type == ExpressionValueType.DOUBLE)
      return ((Double) value);

    if (type == ExpressionValueType.INTEGER)
      return ((Integer) value).doubleValue();

    return interpretAsInteger().doubleValue();
  }

  public String interpretAsString() {
    return toString();
  }

  public Boolean interpretAsBoolean() {
    if (value != null && type == ExpressionValueType.BOOLEAN)
      return (Boolean) value;
    return interpretAsInteger() > 0;
  }

  public List<ExpressionValue> interpretAsList() {
    if (type == ExpressionValueType.LIST)
      return values;

    ArrayList<ExpressionValue> res = new ArrayList<>();
    res.add(this);

    return res;
  }

  //=========================================================================//
  //                                 Convert To                              //
  //=========================================================================//

  public @Nullable Integer asInteger() {
    if (value == null || type != ExpressionValueType.INTEGER)
      return null;

    return (Integer) value;
  }

  public @Nullable Double asDouble() {
    if (value == null || type != ExpressionValueType.DOUBLE)
      return null;

    return (Double) value;
  }

  public @Nullable String asString() {
    if (value == null || type != ExpressionValueType.STRING)
      return null;

    return (String) value;
  }

  public @Nullable Boolean asBoolean() {
    if (value == null || type != ExpressionValueType.BOOLEAN)
      return null;

    return (Boolean) value;
  }

  public @Nullable FExpressionFunction asFunction() {
    if (value == null || type != ExpressionValueType.FUNCTION)
      return null;

    return (FExpressionFunction) value;
  }

  public @Nullable List<ExpressionValue> asList() {
    if (type != ExpressionValueType.LIST)
      return null;
    return values;
  }

  //=========================================================================//
  //                                Convert From                             //
  //=========================================================================//

  public static ExpressionValue fromInteger(Integer value) {
    return new ExpressionValue(value, ExpressionValueType.INTEGER);
  }

  public static ExpressionValue fromDouble(Double value) {
    return new ExpressionValue(value, ExpressionValueType.DOUBLE);
  }

  public static ExpressionValue fromString(String value) {
    return new ExpressionValue(value, ExpressionValueType.STRING);
  }

  public static ExpressionValue fromBoolean(Boolean value) {
    return value ? TRUE : FALSE;
  }

  public static ExpressionValue fromNull() {
    return NULL;
  }

  public static ExpressionValue fromFunction(FExpressionFunction function) {
    return new ExpressionValue(function, ExpressionValueType.FUNCTION);
  }

  public static ExpressionValue fromListAutoWrap(List<?> list) throws IllegalArgumentException {
    if (list == null || list.size() == 0)
      return new ExpressionValue(new ArrayList<>());

    List<ExpressionValue> mapped = new ArrayList<>();

    for (Object item : list) {
      if (item instanceof Integer)
        mapped.add(ExpressionValue.fromInteger(((Integer) item)));
      else
        throw new IllegalArgumentException("Unsupported list item of type " + item.getClass());
    }

    return new ExpressionValue(mapped);
  }

  public static ExpressionValue fromList(List<ExpressionValue> list) {
    // "Null lists" should never exist as expression values
    if (list == null)
      list = new ArrayList<>();

    return new ExpressionValue(list);
  }
}
