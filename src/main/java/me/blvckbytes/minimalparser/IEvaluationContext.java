package me.blvckbytes.minimalparser;

import me.blvckbytes.minimalparser.functions.IExpressionFunction;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface IEvaluationContext {

  Map<String, IExpressionFunction> getFunctions();

  Map<String, Supplier<Object>> getVariables();

  Optional<String> performLookup(String table, String key);

}
