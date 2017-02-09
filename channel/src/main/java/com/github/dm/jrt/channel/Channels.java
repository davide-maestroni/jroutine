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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.channel.builder.ChannelListBuilder;
import com.github.dm.jrt.channel.builder.ChannelMapBuilder;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Utility class for handling routine channels.
 * <br>
 * The class provides several methods to split and merge channels together, making also possible to
 * transfer data in multiple streams, each one identified by a specific ID.
 * <p>
 * Created by davide-maestroni on 03/15/2015.
 */
@SuppressWarnings("WeakerAccess")
public class Channels {

  /**
   * Avoid explicit instantiation.
   */
  protected Channels() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of channels blending the outputs coming from the specified ones.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [B, A, A, C, B, C, B, A, B, ...]
   * </code></pre>
   *
   * @param channels the array of channels.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <OUT> ChannelBuilder<?, OUT> blendOutput(@NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    return (BlendOutputBuilder<OUT>) new BlendOutputBuilder<Object>(Arrays.asList(channels));
  }

  /**
   * Returns a builder of channels blending the outputs coming from the specified ones.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [B, A, A, C, B, C, B, A, B, ...]
   * </code></pre>
   *
   * @param channels the iterable of channels.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> blendOutput(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return new BlendOutputBuilder<OUT>(channels);
  }

  /**
   * Returns a builder of channels concatenating the outputs coming from the specified ones, so
   * that, all the outputs of the first channel will come before all the outputs of the second one,
   * and so on.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [A, A, A, ..., B, B, B, ..., C, C, C, ...]
   * </code></pre>
   *
   * @param channels the array of channels.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <OUT> ChannelBuilder<?, OUT> concatOutput(
      @NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    return (ConcatOutputBuilder<OUT>) new ConcatOutputBuilder<Object>(Arrays.asList(channels));
  }

  /**
   * Returns a builder of channels concatenating the outputs coming from the specified ones, so
   * that, all the outputs of the first channel will come before all the outputs of the second one,
   * and so on.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [A, A, A, ..., B, B, B, ..., C, C, C, ...]
   * </code></pre>
   *
   * @param channels the iterable of channels.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> concatOutput(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return new ConcatOutputBuilder<OUT>(channels);
  }

  /**
   * Returns a builder of channels transforming the input data into flow ones.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channel {@code A}, its final output will be:
   * <pre><code>
   * A =&gt; [Flow(id, IN), Flow(id, IN), Flow(id, IN), ...]
   * </code></pre>
   *
   * @param channel the flow channel.
   * @param id      the flow ID.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the channel builder.
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelBuilder<IN, ?> flowInput(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, final int id) {
    return new FlowInputBuilder<DATA, IN>(channel, id);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   * <p>
   * Given channel {@code A} and channels {@code IN1}, {@code IN2} and {@code IN3} in the returned
   * map, the final output of {@code A} will be:
   * <pre><code>
   * A =&gt; [Flow(ids[1], IN2), Flow(ids[0], IN1), Flow(ids[2], IN3), ...]
   * </code></pre>
   *
   * @param channel the flow channel.
   * @param ids     the array of IDs.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelMapBuilder<Integer, IN, ?> flowInput(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return new FlowInputMapBuilder<DATA, IN>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   * <p>
   * Given channel {@code A} and channels {@code IN1}, {@code IN2} and {@code IN3} in the returned
   * map, the final output of {@code A} will be:
   * <pre><code>
   * A =&gt; [Flow(ids[1], IN2), Flow(ids[0], IN1), Flow(ids[2], IN3), ...]
   * </code></pre>
   *
   * @param channel the flow channel.
   * @param ids     the iterable returning the flow IDs.
   * @param <DATA>  the channel data type.
   * @param <IN>    the input data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelMapBuilder<Integer, IN, ?> flowInput(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      idSet.add(id);
    }

    return new FlowInputMapBuilder<DATA, IN>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels accepting the data identified by the specified IDs.
   * <p>
   * Note that the builder will successfully create several channel map instances.
   * <p>
   * Given channel {@code A} and channels {@code IN1}, {@code IN2} and {@code IN3} in the returned
   * map, the final output of {@code A} will be:
   * <pre><code>
   * A =&gt; [Flow(startId + 1, IN2), Flow(startId + 0, IN1), Flow(startId + 2, IN3), ...]
   * </code></pre>
   *
   * @param startId   the flow start ID.
   * @param rangeSize the size of the range of IDs (must be positive).
   * @param channel   the flow channel.
   * @param <DATA>    the channel data type.
   * @param <IN>      the input data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   */
  @NotNull
  public static <DATA, IN extends DATA> ChannelMapBuilder<Integer, IN, ?> flowInput(
      final int startId, final int rangeSize,
      @NotNull final Channel<? super Flow<DATA>, ?> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return new FlowInputMapBuilder<DATA, IN>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C} in the returned map, the final output will
   * be:
   * <pre><code>
   * A =&gt; [Flow(ids[0], IN).data, Flow(ids[0], IN).data, ...]
   * B =&gt; [Flow(ids[1], IN).data, Flow(ids[1], IN).data, ...]
   * C =&gt; [Flow(ids[2], IN).data, Flow(ids[2], IN).data, ...]
   * </code></pre>
   *
   * @param channel the flow channel.
   * @param ids     the list of IDs.
   * @param <OUT>   the output data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   */
  @NotNull
  public static <OUT> ChannelMapBuilder<Integer, ?, OUT> flowOutput(
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel, @NotNull final int... ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final int id : ids) {
      idSet.add(id);
    }

