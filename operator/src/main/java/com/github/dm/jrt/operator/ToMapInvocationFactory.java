/*
 * Copyright 2016 Davide Maestroni
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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.lambda.FunctionDecorator;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invocations collecting inputs into maps.
 * <p>
 * Created by davide-maestroni on 05/05/2016.
 *
 * @param <IN>    the input data type.
 * @param <KEY>   the map key type.
 * @param <VALUE> the map value type.
 */
class ToMapInvocationFactory<IN, KEY, VALUE> extends InvocationFactory<IN, Map<KEY, VALUE>> {

  private final FunctionDecorator<? super IN, KEY> mKeyFunction;

  private final FunctionDecorator<? super IN, VALUE> mValueFunction;

  /**
   * Constructor.
   *
   * @param keyFunction   the function extracting the key.
   * @param valueFunction the function extracting the value.
   */
  ToMapInvocationFactory(@NotNull final FunctionDecorator<? super IN, KEY> keyFunction,
      @NotNull final FunctionDecorator<? super IN, VALUE> valueFunction) {
    super(asArgs(ConstantConditions.notNull("key function instance", keyFunction),
        ConstantConditions.notNull("value function instance", valueFunction)));
    mKeyFunction = keyFunction;
    mValueFunction = valueFunction;
  }

  @NotNull
  @Override
  public Invocation<IN, Map<KEY, VALUE>> newInvocation() {
    return new ToMapInvocation<IN, KEY, VALUE>(mKeyFunction, mValueFunction);
  }

  /**
   * Invocation collecting inputs into a map.
   *
   * @param <IN>    the input data type.
   * @param <KEY>   the map key type.
   * @param <VALUE> the map value type.
   */
  private static class ToMapInvocation<IN, KEY, VALUE>
      extends TemplateInvocation<IN, Map<KEY, VALUE>> {

    private final FunctionDecorator<? super IN, KEY> mKeyFunction;

    private final FunctionDecorator<? super IN, VALUE> mValueFunction;

    private HashMap<KEY, VALUE> mMap;

    /**
     * Constructor.
     *
     * @param keyFunction   the function extracting the key.
     * @param valueFunction the function extracting the value.
     */
    private ToMapInvocation(@NotNull final FunctionDecorator<? super IN, KEY> keyFunction,
        @NotNull final FunctionDecorator<? super IN, VALUE> valueFunction) {
      mKeyFunction = keyFunction;
      mValueFunction = valueFunction;
    }

    @Override
    public void onComplete(@NotNull final Channel<Map<KEY, VALUE>, ?> result) {
      result.pass(mMap);
    }

    @Override
    public void onInput(final IN input, @NotNull final Channel<Map<KEY, VALUE>, ?> result) throws
        Exception {
      mMap.put(mKeyFunction.apply(input), mValueFunction.apply(input));
    }

    @Override
    public boolean onRecycle() {
      mMap = null;
      return true;
    }

    @Override
    public void onRestart() {
      mMap = new HashMap<KEY, VALUE>();
    }
  }
}
