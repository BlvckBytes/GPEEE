package me.blvckbytes.minimalparser;

public enum CollectorResult {
  // No next required character to read
  NO_NEXT_CHAR,

  // A character in sequence mismatched
  CHAR_MISMATCH,

  // Whole sequence was okay
  READ_OKAY
  ;
}
