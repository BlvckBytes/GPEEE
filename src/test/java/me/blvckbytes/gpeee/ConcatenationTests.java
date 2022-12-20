/*
 * MIT License
 *
 * Copyright (c) 2022 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.gpeee;

import org.junit.jupiter.api.Test;

import java.util.List;

public class ConcatenationTests {

  @Test
  public void shouldStringifyValues() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_long", 5)
      .withStaticVariable("my_double", 2.34)
      .withStaticVariable("my_bool", true)
      .withStaticVariable("my_string", "hello, world")
      .withStaticVariable("my_list", List.of(1, 2, 3));

    env.launch(validator -> {
      validator.validate(
        "my_long & my_double & my_bool & my_string & my_list",
        "" +
        env.getVariable("my_long") +
        env.getVariable("my_double") +
        env.getVariable("my_bool") +
        env.getVariable("my_string") +
        env.stringify(env.getVariable("my_list"))
      );

      validator.validate(
        "my_long & 2 & my_double & false & my_bool & my_string & \"test\" & my_list & 2.44",
        env.getVariable("my_long") + "2" +
        env.getVariable("my_double") + "false" +
        env.getVariable("my_bool") +
        env.getVariable("my_string") + "test" +
        env.stringify(env.getVariable("my_list")) + "2.44"
      );
    });
  }

  @Test
  public void shouldConcatenateImmediates() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate(
          "3 & 3.2 & true & null & \"hello\" & \" \" & \"world\"",
          "33.2true<null>hello world"
        );
      });
  }
}
