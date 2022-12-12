package me.blvckbytes.minimalparser;

import me.blvckbytes.minimalparser.error.AParserError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) {
    try {
      String input = getResourceFileAsString("input.txt");

      if (input == null) {
        System.err.println("Could not read input file!");
        return;
      }

      Tokenizer tk = new Tokenizer(input);

      while (true) {
        try {
          Token curr = tk.nextToken();

          if (curr == null)
            break;

          System.out.println(curr);
        } catch (AParserError err) {
          System.err.println(err.generateWarning(input));
          break;
        }
      }

      System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Reads given resource file as a string.
   *
   * @param fileName path to the resource file
   * @return the file's contents
   * @throws IOException if read fails for any reason
   */
  static String getResourceFileAsString(String fileName) throws IOException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(fileName)) {
      if (is == null) return null;
      try (InputStreamReader isr = new InputStreamReader(is);
           BufferedReader reader = new BufferedReader(isr)) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
  }
}
