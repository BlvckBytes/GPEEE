package me.blvckbytes.gpeee.parser.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public abstract class AExpression {

  public abstract String expressionify();

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
        else if (value instanceof List) {
          List<?> valueList = (List<?>) value;

          StringBuilder listBuilder = new StringBuilder("[\n");

          for (Object item : valueList) {
            if (item instanceof AExpression) {
              listBuilder.append(indent)
                .append(indentWidth.repeat(2))
                .append(((AExpression) item).stringify(indentWidth, indentLevel + 2))
                .append('\n');
            } else {
              listBuilder.append(indent)
                .append(indentWidth.repeat(2))
                .append(item.toString())
                .append('\n');
            }
          }

          result.append(listBuilder).append(indent).append(indentWidth).append("]");
        }
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