    return new OutputMapBuilder<OUT>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C} in the returned map, the final output will
   * be:
   * <pre><code>
   * A =&gt; [Flow(ids[0], IN).data, Flow(ids[0], IN).data, ...]
   * B =&gt; [Flow(ids[1], IN).data, Flow(ids[1], IN).data, ...]
   * C =&gt; [Flow(ids[2], IN).data, Flow(ids[2], IN).data, ...]
   * </code></pre>
   *
   * @param channel the flow channel.
   * @param ids     the iterable returning the flow IDs.
   * @param <OUT>   the output data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   */
  @NotNull
  public static <OUT> ChannelMapBuilder<Integer, ?, OUT> flowOutput(
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel,
      @NotNull final Iterable<Integer> ids) {
    final HashSet<Integer> idSet = new HashSet<Integer>();
    for (final Integer id : ids) {
      idSet.add(id);
    }

    return new OutputMapBuilder<OUT>(channel, idSet);
  }

  /**
   * Returns a builder of maps of channels returning the output data filtered by the specified IDs.
   * <p>
   * Note that the builder will return the same map for the same inputs and equal configuration,
   * and that the passed channels will be bound as a result of the creation.
   * <br>
   * Note also that the returned channels will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C} in the returned map, the final output will
   * be:
   * <pre><code>
   * A =&gt; [Flow(startId + 0, IN).data, Flow(startId + 0, IN).data, ...]
   * B =&gt; [Flow(startId + 1, IN).data, Flow(startId + 1, IN).data, ...]
   * C =&gt; [Flow(startId + 2, IN).data, Flow(startId + 2, IN).data, ...]
   * </code></pre>
   *
   * @param startId   the flow start ID.
   * @param rangeSize the size of the range of IDs (must be positive).
   * @param channel   the flow channel.
   * @param <OUT>     the output data type.
   * @return the map of IDs and channels builder.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   */
  @NotNull
  public static <OUT> ChannelMapBuilder<Integer, ?, OUT> flowOutput(final int startId,
      final int rangeSize, @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel) {
    ConstantConditions.positive("range size", rangeSize);
    final HashSet<Integer> idSet = new HashSet<Integer>();
    final int endId = startId + rangeSize;
    for (int i = startId; i < endId; ++i) {
      idSet.add(i);
    }

    return new OutputMapBuilder<OUT>(channel, idSet);
  }

  /**
   * Returns a builder of channels producing the result of the specified Callable.
   *
   * @param callable the Callable instance.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> fromCallable(@NotNull final Callable<OUT> callable) {
    return new CallableChannelBuilder<OUT>(callable);
  }

  /**
   * Returns a builder of channels producing the result of the specified Future.
   * <br>
   * If the channel is aborted the Future will be cancelled with {@code mayInterruptIfRunning} set
   * to false.
   * <p>
   * Note that the configured runner will be employed to wait for the Future to complete.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param future the Future instance.
   * @param <OUT>  the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> fromFuture(@NotNull final Future<OUT> future) {
    return new FutureChannelBuilder<OUT>(future, false);
  }

  /**
   * Returns a builder of channels producing the result of the specified Future.
   * <br>
   * If the channel is aborted the Future will be cancelled with {@code mayInterruptIfRunning} set
   * to the specified value.
   * <p>
   * Note that the configured runner will be employed to wait for the Future to complete.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param future                the Future instance.
   * @param mayInterruptIfRunning true if the thread executing the Future task should be
   *                              interrupted; otherwise, in-progress tasks are allowed to complete.
   * @param <OUT>                 the output data type.
   * @return the channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> fromFuture(@NotNull final Future<OUT> future,
      final boolean mayInterruptIfRunning) {
    return new FutureChannelBuilder<OUT>(future, mayInterruptIfRunning);
  }

  /**
   * Returns a builder of flow channels feeding the specified one.
   * <br>
   * Each output will be filtered based on the specified ID.
   * <p>
   * Note that the builder will return the same instance for the same input and equal configuration.
   * <p>
   * Given channel {@code A}, its final output will be:
   * <pre><code>
   * A =&gt; [Flow(id, IN).data, Flow(id, IN).data, ...]
   * </code></pre>
   *
   * @param channel the channel to feed.
   * @param id      the flow ID.
   * @param <IN>    the input data type.
   * @return the flow channel builder.
   */
  @NotNull
  public static <IN> ChannelBuilder<Flow<IN>, ?> inputFlow(
      @NotNull final Channel<? super IN, ?> channel, final int id) {
    return new InputFilterBuilder<IN>(channel, id);
  }

  /**
   * Returns a builder of channels distributing the input data among the specified ones. If the
   * list of data exceeds the number of channels, the invocation will be aborted.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
   * B =&gt; [list(1), list(1), list(1), ..., list(1), ..., list(1)]
   * C =&gt; [list(2), list(2), list(2), ..., list(2)]
   * </code></pre>
   *
   * @param channels the array of channels.
   * @param <IN>     the input data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <IN> ChannelBuilder<List<? extends IN>, ?> joinInput(
      @NotNull final Channel<?, ?>... channels) {
    return joinInput(false, null, channels);
  }

  /**
   * Returns a builder of channels distributing the input data among the specified ones. If the
   * list of data is smaller than the specified number of channels, the remaining ones will be fed
   * with the specified placeholder instance. While, if the list of data exceeds the number of
   * channels, the invocation will be aborted.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
   * B =&gt; [list(1), list(1), list(1), ..., list(1), ..., list(1), placeholder, ...]
   * C =&gt; [list(2), list(2), list(2), ..., list(2), placeholder, placeholder, ...]
   * </code></pre>
   *
   * @param placeholder the placeholder instance.
   * @param channels    the array of channels.
   * @param <IN>        the input data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <IN> ChannelBuilder<List<? extends IN>, ?> joinInput(@Nullable final IN placeholder,
      @NotNull final Channel<?, ?>... channels) {
    return joinInput(true, placeholder, channels);
  }

  /**
   * Returns a builder of channels distributing the input data among the specified ones. If the
   * list of data is smaller than the specified number of channels, the remaining ones will be fed
   * with the specified placeholder instance. While, if the list of data exceeds the number of
   * channels, the invocation will be aborted.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
   * B =&gt; [list(1), list(1), list(1), ..., list(1), ..., list(1), placeholder, ...]
   * C =&gt; [list(2), list(2), list(2), ..., list(2), placeholder, placeholder, ...]
   * </code></pre>
   *
   * @param placeholder the placeholder instance.
   * @param channels    the iterable of channels.
   * @param <IN>        the input data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <IN> ChannelBuilder<List<? extends IN>, ?> joinInput(@Nullable final IN placeholder,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return joinInput(true, placeholder, channels);
  }

  /**
   * Returns a builder of channels distributing the input data among the specified ones. If the
   * list of data exceeds the number of channels, the invocation will be aborted.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [list(0), list(0), list(0), ..., list(0), ..., list(0), ...]
   * B =&gt; [list(1), list(1), list(1), ..., list(1), ..., list(1)]
   * C =&gt; [list(2), list(2), list(2), ..., list(2)]
   * </code></pre>
   *
   * @param channels the iterable of channels.
   * @param <IN>     the input data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <IN> ChannelBuilder<List<? extends IN>, ?> joinInput(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return joinInput(false, null, channels);
  }

  /**
   * Returns a builder of channels joining the data coming from the specified ones.
   * <br>
   * An output will be generated only when at least one result is available for each channel.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [(A, B, C), (A, B, C), (A, B, C), ...]
   * </code></pre>
   *
   * @param channels the array of channels.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, List<OUT>> joinOutput(
      @NotNull final Channel<?, ?>... channels) {
    return joinOutput(false, null, channels);
  }

  /**
   * Returns a builder of channels joining the data coming from the specified ones.
   * <br>
   * An output will be generated only when at least one result is available for each channel.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [(A, B, C), (A, B, C), (A, B, C), ...]
   * </code></pre>
   *
   * @param channels the iterable of channels.
   * @param <OUT>    the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, List<OUT>> joinOutput(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return joinOutput(false, null, channels);
  }

  /**
   * Returns a builder of channels joining the data coming from the specified ones.
   * <br>
   * An output will be generated only when at least one result is available for each channel.
   * Moreover, when all the channels are closed, the remaining outputs will be returned by filling
   * the gaps with the specified placeholder instance, so that the generated list of data will
   * always have the same size as the channel list.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [(A, B, C), ..., (placeholder, B, C), ..., (placeholder, B, placeholder), ...]
   * </code></pre>
   *
   * @param placeholder the placeholder instance.
   * @param channels    the array of channels.
   * @param <OUT>       the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, List<OUT>> joinOutput(@Nullable final OUT placeholder,
      @NotNull final Channel<?, ?>... channels) {
    return joinOutput(true, placeholder, channels);
  }

  /**
   * Returns a builder of channels joining the data coming from the specified ones.
   * <br>
   * An output will be generated only when at least one result is available for each channel.
   * Moreover, when all the channels are closed, the remaining outputs will be returned by filling
   * the gaps with the specified placeholder instance, so that the generated list of data will
   * always have the same size as the channel list.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [(A, B, C), ..., (placeholder, B, C), ..., (placeholder, B, placeholder), ...]
   * </code></pre>
   *
   * @param placeholder the placeholder instance.
   * @param channels    the iterable of channels.
   * @param <OUT>       the output data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, List<OUT>> joinOutput(@Nullable final OUT placeholder,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return joinOutput(true, placeholder, channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the position in the array.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [Flow(0, IN).data, Flow(0, IN).data, Flow(0, IN).data, ...]
   * B =&gt; [Flow(1, IN).data, Flow(1, IN).data, Flow(1, IN).data, ...]
   * C =&gt; [Flow(2, IN).data, Flow(2, IN).data, Flow(2, IN).data, ...]
   * </code></pre>
   *
   * @param channels the array of channels.
   * @param <IN>     the input data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <IN> ChannelBuilder<Flow<? extends IN>, ?> mergeInput(
      @NotNull final Channel<?, ?>... channels) {
    return mergeInput(0, channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [Flow(startId + 0, IN).data, Flow(startId + 0, IN).data, ...]
   * B =&gt; [Flow(startId + 1, IN).data, Flow(startId + 1, IN).data, ...]
   * C =&gt; [Flow(startId + 2, IN).data, Flow(startId + 2, IN).data, ...]
   * </code></pre>
   *
   * @param startId  the flow start ID.
   * @param channels the array of channels.
   * @param <IN>     the input data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> ChannelBuilder<Flow<? extends IN>, ?> mergeInput(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    return (MergeInputBuilder<IN>) new MergeInputBuilder<Object>(startId, Arrays.asList(channels));
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [Flow(startId + 0, IN).data, Flow(startId + 0, IN).data, ...]
   * B =&gt; [Flow(startId + 1, IN).data, Flow(startId + 1, IN).data, ...]
   * C =&gt; [Flow(startId + 2, IN).data, Flow(startId + 2, IN).data, ...]
   * </code></pre>
   *
   * @param startId  the flow start ID.
   * @param channels the iterable of channels.
   * @param <IN>     the input data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <IN> ChannelBuilder<Flow<? extends IN>, ?> mergeInput(final int startId,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return new MergeInputBuilder<IN>(startId, channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the position in the iterable.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [Flow(0, IN).data, Flow(0, IN).data, Flow(0, IN).data, ...]
   * B =&gt; [Flow(1, IN).data, Flow(1, IN).data, Flow(1, IN).data, ...]
   * C =&gt; [Flow(2, IN).data, Flow(2, IN).data, Flow(2, IN).data, ...]
   * </code></pre>
   *
   * @param channels the iterable of channels.
   * @param <IN>     the input data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <IN> ChannelBuilder<Flow<? extends IN>, ?> mergeInput(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return mergeInput(0, channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the keys of the specified map.
   * <p>
   * Note that the builder will successfully create several channel instances.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, their final output will be:
   * <pre><code>
   * A =&gt; [Flow(key(A), IN).data, Flow(key(A), IN).data, ...]
   * B =&gt; [Flow(key(B), IN).data, Flow(key(B), IN).data, ...]
   * C =&gt; [Flow(key(C), IN).data, Flow(key(C), IN).data, ...]
   * </code></pre>
   *
   * @param channels the map of IDs and channels.
   * @param <IN>     the input data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <IN> ChannelBuilder<Flow<? extends IN>, ?> mergeInput(
      @NotNull final Map<Integer, ? extends Channel<? extends IN, ?>> channels) {
    return new MergeInputMapBuilder<IN>(channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the position in the array.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [Flow(1, B), Flow(0, A), Flow(2, C), Flow(0, A), ...]
   * </code></pre>
   *
   * @param channels the channels to merge.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, Flow<OUT>> mergeOutput(
      @NotNull final Channel<?, ?>... channels) {
    return mergeOutput(0, channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [Flow(startId + 1, B), Flow(startId + 0, A), Flow(startId + 2, C), ...]
   * </code></pre>
   *
   * @param startId  the flow start ID.
   * @param channels the array of channels.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <OUT> ChannelBuilder<?, Flow<OUT>> mergeOutput(final int startId,
      @NotNull final Channel<?, ?>... channels) {
    if (channels.length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    return (MergeOutputBuilder<OUT>) new MergeOutputBuilder<Object>(startId,
        Arrays.asList(channels));
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will start from the specified one.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [Flow(startId + 1, B), Flow(startId + 0, A), Flow(startId + 2, C), ...]
   * </code></pre>
   *
   * @param startId  the flow start ID.
   * @param channels the iterable of channels.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, Flow<OUT>> mergeOutput(final int startId,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return new MergeOutputBuilder<OUT>(startId, channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the position in the iterable.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [Flow(1, B), Flow(0, A), Flow(2, C), Flow(0, A), ...]
   * </code></pre>
   *
   * @param channels the channels to merge.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, Flow<OUT>> mergeOutput(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return mergeOutput(0, channels);
  }

  /**
   * Returns a builder of channels merging the specified instances into a flow one.
   * <br>
   * The flow IDs will be the keys of the specified map.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * ones will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channels {@code A}, {@code B} and {@code C}, the final output will be:
   * <pre><code>
   * =&gt; [Flow(key(B), B), Flow(key(A), A), Flow(key(C), C), ...]
   * </code></pre>
   *
   * @param channels the map of IDs and channels.
   * @param <OUT>    the output data type.
   * @return the flow channel builder.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, Flow<OUT>> mergeOutput(
      @NotNull final Map<Integer, ? extends Channel<?, ? extends OUT>> channels) {
    return new MergeMapBuilder<OUT>(channels);
  }

  /**
   * Returns a builder of list of channels.
   *
   * @param count  the number of channels to build.
   * @param <DATA> the data type.
   * @return the channel builder.
   * @throws java.lang.IllegalArgumentException if the specified count is negative.
   */
  @NotNull
  public static <DATA> ChannelListBuilder<DATA, DATA> number(final int count) {
    return new InstanceChannelListBuilder<DATA>(count);
  }

  /**
   * Returns a builder of channels making the data of the specified one a flow.
   * <br>
   * Each output will be passed along unchanged.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * one will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channel {@code A}, the final output will be:
   * <pre><code>
   * =&gt; [Flow(id, A), Flow(id, A), Flow(id, A), ...]
   * </code></pre>
   *
   * @param channel the channel.
   * @param id      the flow ID.
   * @param <OUT>   the output data type.
   * @return the flow channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, Flow<OUT>> outputFlow(
      @NotNull final Channel<?, ? extends OUT> channel, final int id) {
    return new OutputFlowBuilder<OUT>(channel, id);
  }

  /**
   * Returns a builder of channels repeating the output data to any newly bound channel or
   * consumer, thus effectively supporting multiple bindings.
   * <p>
   * The {@link com.github.dm.jrt.core.channel.Channel#isBound()} method will always return false
   * and the {@code bind()} methods will never fail.
   * <br>
   * Note, however, that the implementation will silently prevent the same consumer or channel
   * instance to be bound twice.
   * <p>
   * Note that the builder will successfully create only one channel instance, and that the passed
   * one will be bound as a result of the creation.
   * <br>
   * Note also that the returned channel will be already closed.
   * <p>
   * Given channel {@code A}, the final output will be:
   * <pre><code>
   * =&gt; [A, A, A, ...] =&gt; [A, A, A, ...] =&gt; [A, A, A, ...] ...
   * </code></pre>
   *
   * @param channel the channel.
   * @param <OUT>   the output data type.
   * @return the replaying channel builder.
   */
  @NotNull
  public static <OUT> ChannelBuilder<?, OUT> replayOutput(@NotNull final Channel<?, OUT> channel) {
    return new ReplayChannelBuilder<OUT>(channel);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private static <IN> ChannelBuilder<List<? extends IN>, ?> joinInput(final boolean isFlush,
      @Nullable final IN placeholder, @NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    return (JoinInputBuilder<IN>) new JoinInputBuilder<Object>(isFlush, placeholder,
        Arrays.asList(channels));
  }

  @NotNull
  private static <IN> ChannelBuilder<List<? extends IN>, ?> joinInput(final boolean isFlush,
      @Nullable final IN placeholder,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels) {
    return new JoinInputBuilder<IN>(isFlush, placeholder, channels);
  }

  @NotNull
  private static <OUT> ChannelBuilder<?, List<OUT>> joinOutput(final boolean isFlush,
      @Nullable final OUT placeholder,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels) {
    return new JoinOutputBuilder<OUT>(isFlush, placeholder, channels);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private static <OUT> ChannelBuilder<?, List<OUT>> joinOutput(final boolean isFlush,
      @Nullable final OUT placeholder, @NotNull final Channel<?, ?>... channels) {
    final int length = channels.length;
    if (length == 0) {
      throw new IllegalArgumentException("the array of channels must not be empty");
    }

    return (JoinOutputBuilder<OUT>) new JoinOutputBuilder<Object>(isFlush, placeholder,
        Arrays.asList(channels));
  }
}
