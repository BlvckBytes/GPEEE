package me.blvckbytes.minimalparser.functions;

@FunctionalInterface
public interface IExpressionFunction {

  Object apply(Object[] args);

}
