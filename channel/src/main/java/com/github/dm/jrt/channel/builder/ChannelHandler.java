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

package com.github.dm.jrt.channel.builder;

import com.github.dm.jrt.channel.Flow;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfigurable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Interface defining a class handling routine channels.
 * <br>
 * The interface provides several methods to split and merge channels together, making also possible
 * to transfer data in multiple flows, each one identified by a specific ID.
 * <p>
 * Created by davide-maestroni on 04/24/2017.
 */
public interface ChannelHandler extends ChannelConfigurable<ChannelHandler> {

  /**
   * Returns a channel blending the outputs coming from the specified ones.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <OUT> Channel<?, OUT> blendOutputOf(@NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel blending the outputs coming from the specified ones.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <OUT> Channel<?, OUT> blendOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel producing the result of the specified Callable.
   *
   * @param callable the Callable instance.
   * @param <OUT>    the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> channelOf(@NotNull final Callable<OUT> callable);

  /**
   * Returns a builder of channels producing the result of the specified Future.
   * <br>
   * If the channel is aborted the Future will be cancelled with {@code mayInterruptIfRunning} set
   * to false.
   * <p>
   * Note that the configured executor will be employed to wait for the Future to complete.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param future the Future instance.
   * @param <OUT>  the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> channelOf(@NotNull final Future<OUT> future);

  /**
   * Returns a channel producing the result of the specified Future.
   * <br>
   * If the channel is aborted the Future will be cancelled with {@code mayInterruptIfRunning} set
   * to the specified value.
   * <p>
   * Note that the configured executor will be employed to wait for the Future to complete.
   * <br>
   * Note also that the returned channel will be already closed.
   *
   * @param future                the Future instance.
   * @param mayInterruptIfRunning true if the thread executing the Future task should be
   *                              interrupted; otherwise, in-progress tasks are allowed to complete.
   * @param <OUT>                 the output data type.
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> channelOf(@NotNull final Future<OUT> future,
      final boolean mayInterruptIfRunning);

  /**
   * Returns a list of channels.
   *
   * @param count  the number of channels to build.
   * @param <DATA> the data type.
   * @return the channels.
   * @throws java.lang.IllegalArgumentException if the specified count is negative.
   */
  @NotNull
  <DATA> List<Channel<DATA, DATA>> channels(final int count);

  /**
   * Returns a channel concatenating the outputs coming from the specified ones, so that, all the
   * outputs of the first channel will come before all the outputs of the second one, and so on.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <OUT> Channel<?, OUT> concatOutputOf(@NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel concatenating the outputs coming from the specified ones, so that, all the
   * outputs of the first channel will come before all the outputs of the second one, and so on.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <OUT> Channel<?, OUT> concatOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel of flows feeding the specified one.
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
   * @return the channel instance.
   */
  @NotNull
  <IN> Channel<Flow<IN>, ?> inputFlowOf(@NotNull final Channel<? super IN, ?> channel,
      final int id);

  /**
   * Returns a channel transforming the input data into flow ones.
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
   * @return the channel instance.
   */
  @NotNull
  <DATA, IN extends DATA> Channel<IN, ?> inputOfFlow(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, final int id);

