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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class decorating a consumer instance.
 * <p>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN> the input data type.
 */
@SuppressWarnings("WeakerAccess")
public class ConsumerDecorator<IN> extends DeepEqualObject implements Consumer<IN>, Decorator {

  private static final ConsumerDecorator<Object> sSink =
      new ConsumerDecorator<Object>(new Consumer<Object>() {

        public void accept(final Object in) {}
      });

  private final List<Consumer<?>> mConsumers;

  /**
   * Constructor.
   *
   * @param consumer the wrapped consumer.
   */
  private ConsumerDecorator(@NotNull final Consumer<?> consumer) {
    this(Collections.<Consumer<?>>singletonList(
        ConstantConditions.notNull("consumer instance", consumer)));
  }

  /**
   * Constructor.
   *
   * @param consumers the list of wrapped consumers.
   */
  private ConsumerDecorator(@NotNull final List<Consumer<?>> consumers) {
    super(asArgs(consumers));
    mConsumers = consumers;
  }

  /**
   * Decorates the specified consumer instance so to provide additional features.
   * <br>
   * The returned object will support concatenation and comparison.
   * <p>
   * Note that the passed object is expected to have a functional behavior, that is, it must not
   * retain a mutable internal state.
   * <br>
   * Note also that any external object used inside the function must be synchronized in order to
   * avoid concurrency issues.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @return the decorated consumer.
   */
  @NotNull
  public static <IN> ConsumerDecorator<IN> decorate(@NotNull final Consumer<IN> consumer) {
    if (consumer instanceof ConsumerDecorator) {
      return (ConsumerDecorator<IN>) consumer;
    }

    return new ConsumerDecorator<IN>(consumer);
  }

  /**
   * Returns a consumer decorator just discarding the passed inputs.
   * <br>
   * The returned object will support concatenation and comparison.
   *
   * @param <IN> the input data type.
   * @return the consumer decorator.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> ConsumerDecorator<IN> sink() {
    return (ConsumerDecorator<IN>) sSink;
  }

  /**
   * Returns a composed consumer decorator that performs, in sequence, this operation followed by
   * the after operation.
   *
   * @param after the operation to perform after this operation.
   * @return the composed consumer.
   */
  @NotNull
  public ConsumerDecorator<IN> andThen(@NotNull final Consumer<? super IN> after) {
    ConstantConditions.notNull("consumer instance", after);
    final List<Consumer<?>> consumers = mConsumers;
    final ArrayList<Consumer<?>> newConsumers = new ArrayList<Consumer<?>>(consumers.size() + 1);
    newConsumers.addAll(consumers);
    if (after instanceof ConsumerDecorator) {
      newConsumers.addAll(((ConsumerDecorator<?>) after).mConsumers);

    } else {
      newConsumers.add(after);
    }

    return new ConsumerDecorator<IN>(newConsumers);
  }

  public boolean hasStaticScope() {
    for (final Consumer<?> consumer : mConsumers) {
      if (!Reflection.hasStaticScope(consumer)) {
        return false;
      }
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  public void accept(final IN in) throws Exception {
    for (final Consumer<?> consumer : mConsumers) {
      ((Consumer<Object>) consumer).accept(in);
    }
  }
}
