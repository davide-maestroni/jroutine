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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * This utility class represents the entry point to the library by acting as a factory of routine
 * builders.
 * <p>
 * The main way of creating a routine is to implement an invocation object. Invocations mimic the
 * scope of a function call. Objects are instantiated when needed and recycled for successive
 * invocations.
 * <p>
 * This class provides also a way to build channel instances, which can be used to pass data without
 * the need to start a routine invocation.
 * <p>
 * <b>Some usage examples</b>
 * <p>
 * <b>Example 1:</b> Configure and build a routine.
 * <pre><code>
 * final Routine&lt;Input, Result&gt; routine = JRoutineCore.routine()
 *                                                          .withInvocation()
 *                                                          .withLogLevel(Level.WARNING)
 *                                                          .configuration()
 *                                                          .of(myFactory);
 * </code></pre>
 * <p>
 * <b>Example 2:</b> Asynchronously merge the output of two routines.
 * <pre><code>
 * final Channel&lt;Result, Result&gt; channel = JRoutineCore.channel().&lt;Result&gt;ofType();
 * channel.pass(routine1.invoke().close())
 *        .pass(routine2.invoke().close())
 *        .close();
 *        .in(seconds(2))
 *        .allInto(results);
 * </code></pre>
 * <p>
 * Or simply:
 * <pre><code>
 * final Channel&lt;Void, Result&gt; output1 = routine1.invoke().close();
 * final Channel&lt;Void, Result&gt; output2 = routine2.invoke().close();
 * output1.in(seconds(2)).allInto(results);
 * output2.in(seconds(2)).allInto(results);
 * </code></pre>
 * (Note that, the order of the input or the output of the routine is not guaranteed unless properly
 * configured)
 * <p>
 * <b>Example 3:</b> Asynchronously concatenate the output of two routines.
 * <pre><code>
 * routine2.invoke().pass(routine1.invoke().close()).close().in(seconds(20)).all();
 * </code></pre>
 * <p>
 * Or, in an equivalent way:
 * <pre><code>
 * routine1.invoke().pipe(routine2.invoke()).close().in(seconds(20)).all();
 * </code></pre>
 * <p>
 * <b>Example 4:</b> Asynchronously feed a routine from a different thread.
 * <pre><code>
 * final Routine&lt;Result, Result&gt; routine =
 *     JRoutineCore.routine().of(IdentityInvocation.&lt;Result&gt;factory());
 * final Channel&lt;Result, Result&gt; channel = routine.invoke();
 *
 * new Thread() {
 *
 *   &#64;Override
 *   public void run() {
 *     channel.pass(new Result()).close();
 *   }
 * }.start();
 *
 * channel.in(seconds(2)).allInto(results);
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @see com.github.dm.jrt.core.routine.Routine Routine
 */
public class JRoutineCore {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineCore() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of channels running on the default executor.
   *
   * @return the channel builder instance.
   */
  @NotNull
  public static ChannelBuilder channel() {
    return channelOn(defaultExecutor());
  }

  /**
   * Returns a builder of channels running on the specified executor.
   *
   * @param executor the executor instance.
   * @return the channel builder instance.
   */
  @NotNull
  public static ChannelBuilder channelOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultChannelBuilder(executor);
  }

  /**
   * Returns a channel pushing inputs to the specified input one and collecting outputs from the
   * specified output one.
   * <p>
   * Note that it's up to the caller to ensure that inputs and outputs of two channels are actually
   * connected.
   *
   * @param inputChannel  the input channel.
   * @param outputChannel the output channel.
   * @param <IN>          the input data type.
   * @param <OUT>         the output data type.
   * @return the new channel instance.
   */
  @NotNull
  public static <IN, OUT> Channel<IN, OUT> flatten(@NotNull final Channel<IN, ?> inputChannel,
      @NotNull final Channel<?, OUT> outputChannel) {
    return new FlatChannel<IN, OUT>(inputChannel, outputChannel);
  }

  /**
   * Returns a channel making the wrapped one read-only.
   * <br>
   * The returned channel will fail on any attempt to pass input data, and will ignore any closing
   * command.
   * <br>
   * Note, however, that abort operations will be fulfilled.
   *
   * @param channel the wrapped channel.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the new channel instance.
   */
  @NotNull
  public static <IN, OUT> Channel<IN, OUT> readOnly(@NotNull final Channel<IN, OUT> channel) {
    return new ReadOnlyChannel<IN, OUT>(channel);
  }

  /**
   * Returns a builder of routines running on the default executor.
   *
   * @return the routine builder instance.
   */
  @NotNull
  public static RoutineBuilder routine() {
    return routineOn(defaultExecutor());
  }

  /**
   * Returns a builder of routines running on the specified executor.
   *
   * @param executor the executor instance.
   * @return the routine builder instance.
   */
  @NotNull
  public static RoutineBuilder routineOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultRoutineBuilder(executor);
  }
}