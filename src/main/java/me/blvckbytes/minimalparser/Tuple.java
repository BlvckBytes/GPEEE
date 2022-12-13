package me.blvckbytes.minimalparser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Tuple<U, V> {
  private final U first;
  private final V second;
}
