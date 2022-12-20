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

import static org.junit.jupiter.api.Assertions.*;

public class AExpressionTests {

  @Test
  public void shouldStringifyProperly() {

    String resultText = "";

    try {
      resultText = new GPEEE(null).parseString(
        "\"my prefix: \" & iter_cat(my_object.map[\"colormap\"], (it, ind) -> \"(\" & ind & \" -> \" & value(it) & \")\", \", \")"
      ).stringify("  ", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }

    String expectedText =
      "ConcatenationExpression {\n" +
      "  lhs=StringExpression {\n" +
      "    value=my prefix: \n" +
      "  },\n" +
      "  rhs=FunctionInvocationExpression {\n" +
      "    name=IdentifierExpression {\n" +
      "      symbol=iter_cat\n" +
      "    },\n" +
      "    arguments=[\n" +
      "      Tuple(a=IndexExpression {\n" +
      "        lhs=MemberAccessExpression {\n" +
      "          lhs=IdentifierExpression {\n" +
      "            symbol=my_object\n" +
      "          },\n" +
      "          rhs=IdentifierExpression {\n" +
      "            symbol=map\n" +
      "          }\n" +
      "        },\n" +
      "        rhs=StringExpression {\n" +
      "          value=colormap\n" +
      "        }\n" +
      "      }, b=<null>)\n" +
      "      Tuple(a=CallbackExpression {\n" +
      "        signature=[\n" +
      "          IdentifierExpression {\n" +
      "              symbol=it\n" +
      "            }\n" +
      "          IdentifierExpression {\n" +
      "              symbol=ind\n" +
      "            }\n" +
      "        ],\n" +
      "        body=ConcatenationExpression {\n" +
      "          lhs=ConcatenationExpression {\n" +
      "            lhs=ConcatenationExpression {\n" +
      "              lhs=ConcatenationExpression {\n" +
      "                lhs=StringExpression {\n" +
      "                  value=(\n" +
      "                },\n" +
      "                rhs=IdentifierExpression {\n" +
      "                  symbol=ind\n" +
      "                }\n" +
      "              },\n" +
      "              rhs=StringExpression {\n" +
      "                value= -> \n" +
      "              }\n" +
      "            },\n" +
      "            rhs=FunctionInvocationExpression {\n" +
      "              name=IdentifierExpression {\n" +
      "                symbol=value\n" +
      "              },\n" +
      "              arguments=[\n" +
      "                Tuple(a=IdentifierExpression {\n" +
      "                  symbol=it\n" +
      "                }, b=<null>)\n" +
      "              ]\n" +
      "            }\n" +
      "          },\n" +
      "          rhs=StringExpression {\n" +
      "            value=)\n" +
      "          }\n" +
      "        }\n" +
      "      }, b=<null>)\n" +
      "      Tuple(a=StringExpression {\n" +
      "        value=, \n" +
      "      }, b=<null>)\n" +
      "    ]\n" +
      "  }\n" +
      "}";

    assertMultilineStrings(resultText, expectedText);
  }

  /**
   * This function asserts that the two input strings equal line by line, this is
   * more helpful when failing in comparison to asserting two big chunks of newline
   * separated lines within a single string
   * @param result Result text
   * @param expected Expected text
   */
  private void assertMultilineStrings(String result, String expected) {
    String[] resultLines = result.split("\n");
    String[] expectedLines = expected.split("\n");

    assertEquals(resultLines.length, expectedLines.length);

    for (int i = 0; i < resultLines.length; i++)
      assertEquals(resultLines[i], expectedLines[i]);
  }
}
