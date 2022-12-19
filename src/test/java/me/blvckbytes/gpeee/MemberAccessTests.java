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

import lombok.AllArgsConstructor;
import me.blvckbytes.gpeee.error.UnknownMemberError;
import me.blvckbytes.gpeee.functions.FExpressionFunctionBuilder;
import org.junit.Before;
import org.junit.Test;

public class MemberAccessTests {

  @AllArgsConstructor
  private static class TestObject {
    private String text;
    private long number;
    private TestObject self;
  }

  private TestObject testObject;

  @Before
  public void initializeTestObject() {
    this.testObject = new TestObject("Hello, world", 21, new TestObject("Yet another", 22, null));
  }

  @Test
  public void shouldAccessMembersByIdentifiers() {
    new EnvironmentBuilder()
      .withStaticVariable("my_object", testObject)
      .launch(validator -> {
        validator.validate("my_object.text", testObject.text);
        validator.validate("my_object.number", testObject.number);
        validator.validateThrows("my_object.invalid", UnknownMemberError.class);

        validator.validate("my_object.self.text", testObject.self.text);
        validator.validate("my_object.self.number", testObject.self.number);
        validator.validate("my_object.self.self", testObject.self.self);
        validator.validateThrows("my_object.self.invalid", UnknownMemberError.class);

        validator.validateThrows("my_object.self.invalid", UnknownMemberError.class);
      });
  }

  @Test
  public void shouldAccessMembersByExpressions() {
    new EnvironmentBuilder()
      .withStaticVariable("my_object", testObject)
      .withStaticVariable("first_field", "text")
      .withStaticVariable("second_field", "number")
      .withStaticVariable("self_field", "self")
      .withStaticVariable("invalid_field", "invalid")
      .launch(validator -> {
        validator.validate("my_object.\"text\"", testObject.text);
        validator.validate("my_object.\"number\"", testObject.number);
        validator.validateThrows("my_object.\"invalid\"", UnknownMemberError.class);

        validator.validate("my_object.str(first_field)", testObject.text);
        validator.validate("my_object.str(second_field)", testObject.number);
        validator.validate("my_object.str(self_field)", testObject.self);
        validator.validateThrows("my_object.str(invalid_field)", UnknownMemberError.class);

        validator.validate("my_object.self.\"text\"", testObject.self.text);
        validator.validate("my_object.self.\"number\"", testObject.self.number);
        validator.validate("my_object.self.\"self\"", testObject.self.self);
        validator.validateThrows("my_object.self.\"invalid\"", UnknownMemberError.class);
        validator.validate("my_object.\"self\".\"text\"", testObject.self.text);
        validator.validate("my_object.\"self\".\"number\"", testObject.self.number);
        validator.validate("my_object.\"self\".\"self\"", testObject.self.self);
        validator.validateThrows("my_object.\"self\".\"invalid\"", UnknownMemberError.class);

        validator.validate("my_object.self.str(first_field)", testObject.self.text);
        validator.validate("my_object.self.str(second_field)", testObject.self.number);
        validator.validate("my_object.self.str(self_field)", testObject.self.self);
        validator.validateThrows("my_object.self.str(invalid_field)", UnknownMemberError.class);
        validator.validate("my_object.str(self_field).str(first_field)", testObject.self.text);
        validator.validate("my_object.str(self_field).str(second_field)", testObject.self.number);
        validator.validate("my_object.str(self_field).str(self_field)", testObject.self.self);
        validator.validateThrows("my_object.str(self_field).str(invalid_field)", UnknownMemberError.class);

        // Null should have no accessible fields at all
        validator.validateThrows("null.my_field", UnknownMemberError.class);
      });
  }

  @Test
  public void shouldAccessMembersFromFunctionReturns() {
    new EnvironmentBuilder()
      .withFunction("get_my_object", new FExpressionFunctionBuilder().build((e, args) -> testObject))
      .withStaticVariable("first_field", "text")
      .withStaticVariable("second_field", "number")
      .withStaticVariable("self_field", "self")
      .withStaticVariable("invalid_field", "invalid")
      .launch(validator -> {
        validator.validate("get_my_object().text", testObject.text);
        validator.validate("get_my_object().number", testObject.number);
        validator.validateThrows("get_my_object().invalid", UnknownMemberError.class);

        validator.validate("get_my_object().self.text", testObject.self.text);
        validator.validate("get_my_object().self.number", testObject.self.number);
        validator.validate("get_my_object().self.self", testObject.self.self);
        validator.validateThrows("get_my_object().self.invalid", UnknownMemberError.class);

        validator.validateThrows("get_my_object().self.invalid", UnknownMemberError.class);

        validator.validate("get_my_object().\"text\"", testObject.text);
        validator.validate("get_my_object().\"number\"", testObject.number);
        validator.validateThrows("get_my_object().\"invalid\"", UnknownMemberError.class);

        validator.validate("get_my_object().str(first_field)", testObject.text);
        validator.validate("get_my_object().str(second_field)", testObject.number);
        validator.validate("get_my_object().str(self_field)", testObject.self);
        validator.validateThrows("get_my_object().str(invalid_field)", UnknownMemberError.class);

        validator.validate("get_my_object().self.\"text\"", testObject.self.text);
        validator.validate("get_my_object().self.\"number\"", testObject.self.number);
        validator.validate("get_my_object().self.\"self\"", testObject.self.self);
        validator.validateThrows("get_my_object().self.\"invalid\"", UnknownMemberError.class);

        validator.validate("get_my_object().self.str(first_field)", testObject.self.text);
        validator.validate("get_my_object().self.str(second_field)", testObject.self.number);
        validator.validate("get_my_object().self.str(self_field)", testObject.self.self);
        validator.validateThrows("get_my_object().self.str(invalid_field)", UnknownMemberError.class);
      });
  }
}
