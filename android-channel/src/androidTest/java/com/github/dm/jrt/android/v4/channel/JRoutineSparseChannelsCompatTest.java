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
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
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

  public void testConstructor() {

    boolean failed = false;
    try {
      new JRoutineSparseChannelsCompat();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  public void testInputMap() {

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

  public void testInputMapAbort() {

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
                                  .outputOfParcelableFlow(0, 0, JRoutineLoaderCompat.routineOn(
                                      LoaderSourceCompat.loaderOf(getActivity()))
                                                                                    .of(factoryOf(
                                                                                        Sort.class))
                                                                                    .invoke());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMap() {

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

  @SuppressWarnings("unchecked")
  public void testMerge() {

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
  public void testMergeAbort() {

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
                                  .mergeParcelableOutputOf(new SparseArrayCompat<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMergeInput() {

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

  public void testMergeInputAbort() {

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
                                  .mergeParcelableInputOf(new SparseArrayCompat<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMap() {

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
  public void testOutputMapAbort() {

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
  public void testOutputSelect() {

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

  public void testOutputSelectAbort() {

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
