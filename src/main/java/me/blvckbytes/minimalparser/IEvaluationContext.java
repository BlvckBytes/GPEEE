package me.blvckbytes.minimalparser;

import me.blvckbytes.minimalparser.functions.AExpressionFunction;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface IEvaluationContext {

  Map<String, AExpressionFunction> getFunctions();

  Map<String, Supplier<Object>> getVariables();

  Optional<String> performLookup(String table, String key);

}
