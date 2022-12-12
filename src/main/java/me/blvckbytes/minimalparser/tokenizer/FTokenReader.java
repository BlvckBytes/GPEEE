package me.blvckbytes.minimalparser.tokenizer;

import me.blvckbytes.minimalparser.error.AParserError;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FTokenReader {

  @Nullable String apply(ITokenizer tokenizer) throws AParserError;
}
