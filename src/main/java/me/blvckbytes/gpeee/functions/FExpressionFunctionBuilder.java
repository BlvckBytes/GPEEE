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

package me.blvckbytes.gpeee.functions;

import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class FExpressionFunctionBuilder {

  private final List<ExpressionFunctionArgument> argumentDescriptions;

  public FExpressionFunctionBuilder() {
    this.argumentDescriptions = new ArrayList<>();
  }

  public FExpressionFunctionBuilder withArg(String name, boolean required, Class<?>... allowedTypes) {
    if (name == null || name.isBlank())
      throw new IllegalStateException("Arguments always have to have a name");

    if (
      // There are arguments present already
      argumentDescriptions.size() > 0 &&
      // And the last one is not required
      !argumentDescriptions.get(argumentDescriptions.size() - 1).isRequired() &&
      // And the one about to be added is also not required
      !required
    ) {
      // TODO: Think about how to allow multiple successive non-required arguments (maybe named arguments, like python has?)
      throw new IllegalStateException("Currently only supporting one trailing non-required argument");
    }

    argumentDescriptions.add(new ExpressionFunctionArgument(name, required, allowedTypes));
    return this;
  }

  public AExpressionFunction build(BiFunction<IEvaluationEnvironment, List<Object>, Object> handler) {
    return new AExpressionFunction() {

      @Override
      public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
        return handler.apply(environment, args);
      }

      @Override
      public List<ExpressionFunctionArgument> getArguments() {
        return argumentDescriptions;
      }
    };
  }
}
