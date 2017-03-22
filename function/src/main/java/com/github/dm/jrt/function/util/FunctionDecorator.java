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

import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class decorating a function instance.
 * <p>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
@SuppressWarnings("WeakerAccess")
public class FunctionDecorator<IN, OUT> extends DeepEqualObject
    implements Function<IN, OUT>, Decorator {

  private static final FunctionDecorator<Object, Object> sIdentity =
      new FunctionDecorator<Object, Object>(new Function<Object, Object>() {

        public Object apply(final Object in) {
          return in;
        }
      });

  private final List<Function<?, ?>> mFunctions;

  /**
   * Constructor.
   *
   * @param function the wrapped function.
   */
  private FunctionDecorator(@NotNull final Function<?, ?> function) {
    this(Collections.<Function<?, ?>>singletonList(
        ConstantConditions.notNull("function instance", function)));
  }

  /**
   * Constructor.
   *
   * @param functions the list of wrapped functions.
   */
  private FunctionDecorator(@NotNull final List<Function<?, ?>> functions) {
    super(asArgs(functions));
    mFunctions = functions;
  }

  /**
   * Returns a function decorator casting the passed inputs to the specified class.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param type  the class type.
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the function decorator.
   */
  @NotNull
  public static <IN, OUT> FunctionDecorator<IN, OUT> castTo(
      @NotNull final Class<? extends OUT> type) {
    return new FunctionDecorator<IN, OUT>(
        new ClassCastFunction<IN, OUT>(ConstantConditions.notNull("type", type)));
  }

  /**
   * Returns a function decorator casting the passed inputs to the specified class token type.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param token the class token.
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   * @return the function decorator.
   */
  @NotNull
  public static <IN, OUT> FunctionDecorator<IN, OUT> castTo(
      @NotNull final ClassToken<? extends OUT> token) {
    return castTo(token.getRawClass());
  }

  /**
   * Decorates the specified function instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param function the function instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the decorated function.
   */
  @NotNull
  public static <IN, OUT> FunctionDecorator<IN, OUT> decorate(
      @NotNull final Function<IN, OUT> function) {
    if (function instanceof FunctionDecorator) {
      return (FunctionDecorator<IN, OUT>) function;
    }

    return new FunctionDecorator<IN, OUT>(function);
  }

  /**
   * Returns the identity function decorator.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the function decorator.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> FunctionDecorator<IN, IN> identity() {
    return (FunctionDecorator<IN, IN>) sIdentity;
  }

  /**
   * Returns a composed function decorator that first applies this function to its input, and then
   * applies the after function to the result.
   *
   * @param after   the function to apply after this function is applied.
   * @param <AFTER> the type of output of the after function.
   * @return the composed function.
   */
  @NotNull
  public <AFTER> FunctionDecorator<IN, AFTER> andThen(
      @NotNull final Function<? super OUT, ? extends AFTER> after) {
    ConstantConditions.notNull("function instance", after);
    final List<Function<?, ?>> functions = mFunctions;
    final ArrayList<Function<?, ?>> newFunctions =
        new ArrayList<Function<?, ?>>(functions.size() + 1);
    newFunctions.addAll(functions);
    if (after instanceof FunctionDecorator) {
      newFunctions.addAll(((FunctionDecorator<?, ?>) after).mFunctions);

    } else {
      newFunctions.add(after);
    }

    return new FunctionDecorator<IN, AFTER>(newFunctions);
  }

  /**
   * Returns a composed function decorator that first applies the before function to its input,
   * and then applies this function to the result.
   *
   * @param before   the function to apply before this function is applied.
   * @param <BEFORE> the type of input to the before function.
   * @return the composed function.
   */
  @NotNull
  public <BEFORE> FunctionDecorator<BEFORE, OUT> compose(
      @NotNull final Function<? super BEFORE, ? extends IN> before) {
    ConstantConditions.notNull("function instance", before);
    final List<Function<?, ?>> functions = mFunctions;
    final ArrayList<Function<?, ?>> newFunctions =
        new ArrayList<Function<?, ?>>(functions.size() + 1);
    if (before instanceof FunctionDecorator) {
      newFunctions.addAll(((FunctionDecorator<?, ?>) before).mFunctions);

    } else {
      newFunctions.add(before);
    }

    newFunctions.addAll(functions);
    return new FunctionDecorator<BEFORE, OUT>(newFunctions);
  }

  public boolean hasStaticScope() {
    for (final Function<?, ?> function : mFunctions) {
      if (!Reflection.hasStaticScope(function)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Function implementation casting inputs to the specified class.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class ClassCastFunction<IN, OUT> extends DeepEqualObject
      implements Function<IN, OUT> {

    private final Class<? extends OUT> mType;

    /**
     * Constructor.
     *
     * @param type the output class type.
     */
    private ClassCastFunction(@NotNull final Class<? extends OUT> type) {
      super(asArgs(type));
      mType = type;
    }

    public OUT apply(final IN in) {
      return mType.cast(in);
    }
  }

  @SuppressWarnings("unchecked")
  public OUT apply(final IN in) throws Exception {
    Object result = in;
    for (final Function<?, ?> function : mFunctions) {
      result = ((Function<Object, Object>) function).apply(result);
    }

    return (OUT) result;
  }
}
