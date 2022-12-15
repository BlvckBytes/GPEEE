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

package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.parser.MathOperation;
import org.jetbrains.annotations.Nullable;

public interface IValueInterpreter {

  boolean asBoolean(@Nullable Object value);

  long asLong(@Nullable Object value);

  double asDouble(@Nullable Object value);

  String asString(@Nullable Object value);

  boolean hasDecimalPoint(@Nullable Object value);

  int compare(@Nullable Object a, @Nullable Object b);

  boolean areEqual(@Nullable Object a, @Nullable Object b, boolean strict);

  Object performMath(@Nullable Object a, @Nullable Object b, MathOperation operation);

}
