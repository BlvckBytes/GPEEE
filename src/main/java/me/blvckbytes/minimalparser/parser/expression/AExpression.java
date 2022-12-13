package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class AExpression {

  public abstract Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError;

  protected Object evaluateExpression(@Nullable Object maybeExpression, IEvaluationContext context, IValueInterpreter valueInterpreter) {
    if (maybeExpression == null)
      return null;

    if (!(maybeExpression instanceof AExpression))
      return maybeExpression;

    return ((AExpression) maybeExpression).evaluate(context, valueInterpreter);
  }

  public String stringify(String indentWidth, int indentLevel) throws Exception {
    String indent = indentWidth.repeat(indentLevel);
    StringBuilder result = new StringBuilder(getClass().getSimpleName() + " {\n");

    Class<?> currentClass = getClass();
    boolean firstField = true;

    while (currentClass != Object.class) {
      for (Field f : currentClass.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()))
          continue;

        if (!f.trySetAccessible())
          continue;

        if (!firstField)
          result.append(",\n");

        Object value = f.get(this);

        result
          .append(indent)
          .append(indentWidth)
          .append(f.getName())
          .append('=');

        if (value instanceof AExpression)
          result.append(((AExpression) value).stringify(indentWidth, indentLevel + 1));
        else
          result.append(value);

        firstField = false;
      }

      currentClass = currentClass.getSuperclass();
    }

    if (!firstField)
      result.append("\n");

    result.append(indent).append("}");

    return result.toString();
  }
}
