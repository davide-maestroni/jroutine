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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.lambda.Consumer;
import com.github.dm.jrt.function.lambda.Function;
import com.github.dm.jrt.function.lambda.Supplier;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamConfiguration;
import com.github.dm.jrt.stream.transform.LiftFunction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.function.lambda.FunctionDecorator.decorate;

/**
 * Utility class acting as a factory of stream routine builders.
 * <p>
 * A stream routine builder allows to easily build a concatenation of invocations as a single
 * routine.
 * <p>
 * For instance, a routine computing the root mean square of a number of integers can be defined as:
 * <pre><code>
 * final Routine&lt;Integer, Double&gt; rms = JRoutineStream.&lt;Integer&gt;withStream()
 *                                                          .map(i -&gt; i * i)
 *                                                          .map(average(Float.class))
 *                                                          .map(Math::sqrt)
 *                                                          .buildRoutine();
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineStream {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineStream() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a stream routine builder.
   *
   * @param <IN> the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStream() {
    return new DefaultStreamBuilder<IN, IN>();
  }

  /**
   * Returns a stream routine builder producing only the inputs passed by the specified consumer.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamAccept(
      @NotNull final Consumer<Channel<IN, ?>> consumer) {
    return withStreamAccept(1, consumer);
  }

  /**
   * Returns a stream routine builder producing only the inputs passed by the specified consumer.
   * <br>
   * The data will be produced by calling the consumer {@code count} number of times only when the
   * invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param count    the number of times the consumer is called.
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamAccept(final int count,
      @NotNull final Consumer<Channel<IN, ?>> consumer) {
    ConstantConditions.notNull("consumer instance", consumer);
    ConstantConditions.positive("count", count);
    return JRoutineStream.<IN>withStream().lift(new LiftFunction<IN, IN, IN, IN>() {

      public Function<Channel<?, IN>, Channel<?, IN>> apply(
          final StreamConfiguration streamConfiguration,
          final Function<Channel<?, IN>, Channel<?, IN>> function) {
        return decorate(function).andThen(new Function<Channel<?, IN>, Channel<?, IN>>() {

          public Channel<?, IN> apply(final Channel<?, IN> inputs) {
            final Channel<IN, IN> outputChannel = JRoutineCore.<IN>ofData().buildChannel();
            inputs.consume(new ChannelConsumer<IN>() {

              public void onComplete() {
                try {
                  for (int i = 0; i < count; ++i) {
                    consumer.accept(outputChannel);
                  }

                  outputChannel.close();

                } catch (final Throwable t) {
                  outputChannel.abort(t);
                  InterruptedInvocationException.throwIfInterrupt(t);
                }
              }

              public void onError(@NotNull final RoutineException error) {
                outputChannel.abort(error);
              }

              public void onOutput(final IN output) {
                throw new IllegalStateException();
              }
            });
            return outputChannel;
          }
        });
      }
    });
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified supplier.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamGet(@NotNull final Supplier<IN> supplier) {
    return withStreamGet(1, supplier);
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified supplier.
   * <br>
   * The data will be produced by calling the supplier {@code count} number of times only when the
   * invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param count    the number of times the supplier is called.
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamGet(final int count,
      @NotNull final Supplier<IN> supplier) {
    ConstantConditions.notNull("supplier instance", supplier);
    ConstantConditions.positive("count", count);
    return JRoutineStream.<IN>withStream().lift(new LiftFunction<IN, IN, IN, IN>() {

      public Function<Channel<?, IN>, Channel<?, IN>> apply(
          final StreamConfiguration streamConfiguration,
          final Function<Channel<?, IN>, Channel<?, IN>> function) {
        return decorate(function).andThen(new Function<Channel<?, IN>, Channel<?, IN>>() {

          public Channel<?, IN> apply(final Channel<?, IN> inputs) {
            final Channel<IN, IN> outputChannel = JRoutineCore.<IN>ofData().buildChannel();
            inputs.consume(new ChannelConsumer<IN>() {

              public void onComplete() {
                try {
                  for (int i = 0; i < count; ++i) {
                    outputChannel.pass(supplier.get());
                  }

                  outputChannel.close();

                } catch (final Throwable t) {
                  outputChannel.abort(t);
                  InterruptedInvocationException.throwIfInterrupt(t);
                }
              }

              public void onError(@NotNull final RoutineException error) {
                outputChannel.abort(error);
              }

              public void onOutput(final IN output) {
                throw new IllegalStateException();
              }
            });
            return outputChannel;
          }
        });
      }
    });
  }

  /**
   * Returns a stream routine builder producing only the specified input.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param input the input.
   * @param <IN>  the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(@Nullable final IN input) {
    return withStreamOf(
        Channels.replayOutput(JRoutineCore.of(input).buildChannel()).buildChannel());
  }

  /**
   * Returns a stream routine builder producing only the specified inputs.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param inputs the input data.
   * @param <IN>   the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(@Nullable final IN... inputs) {
    return withStreamOf(
        Channels.replayOutput(JRoutineCore.of(inputs).buildChannel()).buildChannel());
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified iterable.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   *
   * @param inputs the inputs iterable.
   * @param <IN>   the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(
      @Nullable final Iterable<? extends IN> inputs) {
    return withStreamOf(
        Channels.replayOutput(JRoutineCore.of(inputs).buildChannel()).buildChannel());
  }

  /**
   * Returns a stream routine builder producing only the inputs returned by the specified channel.
   * <br>
   * The data will be produced only when the invocation completes.
   * <br>
   * If any other input is passed to the built routine, the invocation will be aborted with an
   * {@link java.lang.IllegalStateException}.
   * <p>
   * Note that the passed channel will be bound as a result of the call, so, in order to support
   * multiple invocations, consider wrapping the channel in a replayable one, by calling the
   * {@link Channels#replayOutput(Channel)} utility method.
   *
   * @param channel the input channel.
   * @param <IN>    the input data type.
   * @return the routine builder instance.
   */
  @NotNull
  public static <IN> StreamBuilder<IN, IN> withStreamOf(
      @Nullable final Channel<?, ? extends IN> channel) {
    return JRoutineStream.<IN>withStream().lift(new LiftFunction<IN, IN, IN, IN>() {

      public Function<Channel<?, IN>, Channel<?, IN>> apply(
          final StreamConfiguration streamConfiguration,
          final Function<Channel<?, IN>, Channel<?, IN>> function) {
        return decorate(function).andThen(new Function<Channel<?, IN>, Channel<?, IN>>() {

          public Channel<?, IN> apply(final Channel<?, IN> inputs) {
            final Channel<IN, IN> outputChannel = JRoutineCore.<IN>ofData().buildChannel();
            inputs.consume(new ChannelConsumer<IN>() {

              public void onComplete() {
                try {
                  outputChannel.pass(channel).close();

                } catch (final Throwable t) {
                  outputChannel.abort(t);
                  InterruptedInvocationException.throwIfInterrupt(t);
                }
              }

              public void onError(@NotNull final RoutineException error) {
                outputChannel.abort(error);
              }

              public void onOutput(final IN output) {
                throw new IllegalStateException();
              }
            });
            return outputChannel;
          }
        });
      }
    });
  }
}
