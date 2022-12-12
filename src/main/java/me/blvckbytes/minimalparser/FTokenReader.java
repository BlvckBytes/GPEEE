package me.blvckbytes.minimalparser;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FTokenReader {

  @Nullable String apply(ITokenizer tokenizer);
}