  /**
   * Returns a map of channels accepting the data identified by the specified IDs.
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
   * @return the map of IDs and channels.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   */
  @NotNull
  <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, @NotNull final int... ids);

  /**
   * Returns a map of channels accepting the data identified by the specified IDs.
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
   * @return the map of IDs and channels.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   */
  @NotNull
  <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(
      @NotNull final Channel<? super Flow<DATA>, ?> channel, @NotNull final Iterable<Integer> ids);

  /**
   * Returns a map of channels accepting the data identified by the specified IDs.
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
   * @return the map of IDs and channels.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   */
  @NotNull
  <DATA, IN extends DATA> Map<Integer, Channel<IN, ?>> inputOfFlow(final int startId,
      final int rangeSize, @NotNull final Channel<? super Flow<DATA>, ?> channel);

  /**
   * Returns a channel distributing the input data among the specified ones. If the list of data
   * exceeds the number of channels, the invocation will be aborted.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <IN> Channel<List<? extends IN>, ?> joinInputOf(@NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel distributing the input data among the specified ones. If the list of data is
   * smaller than the specified number of channels, the remaining ones will be fed with the
   * specified placeholder instance. While, if the list of data exceeds the number of channels, the
   * invocation will be aborted.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <IN> Channel<List<? extends IN>, ?> joinInputOf(@Nullable final IN placeholder,
      @NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel distributing the input data among the specified ones. If the list of data is
   * smaller than the specified number of channels, the remaining ones will be fed with the
   * specified placeholder instance. While, if the list of data exceeds the number of channels, the
   * invocation will be aborted.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <IN> Channel<List<? extends IN>, ?> joinInputOf(@Nullable final IN placeholder,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels);

  /**
   * Returns a channel distributing the input data among the specified ones. If the list of data
   * exceeds the number of channels, the invocation will be aborted.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <IN> Channel<List<? extends IN>, ?> joinInputOf(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels);

  /**
   * Returns a channel joining the data coming from the specified ones.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <OUT> Channel<?, List<OUT>> joinOutputOf(@NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel joining the data coming from the specified ones.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <OUT> Channel<?, List<OUT>> joinOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel joining the data coming from the specified ones.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <OUT> Channel<?, List<OUT>> joinOutputOf(@Nullable final OUT placeholder,
      @NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel joining the data coming from the specified ones.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <OUT> Channel<?, List<OUT>> joinOutputOf(@Nullable final OUT placeholder,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(@NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(final int startId,
      @NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(final int startId,
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(
      @NotNull final Iterable<? extends Channel<? extends IN, ?>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   */
  @NotNull
  <IN> Channel<Flow<? extends IN>, ?> mergeInputOf(
      @NotNull final Map<Integer, ? extends Channel<? extends IN, ?>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <OUT> Channel<?, Flow<OUT>> mergeOutputOf(@NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified array is empty.
   * @throws java.lang.NullPointerException     if the specified array is null or contains a null
   *                                            object.
   */
  @NotNull
  <OUT> Channel<?, Flow<OUT>> mergeOutputOf(final int startId,
      @NotNull final Channel<?, ?>... channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <OUT> Channel<?, Flow<OUT>> mergeOutputOf(final int startId,
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified iterable is empty.
   * @throws java.lang.NullPointerException     if the specified iterable is null or contains a
   *                                            null object.
   */
  @NotNull
  <OUT> Channel<?, Flow<OUT>> mergeOutputOf(
      @NotNull final Iterable<? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel merging the specified instances into a flow one.
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
   * @return the channel instance.
   * @throws java.lang.IllegalArgumentException if the specified map is empty.
   * @throws java.lang.NullPointerException     if the specified map is null or contains a null
   *                                            object.
   */
  @NotNull
  <OUT> Channel<?, Flow<OUT>> mergeOutputOf(
      @NotNull final Map<Integer, ? extends Channel<?, ? extends OUT>> channels);

  /**
   * Returns a channel making the data of the specified one a flow.
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
   * @return the channel instance.
   */
  @NotNull
  <OUT> Channel<?, Flow<OUT>> outputFlowOf(@NotNull final Channel<?, ? extends OUT> channel,
      final int id);

  /**
   * Returns a map of channels returning the output data filtered by the specified IDs.
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
   * @return the map of IDs and channels.
   * @throws java.lang.NullPointerException if the specified array is null or contains a null
   *                                        object.
   */
  @NotNull
  <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel, @NotNull final int... ids);

  /**
   * Returns a map of channels returning the output data filtered by the specified IDs.
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
   * @return the map of IDs and channels.
   * @throws java.lang.NullPointerException if the specified iterable is null or returns a null
   *                                        object.
   */
  @NotNull
  <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel,
      @NotNull final Iterable<Integer> ids);

  /**
   * Returns a map of channels returning the output data filtered by the specified IDs.
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
   * @return the map of IDs and channels.
   * @throws java.lang.IllegalArgumentException if the specified range size is not positive.
   */
  @NotNull
  <OUT> Map<Integer, Channel<?, OUT>> outputOfFlow(final int startId, final int rangeSize,
      @NotNull final Channel<?, ? extends Flow<? extends OUT>> channel);

  /**
   * Returns a channel repeating the output data to any newly bound channel or
   * consumer, thus effectively supporting multiple bindings.
   * <p>
   * The {@link com.github.dm.jrt.core.channel.Channel#isBound()} method will always return false
   * and the binding methods will never fail.
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
   * @return the replaying channel instance.
   */
  @NotNull
  <OUT> Channel<?, OUT> replayOutputOf(@NotNull final Channel<?, OUT> channel);
}
