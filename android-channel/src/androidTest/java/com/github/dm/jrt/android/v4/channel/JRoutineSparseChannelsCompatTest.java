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

package com.github.dm.jrt.android.v4.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.support.v4.util.SparseArrayCompat;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.channel.ParcelableFlowData;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
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
public class JRoutineSparseChannelsCompatTest
    extends ActivityInstrumentationTestCase2<TestActivity> {

  public JRoutineSparseChannelsCompatTest() {

    super(TestActivity.class);
  }

  @NotNull
  private static <T> Map<Integer, Channel<?, T>> toMap(
      @NotNull final SparseArrayCompat<? extends Channel<?, T>> sparseArray) {

    final int size = sparseArray.size();
    final HashMap<Integer, Channel<?, T>> map = new HashMap<Integer, Channel<?, T>>(size);
    for (int i = 0; i < size; ++i) {
      map.put(sparseArray.keyAt(i), sparseArray.valueAt(i));
    }

    return map;
  }

  public void testBlendOutput() {

    Channel<?, String> channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    Channel<?, String> channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannelsCompat.channelHandler()
                                           .blendOutputOf(channel2, channel1)
                                           .in(seconds(1))
                                           .all()).containsOnly("test1", "test2", "test3", "test4",
        "test5", "test6");
    channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannelsCompat.channelHandler()
                                           .blendOutputOf(
                                               Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                           .in(seconds(1))
                                           .all()).containsOnly("test1", "test2", "test3", "test4",
        "test5", "test6");
  }

  public void testBlendOutputAbort() {

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
             .pass(JRoutineSparseChannelsCompat.channelHandler().blendOutputOf(channel1, channel2))
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
             .pass(JRoutineSparseChannelsCompat.channelHandler()
                                               .blendOutputOf(Arrays.<Channel<?, ?>>asList(channel1,
                                                   channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testBlendOutputError() {

    try {

      JRoutineSparseChannelsCompat.channelHandler().blendOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().blendOutputOf((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().blendOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .blendOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().blendOutputOf((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .blendOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testCallableAsync() {
    assertThat(JRoutineSparseChannelsCompat.channelHandler().channelOf(new Callable<String>() {

      public String call() {
        return "test";
      }
    }).in(seconds(1)).next()).isEqualTo("test");
  }

  public void testCallableSync() {
    assertThat(JRoutineSparseChannelsCompat.channelHandlerOn(immediateExecutor())
                                           .channelOf(new Callable<String>() {

                                             public String call() throws InterruptedException {
                                               seconds(.3).sleepAtLeast();
                                               return "test";
                                             }
                                           })
                                           .next()).isEqualTo("test");
  }

  public void testConcatOutput() {

    Channel<?, String> channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    Channel<?, String> channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannelsCompat.channelHandler()
                                           .concatOutputOf(channel2, channel1)
                                           .in(seconds(1))
                                           .all()).containsExactly("test4", "test5", "test6",
        "test1", "test2", "test3");
    channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineSparseChannelsCompat.channelHandler()
                                           .concatOutputOf(
                                               Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                           .in(seconds(1))
                                           .all()).containsExactly("test1", "test2", "test3",
        "test4", "test5", "test6");
  }

  public void testConcatOutputAbort() {

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
             .pass(JRoutineSparseChannelsCompat.channelHandler().concatOutputOf(channel1, channel2))
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
             .pass(JRoutineSparseChannelsCompat.channelHandler()
                                               .concatOutputOf(
                                                   Arrays.<Channel<?, ?>>asList(channel1,
                                                       channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testConcatOutputError() {

    try {

      JRoutineSparseChannelsCompat.channelHandler().concatOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().concatOutputOf((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().concatOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .concatOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().concatOutputOf((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
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
    JRoutineSparseChannelsCompat.channelHandler()
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
    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    final TestLog testLog = new TestLog();
    JRoutineSparseChannelsCompat.channelHandler()
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
      new JRoutineSparseChannelsCompat();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  public void testDuplicateInput() {
    final List<Channel<String, String>> outputChannels =
        JRoutineSparseChannelsCompat.channelHandler().channels(3);
    JRoutineSparseChannelsCompat.channelHandler().duplicateInputOf(outputChannels).pass("test");
    for (final Channel<?, String> outputChannel : outputChannels) {
      assertThat(outputChannel.in(seconds(3)).next()).isEqualTo("test");
    }

    final Channel<String, String> channel1 = JRoutineCore.channel().ofType();
    final Channel<String, String> channel2 = JRoutineCore.channel().ofType();
    JRoutineSparseChannelsCompat.channelHandler().duplicateInputOf(channel1, channel2).pass("test");
    assertThat(channel1.in(seconds(3)).next()).isEqualTo("test");
    assertThat(channel2.in(seconds(3)).next()).isEqualTo("test");
  }

  public void testDuplicateOutput() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    final List<Channel<?, String>> outputChannels =
        JRoutineSparseChannelsCompat.channelHandler().duplicateOutputOf(channel, 3);
    channel.pass("test");
    for (final Channel<?, String> outputChannel : outputChannels) {
      assertThat(outputChannel.in(seconds(3)).next()).isEqualTo("test");
    }
  }

  @SuppressWarnings("unchecked")
  public void testInputFlow() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannelsCompat.channelHandler()
                                .inputFlowOf(channel, 33)
                                .pass(new FlowData<String>(33, "test1"),
                                    new FlowData<String>(-33, "test2"),
                                    new FlowData<String>(33, "test3"),
                                    new FlowData<String>(333, "test4"))
                                .close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  public void testInputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannelsCompat.channelHandler().inputFlowOf(channel, 33).abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testInputMap() {

    final ArrayList<FlowData<Object>> outputs = new ArrayList<FlowData<Object>>();
    outputs.add(new FlowData<Object>(Sort.STRING, "test21"));
    outputs.add(new FlowData<Object>(Sort.INTEGER, -11));
    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<FlowData<Object>, FlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfFlow(channel,
                                                 Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                                 channel);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  public void testInputMapAbort() {

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<FlowData<Object>, FlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
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
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
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

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .of(factoryOf(Sort.class));
    SparseArrayCompat<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlowData<Object>, ParcelableFlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
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
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfParcelableFlow(channel, Sort.INTEGER,
                                                 Sort.STRING);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfParcelableFlow(
                                                 Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testInputMapError() {

    try {
      JRoutineSparseChannelsCompat.channelHandler()
                                  .inputOfFlow(0, 0,
                                      JRoutineCore.routine().of(new SortFlow()).invoke());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testInputMapErrorParcelable() {

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfParcelableFlow(0, 0, JRoutineLoaderCompat.routineOn(
                                      LoaderSourceCompat.loaderOf(getActivity()))
                                                                                    .of(factoryOf(
                                                                                        Sort.class))
                                                                                    .invoke());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testInputMapParcelable() {

    final ArrayList<ParcelableFlowData<Object>> outputs =
        new ArrayList<ParcelableFlowData<Object>>();
    outputs.add(new ParcelableFlowData<Object>(Sort.STRING, "test21"));
    outputs.add(new ParcelableFlowData<Object>(Sort.INTEGER, -11));
    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .of(factoryOf(Sort.class));
    SparseArrayCompat<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlowData<Object>, ParcelableFlowData<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfParcelableFlow(channel,
                                                 Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfParcelableFlow(channel, Sort.INTEGER,
                                                 Sort.STRING);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .inputOfParcelableFlow(
                                                 Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  @SuppressWarnings("unchecked")
  public void testInputSelect() {

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannelsCompat.channelHandler()
                                .inputOfFlow(channel, 33)
                                .pass("test1", "test2", "test3")
                                .close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly(
        new FlowData<String>(33, "test1"), new FlowData<String>(33, "test2"),
        new FlowData<String>(33, "test3"));
  }

  public void testInputSelectAbort() {

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    JRoutineSparseChannelsCompat.channelHandler()
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

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();

    try {
      JRoutineSparseChannelsCompat.channelHandler().inputOfFlow(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannelsCompat.channelHandler().inputOfFlow(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannelsCompat.channelHandler()
                                  .inputOfFlow(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoin() {

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineSparseChannelsCompat.channelHandler()
                                                        .joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineSparseChannelsCompat.channelHandler()
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
                      .pass(JRoutineSparseChannelsCompat.channelHandler()
                                                        .joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
  }

  public void testJoinAbort() {

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
             .pass(JRoutineSparseChannelsCompat.channelHandler().joinOutputOf(channel1, channel2))
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
             .pass(JRoutineSparseChannelsCompat.channelHandler()
                                               .joinOutputOf(Arrays.<Channel<?, ?>>asList(channel1,
                                                   channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinBackoff() {

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.after(millis(100)).pass("test").pass("test").close();
    try {
      routine.invoke()
             .pass(JRoutineSparseChannelsCompat.channelHandler()
                                               .withChannel()
                                               .withBackoff(
                                                   afterCount(0).constantDelay(millis(100)))
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

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoinInput() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf(channel1, channel2)
                                .pass(Arrays.asList("test1-1", "test1-2"))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                .pass(Arrays.asList("test2-1", "test2-2"))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf(channel1, channel2)
                                .pass(Collections.singletonList("test3-1"))
                                .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly("test1-2", "test2-2");
  }

  public void testJoinInputAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler().joinInputOf(channel1, channel2).abort();

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
    JRoutineSparseChannelsCompat.channelHandler()
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

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf(channel1)
                                .pass(Arrays.asList("test1-1", "test1-2"))
                                .close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinInputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinInputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoinInputPlaceHolderError() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf((Object) null, channel1)
                                .pass(Arrays.asList("test1-1", "test1-2"))
                                .close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinInputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinInputOf(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinInputOf(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinInputOf(new Object(),
                                      Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testJoinInputPlaceholder() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf((Object) null, channel1, channel2)
                                .pass(Arrays.asList("test1-1", "test1-2"))
                                .close();
    final String placeholder = "placeholder";
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf((Object) placeholder,
                                    Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                .pass(Arrays.asList("test2-1", "test2-2"))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf(placeholder, channel1, channel2)
                                .pass(Collections.singletonList("test3-1"))
                                .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly("test1-2", "test2-2",
        placeholder);
  }

  public void testJoinInputPlaceholderAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler()
                                .joinInputOf((Object) null, channel1, channel2)
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
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler()
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

    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine = JRoutineCore.routine().of(new CharAt());
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineSparseChannelsCompat.channelHandler()
                                                        .joinOutputOf(new Object(), channel1,
                                                            channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineSparseChannelsCompat.channelHandler()
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
             .pass(JRoutineSparseChannelsCompat.channelHandler()
                                               .joinOutputOf(new Object(), channel1, channel2))
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final InvocationException ignored) {

    }
  }

  public void testJoinPlaceholderAbort() {

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
             .pass(JRoutineSparseChannelsCompat.channelHandler()
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
             .pass(JRoutineSparseChannelsCompat.channelHandler()
                                               .joinOutputOf(new Object(),
                                                   Arrays.<Channel<?, ?>>asList(channel1,
                                                       channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinPlaceholderError() {

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinOutputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinOutputOf(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().joinOutputOf(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .joinOutputOf(new Object(),
                                      Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testList() {
    final int count = 7;
    final List<? extends Channel<Object, Object>> channels =
        JRoutineSparseChannelsCompat.channelHandlerOn(immediateExecutor()).channels(count);
    for (final Channel<Object, Object> channel : channels) {
      assertThat(channel.pass("test").next()).isEqualTo("test");
    }
  }

  public void testListEmpty() {
    assertThat(JRoutineSparseChannelsCompat.channelHandler().channels(0)).isEmpty();
  }

  public void testListError() {
    JRoutineSparseChannelsCompat.channelHandler().channels(-1);
  }

  public void testMap() {

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();

    final Channel<?, ? extends FlowData<Object>> channel =
        JRoutineSparseChannelsCompat.channelHandler()
                                    .mergeOutputOf(
                                        Arrays.<Channel<?, ?>>asList(channel1, channel2));
    final Channel<?, FlowData<Object>> output = JRoutineCore.routine()
                                                            .withInvocation()
                                                            .withInputOrder(OrderType.SORTED)
                                                            .configuration()
                                                            .of(new SortFlow())
                                                            .invoke()
                                                            .pass(channel)
                                                            .close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannelsCompat.channelHandler()
                                    .outputOfFlow(output, Sort.INTEGER, Sort.STRING);

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

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();

    final Channel<?, ? extends ParcelableFlowData<Object>> channel =
        JRoutineSparseChannelsCompat.channelHandler()
                                    .mergeParcelableOutputOf(
                                        Arrays.<Channel<?, ?>>asList(channel1, channel2));
    final Channel<?, ParcelableFlowData<Object>> output =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .withInvocation()
                            .withInputOrder(OrderType.SORTED)
                            .configuration()
                            .of(factoryOf(Sort.class))
                            .invoke()
                            .pass(channel)
                            .close();
    final SparseArrayCompat<? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannelsCompat.channelHandler()
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

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends FlowData<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.pass(13).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(-7, "test1"),
        new FlowData<Integer>(-6, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannelsCompat.channelHandler()
                                                .mergeOutputOf(11,
                                                    Arrays.<Channel<?, ?>>asList(channel1,
                                                        channel2));
    channel2.pass(13).close();
    channel1.pass("test1").close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(11, "test1"),
        new FlowData<Integer>(12, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(channel1, channel2);
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(0, "test2"),
        new FlowData<Integer>(1, -17));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannelsCompat.channelHandler()
                                                .mergeOutputOf(
                                                    Arrays.<Channel<?, ?>>asList(channel1,
                                                        channel2));
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(0, "test2"),
        new FlowData<Integer>(1, -17));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(channelMap);
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new FlowData<String>(7, "test3"),
        new FlowData<Integer>(-3, 111));
  }

  @SuppressWarnings("unchecked")
  public void testMerge4() {

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<String, String> channel2 = builder.ofType();
    final Channel<String, String> channel3 = builder.ofType();
    final Channel<String, String> channel4 = builder.ofType();

    final Routine<FlowData<String>, String> routine =
        JRoutineCore.routine().of(InvocationFactory.factoryOf(new ClassToken<Amb<String>>() {}));
    final Channel<?, String> outputChannel = routine.invoke()
                                                    .pass(
                                                        JRoutineSparseChannelsCompat
                                                            .channelHandler()
                                                                                    .mergeOutputOf(
                                                                                        Arrays
                                                                                            .asList(
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

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends FlowData<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannelsCompat.channelHandler()
                                                .mergeOutputOf(11,
                                                    Arrays.<Channel<?, ?>>asList(channel1,
                                                        channel2));
    channel2.abort();
    channel1.pass("test1").close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(channel1, channel2);
    channel1.abort();
    channel2.pass(-17).close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineSparseChannelsCompat.channelHandler()
                                                .mergeOutputOf(
                                                    Arrays.<Channel<?, ?>>asList(channel1,
                                                        channel2));
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
    outputChannel = JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(channelMap);
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

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();
    final SparseArrayCompat<Channel<?, ?>> channelMap = new SparseArrayCompat<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlowData<?>> outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().mergeParcelableOutputOf(channelMap);
    channel1.abort();
    channel2.pass(111).close();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testMergeError() {

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeOutputOf(0, Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeOutputOf(Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeOutputOf(
                                      Collections.<Integer, Channel<?, Object>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeOutputOf(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeOutputOf(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeOutputOf(
                                      Collections.<Integer, Channel<?, ?>>singletonMap(1, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testMergeErrorParcelable() {

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeParcelableOutputOf(new SparseArrayCompat<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMergeInput() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(channel1, channel2)
                                .pass(new FlowData<String>(0, "test1"))
                                .pass(new FlowData<Integer>(1, 1))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(3, channel1, channel2)
                                .pass(new FlowData<String>(3, "test2"))
                                .pass(new FlowData<Integer>(4, 2))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                .pass(new FlowData<String>(0, "test3"))
                                .pass(new FlowData<Integer>(1, 3))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                .pass(new FlowData<String>(-5, "test4"))
                                .pass(new FlowData<Integer>(-4, 4))
                                .close();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(map)
                                .pass(new FlowData<String>(31, "test5"))
                                .pass(new FlowData<Integer>(17, 5))
                                .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  public void testMergeInputAbort() {

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(channel1, channel2).abort();

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
    JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(3, channel1, channel2).abort();

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
    JRoutineSparseChannelsCompat.channelHandler()
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
    JRoutineSparseChannelsCompat.channelHandler()
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
    JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(map).abort();

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

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingString.class))
                                   .invoke()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingInteger.class))
                                   .invoke()
                                   .sorted();
    JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(channel1, channel2).abort();

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

    channel1 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingString.class))
                                   .invoke()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingInteger.class))
                                   .invoke()
                                   .sorted();
    JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(3, channel1, channel2).abort();

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

    channel1 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingString.class))
                                   .invoke()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingInteger.class))
                                   .invoke()
                                   .sorted();
    JRoutineSparseChannelsCompat.channelHandler()
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

    channel1 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingString.class))
                                   .invoke()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingInteger.class))
                                   .invoke()
                                   .sorted();
    JRoutineSparseChannelsCompat.channelHandler()
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

    channel1 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingString.class))
                                   .invoke()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                                   .of(factoryOf(PassingInteger.class))
                                   .invoke()
                                   .sorted();
    final SparseArrayCompat<Channel<?, ?>> map = new SparseArrayCompat<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineSparseChannelsCompat.channelHandler().mergeParcelableInputOf(map).abort();

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

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(Collections.<Integer, Channel<?, ?>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(
                                      Collections.<Integer, Channel<?, ?>>singletonMap(0, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testMergeInputErrorParcelable() {

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler().mergeInputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeInputOf(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .mergeParcelableInputOf(new SparseArrayCompat<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMergeInputParcelable() {

    final Channel<String, String> channel1 =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .of(factoryOf(PassingString.class))
                            .invoke()
                            .sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .of(factoryOf(PassingInteger.class))
                            .invoke()
                            .sorted();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(channel1, channel2)
                                .pass(new ParcelableFlowData<String>(0, "test1"))
                                .pass(new ParcelableFlowData<Integer>(1, 1))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(3, channel1, channel2)
                                .pass(new ParcelableFlowData<String>(3, "test2"))
                                .pass(new ParcelableFlowData<Integer>(4, 2))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                .pass(new ParcelableFlowData<String>(0, "test3"))
                                .pass(new ParcelableFlowData<Integer>(1, 3))
                                .close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                .pass(new ParcelableFlowData<String>(-5, "test4"))
                                .pass(new ParcelableFlowData<Integer>(-4, 4))
                                .close();
    final SparseArrayCompat<Channel<?, ?>> map = new SparseArrayCompat<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineSparseChannelsCompat.channelHandler()
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

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();
    final SparseArrayCompat<Channel<?, ?>> channelMap = new SparseArrayCompat<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlowData<?>> outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().mergeParcelableOutputOf(channelMap);
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlowData<String>(7, "test3"), new ParcelableFlowData<Integer>(-3, 111));
  }

  @SuppressWarnings("unchecked")
  public void testOutputFlow() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").close();
    assertThat(JRoutineSparseChannelsCompat.channelHandler()
                                           .outputFlowOf(channel, 33)
                                           .in(seconds(1))
                                           .all()).containsExactly(
        new FlowData<String>(33, "test1"), new FlowData<String>(33, "test2"),
        new FlowData<String>(33, "test3"));
  }

  public void testOutputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").abort();

    try {

      JRoutineSparseChannelsCompat.channelHandler().outputFlowOf(channel, 33).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routineOn(syncExecutor()).of(new SortFlow());
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, FlowData<Object>> channel;
    channel = routine.invoke()
                     .pass(new FlowData<Object>(Sort.STRING, "test21"),
                         new FlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfFlow(channel,
                                                 Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new FlowData<Object>(Sort.INTEGER, -11),
                         new FlowData<Object>(Sort.STRING, "test21"))
                     .close();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new FlowData<Object>(Sort.STRING, "test21"),
                         new FlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                                 channel);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, FlowData<Object>> channel;
    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new FlowData<Object>(Sort.STRING, "test21"),
                         new FlowData<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
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
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
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
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
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

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .of(factoryOf(Sort.class));
    SparseArrayCompat<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlowData<Object>> channel;
    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
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
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfParcelableFlow(channel, Sort.INTEGER,
                                                 Sort.STRING);
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
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfParcelableFlow(
                                                 Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
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

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(InvocationFactory.factoryOf(SortFlow.class));
    final Channel<?, FlowData<Object>> channel = routine.invoke();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannelsCompat.channelHandler()
                                    .outputOfFlow(channel,
                                        Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap).isEqualTo(JRoutineSparseChannelsCompat.channelHandler()
                                                                 .outputOfFlow(channel,
                                                                     Sort.INTEGER, Sort.STRING));
  }

  public void testOutputMapCacheParcelable() {

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .of(factoryOf(Sort.class));
    final Channel<?, ParcelableFlowData<Object>> channel = routine.invoke();
    final SparseArrayCompat<? extends Channel<?, Object>> channelMap =
        JRoutineSparseChannelsCompat.channelHandler()
                                    .outputOfParcelableFlow(channel,
                                        Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(toMap(channelMap)).isEqualTo(toMap(JRoutineSparseChannelsCompat.channelHandler()
                                                                              .outputOfParcelableFlow(
                                                                                  channel,
                                                                                  Sort.INTEGER,
                                                                                  Sort.STRING)));
  }

  public void testOutputMapError() {

    try {
      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfFlow(0, 0,
                                      JRoutineCore.routine().of(new SortFlow()).invoke());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testOutputMapErrorParcelable() {

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfParcelableFlow(0, 0, JRoutineLoaderCompat.routineOn(
                                      LoaderSourceCompat.loaderOf(getActivity()))
                                                                                    .of(factoryOf(
                                                                                        Sort.class))
                                                                                    .invoke());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapParcelable() {

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineLoaderCompat.routineOn(LoaderSourceCompat.loaderOf(getActivity()))
                            .of(factoryOf(Sort.class));
    SparseArrayCompat<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlowData<Object>> channel;
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfParcelableFlow(channel,
                                                 Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.INTEGER, -11),
                         new ParcelableFlowData<Object>(Sort.STRING, "test21"))
                     .close();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfParcelableFlow(channel, Sort.INTEGER,
                                                 Sort.STRING);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineSparseChannelsCompat.channelHandler()
                                             .outputOfParcelableFlow(
                                                 Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputSelect() {

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().outputOfFlow(channel, 33).get(33);
    channel.pass(new FlowData<String>(33, "test1"), new FlowData<String>(-33, "test2"),
        new FlowData<String>(33, "test3"), new FlowData<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  public void testOutputSelectAbort() {

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().outputOfFlow(channel, 33).get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testOutputSelectAbortParcelable() {

    final Channel<ParcelableFlowData<String>, ParcelableFlowData<String>> channel =
        JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().outputOfParcelableFlow(channel, 33).get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testOutputSelectError() {

    final Channel<FlowData<String>, FlowData<String>> channel = JRoutineCore.channel().ofType();

    try {
      JRoutineSparseChannelsCompat.channelHandler().outputOfFlow(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannelsCompat.channelHandler().outputOfFlow(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfFlow(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputSelectParcelable() {

    final Channel<ParcelableFlowData<String>, ParcelableFlowData<String>> channel =
        JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineSparseChannelsCompat.channelHandler().outputOfParcelableFlow(channel, 33).get(33);
    channel.pass(new ParcelableFlowData<String>(33, "test1"),
        new ParcelableFlowData<String>(-33, "test2"), new ParcelableFlowData<String>(33, "test3"),
        new ParcelableFlowData<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly("test1", "test3");
  }

  @SuppressWarnings("ConstantConditions")
  public void testReplayError() {

    try {
      JRoutineSparseChannelsCompat.channelHandler().replayOutputOf(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testSelectMap() {

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    final Channel<FlowData<Object>, FlowData<Object>> inputChannel =
        JRoutineCore.channel().ofType();
    final Channel<?, FlowData<Object>> outputChannel = routine.invoke().pass(inputChannel).close();
    final Channel<?, Object> intChannel = JRoutineSparseChannelsCompat.channelHandler()
                                                                      .withChannel()
                                                                      .withLogLevel(Level.WARNING)
                                                                      .configuration()
                                                                      .outputOfFlow(outputChannel,
                                                                          Sort.INTEGER, Sort.STRING)
                                                                      .get(Sort.INTEGER);
    final Channel<?, Object> strChannel = JRoutineSparseChannelsCompat.channelHandler()
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

    final Routine<FlowData<Object>, FlowData<Object>> routine =
        JRoutineCore.routine().of(new SortFlow());
    Channel<FlowData<Object>, FlowData<Object>> inputChannel = JRoutineCore.channel().ofType();
    Channel<?, FlowData<Object>> outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING);
    inputChannel.after(millis(100))
                .pass(new FlowData<Object>(Sort.STRING, "test21"),
                    new FlowData<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                                  .get(Sort.STRING)
                                  .in(seconds(1))
                                  .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
                                  .get(Sort.INTEGER)
                                  .in(seconds(1))
                                  .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.channel().ofType();
    outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING);
    inputChannel.after(millis(100))
                .pass(new FlowData<Object>(Sort.INTEGER, -11),
                    new FlowData<Object>(Sort.STRING, "test21"))
                .abort();

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                                  .get(Sort.STRING)
                                  .in(seconds(1))
                                  .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                                  .get(Sort.INTEGER)
                                  .in(seconds(1))
                                  .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.channel().ofType();
    outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineSparseChannelsCompat.channelHandler()
                                .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER);
    inputChannel.after(millis(100))
                .pass(new FlowData<Object>(Sort.STRING, "test21"),
                    new FlowData<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      JRoutineSparseChannelsCompat.channelHandler()
                                  .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
                                  .get(Sort.STRING)
                                  .in(seconds(1))
                                  .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineSparseChannelsCompat.channelHandler()
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
          JRoutineSparseChannelsCompat.channelHandler().<Object, Integer>inputOfParcelableFlow(
              result, INTEGER).pass((Integer) flow.data).close();
          break;

        case STRING:
          JRoutineSparseChannelsCompat.channelHandler().<Object, String>inputOfParcelableFlow(
              result, STRING).pass((String) flow.data).close();
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
          JRoutineSparseChannelsCompat.channelHandler().<Object, Integer>inputOfFlow(result,
              INTEGER).pass(flowData.<Integer>data()).close();
          break;

        case STRING:
          JRoutineSparseChannelsCompat.channelHandler().<Object, String>inputOfFlow(result,
              STRING).pass(flowData.<String>data()).close();
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
