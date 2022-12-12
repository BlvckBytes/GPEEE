package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

@Getter
@AllArgsConstructor
public class LookupTableAccessExpression extends AExpression {

  private final String table;
  private final String key;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return context.performLookup(table, key).orElse(null);
  }

  @Override
  public String toString() {
    return "LookupTableAccessExpression (\n" +
      "table='" + table + '\'' + ",\n" +
      "key='" + key + '\'' + "\n" +
    ')';
  }
}
