package me.blvckbytes.gpeee;

@FunctionalInterface
public interface IDebugLogger {

  void log(DebugLogLevel level, String message);

}
