package me.blvckbytes.gpeee.tokenizer;

import me.blvckbytes.gpeee.error.AParserError;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FTokenReader {

  @Nullable String apply(ITokenizer tokenizer) throws AParserError;
}
