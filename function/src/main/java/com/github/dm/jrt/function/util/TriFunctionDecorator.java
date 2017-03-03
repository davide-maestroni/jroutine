/*
 * Copyright 2017 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.function.util;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class decorating a tri-function instance.
 * <p>
 * Created by davide-maestroni on 02/23/2017.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 * @param <IN3> the third input data type.
 * @param <OUT> the output data type.
 */
@SuppressWarnings("WeakerAccess")
public class TriFunctionDecorator<IN1, IN2, IN3, OUT> extends DeepEqualObject
    implements TriFunction<IN1, IN2, IN3, OUT>, Decorator {

  private final FunctionDecorator<?, ? extends OUT> mFunction;

  private final TriFunction<IN1, IN2, IN3, ?> mTriFunction;

  /**
   * Constructor.
   *
   * @param triFunction the wrapped tri-function.
   */
  private TriFunctionDecorator(@NotNull final TriFunction<IN1, IN2, IN3, ?> triFunction) {
    this(ConstantConditions.notNull("bi-function instance", triFunction),
        FunctionDecorator.<OUT>identity());
  }

  /**
   * Constructor.
   *
   * @param biFunction the initial wrapped tri-function.
   * @param function   the concatenated function chain.
   */
  private TriFunctionDecorator(@NotNull final TriFunction<IN1, IN2, IN3, ?> biFunction,
      @NotNull final FunctionDecorator<?, ? extends OUT> function) {
    super(asArgs(biFunction, function));
    mTriFunction = biFunction;
    mFunction = function;
  }

  /**
   * Decorates the specified tri-function instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param function the tri-function instance.
   * @param <IN1>    the first input data type.
   * @param <IN2>    the second input data type.
   * @param <IN3>    the third input data type.
   * @param <OUT>    the output data type.
   * @return the decorated tri-function.
   */
  @NotNull
  public static <IN1, IN2, IN3, OUT> TriFunctionDecorator<IN1, IN2, IN3, OUT> decorate(
      @NotNull final TriFunction<IN1, IN2, IN3, OUT> function) {
    if (function instanceof TriFunctionDecorator) {
      return (TriFunctionDecorator<IN1, IN2, IN3, OUT>) function;
    }

    return new TriFunctionDecorator<IN1, IN2, IN3, OUT>(function);
  }

  /**
   * Returns a composed tri-function decorator that first applies this function to its input, and
   * then applies the after function to the result.
   *
   * @param after   the function to apply after this function is applied.
   * @param <AFTER> the type of output of the after function.
   * @return the composed tri-function.
   */
  @NotNull
  public <AFTER> TriFunctionDecorator<IN1, IN2, IN3, AFTER> andThen(
      @NotNull final Function<? super OUT, ? extends AFTER> after) {
    return new TriFunctionDecorator<IN1, IN2, IN3, AFTER>(mTriFunction, mFunction.andThen(after));
  }

  @SuppressWarnings("unchecked")
  public OUT apply(final IN1 in1, final IN2 in2, final IN3 in3) throws Exception {
    return ((FunctionDecorator<Object, OUT>) mFunction).apply(mTriFunction.apply(in1, in2, in3));
  }

  public boolean hasStaticScope() {
    return Reflection.hasStaticScope(mTriFunction) && mFunction.hasStaticScope();
  }
}
