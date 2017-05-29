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

package com.github.dm.jrt.android.v11.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;
import android.util.SparseArray;

import com.github.dm.jrt.android.channel.ParcelableFlowData;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderSource;
import com.github.dm.jrt.channel.FlowData;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.DeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.common.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.immediateExecutor;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 08/03/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class JRoutineSparseChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public JRoutineSparseChannelsTest() {

    super(TestActivity.class);
  }

  @NotNull
  private static <T> Map<Integer, Channel<?, T>> toMap(
      @NotNull final SparseArray<? extends Channel<?, T>> sparseArray) {

    final int size = sparseArray.size();
    final HashMap<Integer, Channel<?, T>> map = new HashMap<Integer, Channel<?, T>>(size);
    for (int i = 0; i < size; ++i) {
      map.put(sparseArray.keyAt(i), sparseArray.valueAt(i));
    }

    return map;
  }

  public void testBlendOutput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<?, String> channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    Channel<?, String> channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannels.channelHandler()
                                     .blendOutputOf(channel2, channel1)
                                     .in(seconds(1))
                                     .all()).containsOnly("test1", "test2", "test3", "test4",
        "test5", "test6");
    channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannels.channelHandler()
                                     .blendOutputOf(
                                         Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                     .in(seconds(1))
                                     .all()).containsOnly("test1", "test2", "test3", "test4",
        "test5", "test6");
  }

  public void testBlendOutputAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<Object, Object> routine = JRoutineCore.routine().of(IdentityInvocation.factory());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler().blendOutputOf(channel1, channel2))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler()
                                         .blendOutputOf(
                                             Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testBlendOutputError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {

      JRoutineSparseChannels.channelHandler().blendOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().blendOutputOf((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().blendOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().blendOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().blendOutputOf((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .blendOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testCallableAsync() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineSparseChannels.channelHandler().channelOf(new Callable<String>() {

      public String call() {
        return "test";
      }
    }).in(seconds(1)).next()).isEqualTo("test");
  }

  public void testCallableSync() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineSparseChannels.channelHandlerOn(immediateExecutor())
                                     .channelOf(new Callable<String>() {

                                       public String call() throws InterruptedException {
                                         seconds(.3).sleepAtLeast();
                                         return "test";
                                       }
                                     })
                                     .next()).isEqualTo("test");
  }

  public void testConcatOutput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<?, String> channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    Channel<?, String> channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannels.channelHandler()
                                     .concatOutputOf(channel2, channel1)
                                     .in(seconds(1))
                                     .all()).containsExactly("test4", "test5", "test6", "test1",
        "test2", "test3");
    channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannels.channelHandler()
                                     .concatOutputOf(
                                         Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                     .in(seconds(1))
                                     .all()).containsExactly("test1", "test2", "test3", "test4",
        "test5", "test6");
  }

  public void testConcatOutputAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<Object, Object> routine = JRoutineCore.routine().of(IdentityInvocation.factory());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler().concatOutputOf(channel1, channel2))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler()
                                         .concatOutputOf(
                                             Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testConcatOutputError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {

      JRoutineSparseChannels.channelHandler().concatOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().concatOutputOf((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().concatOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .concatOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().concatOutputOf((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .concatOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<ParcelableFlowData<String>, ParcelableFlowData<String>> channel =
        JRoutineCore.channel().ofType();
    final TestLog testLog = new TestLog();
    JRoutineSparseChannels.channelHandler()
                          .withChannel()
                          .withLog(testLog)
                          .withLogLevel(Level.DEBUG)
                          .configuration()
                          .inputOfParcelableFlow(3, 1, channel)
                          .get(3)
                          .pass("test")
                          .close();
    assertThat(channel.close().all()).containsExactly(new ParcelableFlowData<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  @SuppressWarnings("unchecked")
  public void testConfigurationMap() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    final TestLog testLog = new TestLog();
    JRoutineSparseChannels.channelHandler()
                          .withChannel()
                          .withLog(testLog)
                          .withLogLevel(Level.DEBUG)
                          .configuration()
                          .inputOfFlow(3, 1, channel)
                          .get(3)
                          .pass("test")
                          .close();
    assertThat(channel.close().all()).containsExactly(new FlowData<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  public void testConstructor() {

    boolean failed = false;
    try {
      new JRoutineSparseChannels();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  public void testDuplicateInput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final List<Channel<String, String>> outputChannels =
        JRoutineSparseChannels.channelHandler().channels(3);
    JRoutineSparseChannels.channelHandler().duplicateInputOf(outputChannels).pass("test");
    for (final Channel<?, String> outputChannel : outputChannels) {
      assertThat(outputChannel.in(seconds(3)).next()).isEqualTo("test");
    }

    final Channel<String, String> channel1 = JRoutineCore.channel().ofType();
    final Channel<String, String> channel2 = JRoutineCore.channel().ofType();
    JRoutineSparseChannels.channelHandler().duplicateInputOf(channel1, channel2).pass("test");
    assertThat(channel1.in(seconds(3)).next()).isEqualTo("test");
    assertThat(channel2.in(seconds(3)).next()).isEqualTo("test");
  }

  public void testDuplicateOutput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    final List<Channel<?, String>> outputChannels =
        JRoutineSparseChannels.channelHandler().duplicateOutputOf(channel, 3);
    channel.pass("test");
    for (final Channel<?, String> outputChannel : outputChannels) {
      assertThat(outputChannel.in(seconds(3)).next()).isEqualTo("test");
    }
  }

  @SuppressWarnings("unchecked")
  public void testInputFlow() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannels.channelHandler()
                          .inputFlowOf(channel, 33)
                          .pass(new FlowData<String>(33, "test1"),
                              new FlowData<String>(-33, "test2"), new FlowData<String>(33, "test3"),
                              new FlowData<String>(333, "test4"))
                          .close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  public void testInputFlowAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannels.channelHandler().inputFlowOf(channel, 33).abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testInputMap() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ArrayList<FlowData<Object>> outputs = new ArrayList<FlowData<Object>>();
    outputs.add(new FlowData<Object>(Sort.STRING, "test21"));
    outputs.add(new FlowData<Object>(Sort.INTEGER, -11));
    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<FlowData<Object>, FlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap =
        JRoutineSparseChannels.channelHandler().inputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                           channel);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  public void testInputMapAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<FlowData<Object>, FlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap =
        JRoutineSparseChannels.channelHandler().inputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                           channel);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testInputMapAbortParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity())).of(factoryOf(Sort.class));
    SparseArray<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlowData<Object>, ParcelableFlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfParcelableFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfParcelableFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfParcelableFlow(Math.min(Sort.INTEGER, Sort.STRING),
                                           2, channel);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testInputMapError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      JRoutineSparseChannels.channelHandler()
                            .inputOfFlow(0, 0, JRoutineCore.routine().of(new SortFlow()).invoke());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testInputMapErrorParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .inputOfParcelableFlow(0, 0,
                                JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                                              .of(factoryOf(Sort.class))
                                              .invoke());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testInputMapParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final ArrayList<ParcelableFlowData<Object>> outputs =
        new ArrayList<ParcelableFlowData<Object>>();
    outputs.add(new ParcelableFlowData<Object>(Sort.STRING, "test21"));
    outputs.add(new ParcelableFlowData<Object>(Sort.INTEGER, -11));
    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity())).of(factoryOf(Sort.class));
    SparseArray<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlowData<Object>, ParcelableFlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfParcelableFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfParcelableFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .inputOfParcelableFlow(Math.min(Sort.INTEGER, Sort.STRING),
                                           2, channel);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  @SuppressWarnings("unchecked")
  public void testInputSelect() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannels.channelHandler()
                          .inputOfFlow(channel, 33)
                          .pass("test1", "test2", "test3")
                          .close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly(
        new FlowData<String>(33, "test1"), new FlowData<String>(33, "test2"),
        new FlowData<String>(33, "test3"));
  }

  public void testInputSelectAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannels.channelHandler()
                          .inputOfFlow(channel, 33)
                          .pass("test1", "test2", "test3")
                          .abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testInputSelectError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();

    try {
      JRoutineSparseChannels.channelHandler().inputOfFlow(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannels.channelHandler().inputOfFlow(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannels.channelHandler()
                            .inputOfFlow(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoin() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(
                          JRoutineSparseChannels.channelHandler().joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineSparseChannels.channelHandler()
                                                  .joinOutputOf(
                                                      Arrays.<Channel<?, ?>>asList(channel1,
                                                          channel2)))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(
                          JRoutineSparseChannels.channelHandler().joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
  }

  public void testJoinAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler().joinOutputOf(channel1, channel2))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler()
                                         .joinOutputOf(
                                             Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinBackoff() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.after(millis(100)).pass("test").pass("test").close();
    try {
      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler()
                                         .withChannel()
                                         .withBackoff(afterCount(0).constantDelay(millis(100)))
                                         .withMaxSize(1)
                                         .configuration()
                                         .joinOutputOf(channel1, channel2))
             .close()
             .in(seconds(10))
             .all();
      fail();

    } catch (final DeadlockException ignored) {

    }
  }

  public void testJoinError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {

      JRoutineSparseChannels.channelHandler().joinOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .joinOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoinInput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf(channel1, channel2)
                          .pass(Arrays.asList("test1-1", "test1-2"))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .pass(Arrays.asList("test2-1", "test2-2"))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf(channel1, channel2)
                          .pass(Collections.singletonList("test3-1"))
                          .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly("test1-2", "test2-2");
  }

  public void testJoinInputAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler().joinInputOf(channel1, channel2).abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinInputError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf(channel1)
                          .pass(Arrays.asList("test1-1", "test1-2"))
                          .close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinInputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .joinInputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoinInputPlaceHolderError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf((Object) null, channel1)
                          .pass(Arrays.asList("test1-1", "test1-2"))
                          .close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinInputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .joinInputOf(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinInputOf(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .joinInputOf(new Object(),
                                Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoinInputPlaceholder() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf((Object) null, channel1, channel2)
                          .pass(Arrays.asList("test1-1", "test1-2"))
                          .close();
    final String placeholder = "placeholder";
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf((Object) placeholder,
                              Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .pass(Arrays.asList("test2-1", "test2-2"))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf(placeholder, channel1, channel2)
                          .pass(Collections.singletonList("test3-1"))
                          .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly("test1-2", "test2-2",
        placeholder);
  }

  public void testJoinInputPlaceholderAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler().joinInputOf((Object) null, channel1, channel2).abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .joinInputOf(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinPlaceholder() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineSparseChannels.channelHandler()
                                                  .joinOutputOf(new Object(), channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineSparseChannels.channelHandler()
                                                  .joinOutputOf(null,
                                                      Arrays.<Channel<?, ?>>asList(channel1,
                                                          channel2)))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler()
                                         .joinOutputOf(new Object(), channel1, channel2))
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final InvocationException ignored) {

    }
  }

  public void testJoinPlaceholderAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler()
                                         .joinOutputOf((Object) null, channel1, channel2))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(JRoutineSparseChannels.channelHandler()
                                         .joinOutputOf(new Object(),
                                             Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinPlaceholderError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {

      JRoutineSparseChannels.channelHandler().joinOutputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .joinOutputOf(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().joinOutputOf(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .joinOutputOf(new Object(),
                                Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testList() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final int count = 7;
    final List<? extends Channel<Object, Object>> channels =
        JRoutineSparseChannels.channelHandlerOn(immediateExecutor()).channels(count);
    for (final Channel<Object, Object> channel : channels) {
      assertThat(channel.pass("test").next()).isEqualTo("test");
    }
  }

  public void testListEmpty() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineSparseChannels.channelHandler().channels(0)).isEmpty();
  }

  public void testListError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    JRoutineSparseChannels.channelHandler().channels(-1);
  }

  public void testMap() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();

    final Channel<?, ? extends FlowData<Object>> channel = JRoutineSparseChannels.channelHandler()
                                                                                 .mergeOutputOf(
                                                                                     Arrays
                                                                                         .<Channel<?, ?>>asList(
                                                                                         channel1,
                                                                                         channel2));
    final Channel<?, FlowData<Object>> output = JRoutineCore.routine()
                                                            .withInvocation()
                                                            .withInputOrder(OrderType.SORTED)
                                                            .configuration()
                                                            .of(new SortFlow())
                                                            .invoke()
                                                            .pass(channel)
                                                            .close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannels.channelHandler().outputOfFlow(output, Sort.INTEGER, Sort.STRING);

    for (int i = 0; i < 4; i++) {

      final String input = Integer.toString(i);
      channel1.after(millis(20)).pass(input);
      channel2.after(millis(20)).pass(i);
    }

    channel1.close();
    channel2.close();

    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsExactly("0", "1", "2",
        "3");
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsExactly(0, 1, 2, 3);
  }

  public void testMapParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();

    final Channel<?, ? extends ParcelableFlowData<Object>> channel =
        JRoutineSparseChannels.channelHandler()
                              .mergeParcelableOutputOf(
                                  Arrays.<Channel<?, ?>>asList(channel1, channel2));
    final Channel<?, ParcelableFlowData<Object>> output =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                      .withInvocation()
                      .withInputOrder(OrderType.SORTED)
                      .configuration()
                      .of(factoryOf(Sort.class))
                      .invoke()
                      .pass(channel)
                      .close();
    final SparseArray<? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannels.channelHandler()
                              .outputOfParcelableFlow(output, Sort.INTEGER, Sort.STRING);

    for (int i = 0; i < 4; i++) {

      final String input = Integer.toString(i);
      channel1.after(millis(20)).pass(input);
      channel2.after(millis(20)).pass(i);
    }

    channel1.close();
    channel2.close();

    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsExactly("0", "1", "2",
        "3");
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsExactly(0, 1, 2, 3);
  }

  public void testMerge() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends FlowData<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler().mergeOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.pass(13).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(-7, "test1"),
        new FlowData<Integer>(-6, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler()
                                          .mergeOutputOf(11,
                                              Arrays.<Channel<?, ?>>asList(channel1, channel2));
    channel2.pass(13).close();
    channel1.pass("test1").close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(11, "test1"),
        new FlowData<Integer>(12, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler().mergeOutputOf(channel1, channel2);
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(0, "test2"),
        new FlowData<Integer>(1, -17));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler()
                                          .mergeOutputOf(
                                              Arrays.<Channel<?, ?>>asList(channel1, channel2));
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(0, "test2"),
        new FlowData<Integer>(1, -17));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = JRoutineSparseChannels.channelHandler().mergeOutputOf(channelMap);
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(7, "test3"),
        new FlowData<Integer>(-3, 111));
  }

  @SuppressWarnings("unchecked")
  public void testMerge4() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<String, String> channel2 = builder.ofType();
    final Channel<String, String> channel3 = builder.ofType();
    final Channel<String, String> channel4 = builder.ofType();

    final Routine<FlowData<String>, String> routine =
        JRoutineCore.routine().of(InvocationFactory.factoryOf(new ClassToken<Amb<String>>() {}));
    final Channel<?, String> outputChannel = routine.invoke()
                                                    .pass(JRoutineSparseChannels.channelHandler()
                                                                                .mergeOutputOf(
                                                                                    Arrays.asList(
                                                                                        channel1,
                                                                                        channel2,
                                                                                        channel3,
                                                                                        channel4)))
                                                    .close();

    for (int i = 0; i < 4; i++) {

      final String input = Integer.toString(i);
      channel1.after(millis(20)).pass(input);
      channel2.after(millis(20)).pass(input);
      channel3.after(millis(20)).pass(input);
      channel4.after(millis(20)).pass(input);
    }

    channel1.close();
    channel2.close();
    channel3.close();
    channel4.close();

    assertThat(outputChannel.in(seconds(10)).all()).containsExactly("0", "1", "2", "3");
  }

  public void testMergeAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends FlowData<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler().mergeOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler()
                                          .mergeOutputOf(11,
                                              Arrays.<Channel<?, ?>>asList(channel1, channel2));
    channel2.abort();
    channel1.pass("test1").close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler().mergeOutputOf(channel1, channel2);
    channel1.abort();
    channel2.pass(-17).close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannels.channelHandler()
                                          .mergeOutputOf(
                                              Arrays.<Channel<?, ?>>asList(channel1, channel2));
    channel1.pass("test2").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = JRoutineSparseChannels.channelHandler().mergeOutputOf(channelMap);
    channel1.abort();
    channel2.pass(111).close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testMergeAbortParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();
    final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlowData<?>> outputChannel =
        JRoutineSparseChannels.channelHandler().mergeParcelableOutputOf(channelMap);
    channel1.abort();
    channel2.pass(111).close();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testMergeError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeOutputOf(0, Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeOutputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeOutputOf(Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeOutputOf(Collections.<Integer, Channel<?, Object>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeOutputOf(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeOutputOf(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeOutputOf(
                                Collections.<Integer, Channel<?, ?>>singletonMap(1, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testMergeErrorParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeParcelableOutputOf(new SparseArray<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMergeInput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(channel1, channel2)
                          .pass(new FlowData<String>(0, "test1"))
                          .pass(new FlowData<Integer>(1, 1))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(3, channel1, channel2)
                          .pass(new FlowData<String>(3, "test2"))
                          .pass(new FlowData<Integer>(4, 2))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .pass(new FlowData<String>(0, "test3"))
                          .pass(new FlowData<Integer>(1, 3))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .pass(new FlowData<String>(-5, "test4"))
                          .pass(new FlowData<Integer>(-4, 4))
                          .close();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(map)
                          .pass(new FlowData<String>(31, "test5"))
                          .pass(new FlowData<Integer>(17, 5))
                          .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  public void testMergeInputAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler().mergeInputOf(channel1, channel2).abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler().mergeInputOf(3, channel1, channel2).abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineSparseChannels.channelHandler().mergeInputOf(map).abort();

    try {

      channel1.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testMergeInputAbortParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingString.class))
                             .invoke()
                             .sorted();
    channel2 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingInteger.class))
                             .invoke()
                             .sorted();
    JRoutineSparseChannels.channelHandler().mergeInputOf(channel1, channel2).abort();

    try {

      channel1.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingString.class))
                             .invoke()
                             .sorted();
    channel2 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingInteger.class))
                             .invoke()
                             .sorted();
    JRoutineSparseChannels.channelHandler().mergeInputOf(3, channel1, channel2).abort();

    try {

      channel1.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingString.class))
                             .invoke()
                             .sorted();
    channel2 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingInteger.class))
                             .invoke()
                             .sorted();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .abort();

    try {

      channel1.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingString.class))
                             .invoke()
                             .sorted();
    channel2 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingInteger.class))
                             .invoke()
                             .sorted();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .abort();

    try {

      channel1.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingString.class))
                             .invoke()
                             .sorted();
    channel2 = JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                             .of(factoryOf(PassingInteger.class))
                             .invoke()
                             .sorted();
    final SparseArray<Channel<?, ?>> map = new SparseArray<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineSparseChannels.channelHandler().mergeParcelableInputOf(map).abort();

    try {

      channel1.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().in(seconds(10)).next();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testMergeInputError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeInputOf(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeInputOf(Collections.<Integer, Channel<?, ?>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeInputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeInputOf(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeInputOf(
                                Collections.<Integer, Channel<?, ?>>singletonMap(0, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testMergeInputErrorParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler().mergeInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeInputOf(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .mergeParcelableInputOf(new SparseArray<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMergeInputParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Channel<String, String> channel1 =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                      .of(factoryOf(PassingString.class))
                      .invoke()
                      .sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                      .of(factoryOf(PassingInteger.class))
                      .invoke()
                      .sorted();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(channel1, channel2)
                          .pass(new ParcelableFlowData<String>(0, "test1"))
                          .pass(new ParcelableFlowData<Integer>(1, 1))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(3, channel1, channel2)
                          .pass(new ParcelableFlowData<String>(3, "test2"))
                          .pass(new ParcelableFlowData<Integer>(4, 2))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .pass(new ParcelableFlowData<String>(0, "test3"))
                          .pass(new ParcelableFlowData<Integer>(1, 3))
                          .close();
    JRoutineSparseChannels.channelHandler()
                          .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                          .pass(new ParcelableFlowData<String>(-5, "test4"))
                          .pass(new ParcelableFlowData<Integer>(-4, 4))
                          .close();
    final SparseArray<Channel<?, ?>> map = new SparseArray<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineSparseChannels.channelHandler()
                          .mergeParcelableInputOf(map)
                          .pass(new ParcelableFlowData<String>(31, "test5"))
                          .pass(new ParcelableFlowData<Integer>(17, 5))
                          .close();
    assertThat(channel1.close().in(seconds(10)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().in(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  @SuppressWarnings("unchecked")
  public void testMergeParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();
    final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlowData<?>> outputChannel =
        JRoutineSparseChannels.channelHandler().mergeParcelableOutputOf(channelMap);
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlowData<String>(7, "test3"), new ParcelableFlowData<Integer>(-3, 111));
  }

  @SuppressWarnings("unchecked")
  public void testOutputFlow() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").close();
    assertThat(JRoutineSparseChannels.channelHandler()
                                     .outputFlowOf(channel, 33)
                                     .in(seconds(1))
                                     .all()).containsExactly(new FlowData<String>(33, "test1"),
        new FlowData<String>(33, "test2"), new FlowData<String>(33, "test3"));
  }

  public void testOutputFlowAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").abort();

    try {

      JRoutineSparseChannels.channelHandler().outputFlowOf(channel, 33).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMap() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routineOn(syncExecutor()).of(new SortFlow());
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, FlowData<Object>> channel;
    channel = routine.invoke()
                     .pass(new FlowData<Object>(Sort.STRING, "test21"),
                         new FlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new FlowData<Object>(Sort.INTEGER, -11),
                         new FlowData<Object>(Sort.STRING, "test21"))
                     .close();
    channelMap =
        JRoutineSparseChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new FlowData<Object>(Sort.STRING, "test21"),
                         new FlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                           channel);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, FlowData<Object>> channel;
    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new FlowData<Object>(Sort.STRING, "test21"),
                         new FlowData<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    channel.abort();

    try {

      channelMap.get(Sort.STRING).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new FlowData<Object>(Sort.INTEGER, -11),
                         new FlowData<Object>(Sort.STRING, "test21"));
    channelMap =
        JRoutineSparseChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    channel.abort();

    try {

      channelMap.get(Sort.STRING).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new FlowData<Object>(Sort.STRING, "test21"),
                         new FlowData<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                           channel);
    channel.abort();

    try {

      channelMap.get(Sort.STRING).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapAbortParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity())).of(factoryOf(Sort.class));
    SparseArray<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlowData<Object>> channel;
    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfParcelableFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    channel.abort();

    try {

      channelMap.get(Sort.STRING).in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new ParcelableFlowData<Object>(Sort.INTEGER, -11),
                         new ParcelableFlowData<Object>(Sort.STRING, "test21"));
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfParcelableFlow(channel, Sort.INTEGER, Sort.STRING);
    channel.abort();

    try {

      channelMap.get(Sort.STRING).in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfParcelableFlow(Math.min(Sort.INTEGER, Sort.STRING),
                                           2, channel);
    channel.abort();

    try {

      channelMap.get(Sort.STRING).in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testOutputMapCache() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(InvocationFactory.factoryOf(SortFlow.class));
    final Channel<?, FlowData<Object>> channel = routine.invoke();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannels.channelHandler()
                              .outputOfFlow(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap).isEqualTo(
        JRoutineSparseChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING));
  }

  public void testOutputMapCacheParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity())).of(factoryOf(Sort.class));
    final Channel<?, ParcelableFlowData<Object>> channel = routine.invoke();
    final SparseArray<? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannels.channelHandler()
                              .outputOfParcelableFlow(channel,
                                  Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(toMap(channelMap)).isEqualTo(toMap(JRoutineSparseChannels.channelHandler()
                                                                        .outputOfParcelableFlow(
                                                                            channel, Sort.INTEGER,
                                                                            Sort.STRING)));
  }

  public void testOutputMapError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(0, 0, JRoutineCore.routine().of(new SortFlow()).invoke());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testOutputMapErrorParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .outputOfParcelableFlow(0, 0,
                                JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity()))
                                              .of(factoryOf(Sort.class))
                                              .invoke());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoader.routineOn(LoaderSource.loaderOf(getActivity())).of(factoryOf(Sort.class));
    SparseArray<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlowData<Object>> channel;
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfParcelableFlow(channel,
                                           Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.INTEGER, -11),
                         new ParcelableFlowData<Object>(Sort.STRING, "test21"))
                     .close();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfParcelableFlow(channel, Sort.INTEGER, Sort.STRING);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannels.channelHandler()
                                       .outputOfParcelableFlow(Math.min(Sort.INTEGER, Sort.STRING),
                                           2, channel);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputSelect() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannels.channelHandler().outputOfFlow(channel, 33).get(33);
    channel.pass(new FlowData<String>(33, "test1"), new FlowData<String>(-33, "test2"),
        new FlowData<String>(33, "test3"), new FlowData<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  public void testOutputSelectAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannels.channelHandler().outputOfFlow(channel, 33).get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testOutputSelectAbortParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Channel<ParcelableFlowData<String>, ParcelableFlowData<String>> channel =
        JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannels.channelHandler().outputOfParcelableFlow(channel, 33).get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testOutputSelectError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();

    try {
      JRoutineSparseChannels.channelHandler().outputOfFlow(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannels.channelHandler().outputOfFlow(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputSelectParcelable() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Channel<ParcelableFlowData<String>, ParcelableFlowData<String>> channel =
        JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannels.channelHandler().outputOfParcelableFlow(channel, 33).get(33);
    channel.pass(new ParcelableFlowData<String>(33, "test1"),
        new ParcelableFlowData<String>(-33, "test2"), new ParcelableFlowData<String>(33, "test3"),
        new ParcelableFlowData<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly("test1", "test3");
  }

  @SuppressWarnings("ConstantConditions")
  public void testReplayError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      JRoutineSparseChannels.channelHandler().replayOutputOf(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testSelectMap() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    final Channel<FlowData<Object>, FlowData<Object>> inputChannel =
        JRoutineCore.channel().ofType();
    final Channel<?, FlowData<Object>> outputChannel = routine.invoke().pass(inputChannel).close();
    final Channel<?, Object> intChannel = JRoutineSparseChannels.channelHandler()
                                                                .withChannel()
                                                                .withLogLevel(Level.WARNING)
                                                                .configuration()
                                                                .outputOfFlow(outputChannel,
                                                                    Sort.INTEGER, Sort.STRING)
                                                                .get(Sort.INTEGER);
    final Channel<?, Object> strChannel = JRoutineSparseChannels.channelHandler()
                                                                .withChannel()
                                                                .withLogLevel(Level.WARNING)
                                                                .configuration()
                                                                .outputOfFlow(outputChannel,
                                                                    Sort.STRING, Sort.INTEGER)
                                                                .get(Sort.STRING);
    inputChannel.pass(new FlowData<Object>(Sort.STRING, "test21"),
        new FlowData<Object>(Sort.INTEGER, -11));
    assertThat(intChannel.in(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.in(seconds(10)).next()).isEqualTo("test21");
    inputChannel.pass(new FlowData<Object>(Sort.INTEGER, -11),
        new FlowData<Object>(Sort.STRING, "test21"));
    assertThat(intChannel.in(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.in(seconds(10)).next()).isEqualTo("test21");
    inputChannel.pass(new FlowData<Object>(Sort.STRING, "test21"),
        new FlowData<Object>(Sort.INTEGER, -11));
    assertThat(intChannel.in(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.in(seconds(10)).next()).isEqualTo("test21");
  }

  @SuppressWarnings("unchecked")
  public void testSelectMapAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Channel<FlowData<Object>, FlowData<Object>> inputChannel = JRoutineCore.channel().ofType();
    Channel<?, FlowData<Object>> outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineSparseChannels.channelHandler().outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING);
    inputChannel.after(millis(100))
                .pass(new FlowData<Object>(Sort.STRING, "test21"),
                    new FlowData<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                            .get(Sort.STRING)
                            .in(seconds(1))
                            .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
                            .get(Sort.INTEGER)
                            .in(seconds(1))
                            .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.channel().ofType();
    outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineSparseChannels.channelHandler().outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING);
    inputChannel.after(millis(100))
                .pass(new FlowData<Object>(Sort.INTEGER, -11),
                    new FlowData<Object>(Sort.STRING, "test21"))
                .abort();

    try {

      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                            .get(Sort.STRING)
                            .in(seconds(1))
                            .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                            .get(Sort.INTEGER)
                            .in(seconds(1))
                            .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.channel().ofType();
    outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineSparseChannels.channelHandler().outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER);
    inputChannel.after(millis(100))
                .pass(new FlowData<Object>(Sort.STRING, "test21"),
                    new FlowData<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
                            .get(Sort.STRING)
                            .in(seconds(1))
                            .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineSparseChannels.channelHandler()
                            .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
                            .get(Sort.INTEGER)
                            .in(seconds(1))
                            .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  private static class Amb<DATA> extends TemplateInvocation<FlowData<DATA>, DATA> {

    private static final int NO_INDEX = Integer.MIN_VALUE;

    private int mFirstIndex;

    @Override
    public void onInput(final FlowData<DATA> input, @NotNull final Channel<DATA, ?> result) {
      if (mFirstIndex == NO_INDEX) {
        mFirstIndex = input.id;
        result.pass(input.data);

      } else if (mFirstIndex == input.id) {
        result.pass(input.data);
      }
    }

    @Override
    public boolean onRecycle() {
      return true;
    }

    @Override
    public void onStart() {
      mFirstIndex = NO_INDEX;
    }
  }

  private static class CharAt extends MappingInvocation<List<?>, Character> {

    /**
     * Constructor.
     */
    protected CharAt() {

      super(null);
    }

    public void onInput(final List<?> objects, @NotNull final Channel<Character, ?> result) {

      final String text = (String) objects.get(0);
      final int index = ((Integer) objects.get(1));
      result.pass(text.charAt(index));
    }
  }

  private static class PassingInteger extends TemplateContextInvocation<Integer, Integer> {

    @Override
    public void onInput(final Integer i, @NotNull final Channel<Integer, ?> result) {
      result.pass(i);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class PassingString extends TemplateContextInvocation<String, String> {

    @Override
    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      result.pass(s);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class Sort
      extends TemplateContextInvocation<ParcelableFlowData<Object>, ParcelableFlowData<Object>> {

    private static final int INTEGER = 1;

    private static final int STRING = 0;

    @Override
    public void onInput(final ParcelableFlowData<Object> flow,
        @NotNull final Channel<ParcelableFlowData<Object>, ?> result) {
      switch (flow.id) {
        case INTEGER:
          JRoutineSparseChannels.channelHandler().<Object, Integer>inputOfParcelableFlow(result,
              INTEGER).pass((Integer) flow.data).close();
          break;

        case STRING:
          JRoutineSparseChannels.channelHandler().<Object, String>inputOfParcelableFlow(result,
              STRING).pass((String) flow.data).close();
          break;
      }
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class SortFlow extends MappingInvocation<FlowData<Object>, FlowData<Object>> {

    private static final int INTEGER = 1;

    private static final int STRING = 0;

    /**
     * Constructor.
     */
    protected SortFlow() {

      super(null);
    }

    public void onInput(final FlowData<Object> flowData,
        @NotNull final Channel<FlowData<Object>, ?> result) {

      switch (flowData.id) {

        case INTEGER:
          JRoutineSparseChannels.channelHandler().<Object, Integer>inputOfFlow(result,
              INTEGER).pass(flowData.<Integer>data()).close();
          break;

        case STRING:
          JRoutineSparseChannels.channelHandler().<Object, String>inputOfFlow(result, STRING).pass(
              flowData.<String>data()).close();
          break;
      }
    }
  }

  private static class TestLog implements Log {

    private int mLogCount;

    @Override
    public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mLogCount;
    }

    @Override
    public void err(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mLogCount;
    }

    @Override
    public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mLogCount;
    }
  }
}
