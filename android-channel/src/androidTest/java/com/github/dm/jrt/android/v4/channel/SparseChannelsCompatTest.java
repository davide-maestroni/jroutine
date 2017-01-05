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

import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
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
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 08/03/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class SparseChannelsCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public SparseChannelsCompatTest() {

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

  public void testCombine() {

    final Channel<String, String> channel1 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                                                 .with(
                                                                     factoryOf(PassingString.class))
                                                                 .call()
                                                                 .sorted();
    final Channel<Integer, Integer> channel2 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                                                   .with(factoryOf(
                                                                       PassingInteger.class))
                                                                   .call()
                                                                   .sorted();
    SparseChannelsCompat.combine(channel1, channel2)
                        .buildChannel()
                        .pass(new ParcelableFlow<String>(0, "test1"))
                        .pass(new ParcelableFlow<Integer>(1, 1))
                        .close();
    SparseChannelsCompat.combine(3, channel1, channel2)
                        .buildChannel()
                        .pass(new ParcelableFlow<String>(3, "test2"))
                        .pass(new ParcelableFlow<Integer>(4, 2))
                        .close();
    SparseChannelsCompat.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                        .buildChannel()
                        .pass(new ParcelableFlow<String>(0, "test3"))
                        .pass(new ParcelableFlow<Integer>(1, 3))
                        .close();
    SparseChannelsCompat.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                        .buildChannel()
                        .pass(new ParcelableFlow<String>(-5, "test4"))
                        .pass(new ParcelableFlow<Integer>(-4, 4))
                        .close();
    final SparseArrayCompat<Channel<?, ?>> map = new SparseArrayCompat<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    SparseChannelsCompat.combine(map)
                        .buildChannel()
                        .pass(new ParcelableFlow<String>(31, "test5"))
                        .pass(new ParcelableFlow<Integer>(17, 5))
                        .close();
    assertThat(channel1.close().in(seconds(10)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().in(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  public void testCombineAbort() {

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingString.class))
                                   .call()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingInteger.class))
                                   .call()
                                   .sorted();
    SparseChannelsCompat.combine(channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingString.class))
                                   .call()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingInteger.class))
                                   .call()
                                   .sorted();
    SparseChannelsCompat.combine(3, channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingString.class))
                                   .call()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingInteger.class))
                                   .call()
                                   .sorted();
    SparseChannelsCompat.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                        .buildChannel()
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

    channel1 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingString.class))
                                   .call()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingInteger.class))
                                   .call()
                                   .sorted();
    SparseChannelsCompat.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                        .buildChannel()
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

    channel1 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingString.class))
                                   .call()
                                   .sorted();
    channel2 = JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                                   .with(factoryOf(PassingInteger.class))
                                   .call()
                                   .sorted();
    final SparseArrayCompat<Channel<?, ?>> map = new SparseArrayCompat<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    SparseChannelsCompat.combine(map).buildChannel().abort();

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

  public void testCombineError() {

    try {

      SparseChannelsCompat.combine();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannelsCompat.combine(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannelsCompat.combine(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannelsCompat.combine(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannelsCompat.combine(new SparseArrayCompat<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofInputs().buildChannel();
    final TestLog testLog = new TestLog();
    SparseChannelsCompat.parcelableFlowInput(3, 1, channel)
                        .applyChannelConfiguration()
                        .withLog(testLog)
                        .withLogLevel(Level.DEBUG)
                        .configured()
                        .buildChannelArray()
                        .get(3)
                        .pass("test")
                        .close();
    assertThat(channel.close().all()).containsExactly(new ParcelableFlow<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  public void testConstructor() {

    boolean failed = false;
    try {
      new SparseChannelsCompat();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  public void testInputMap() {

    final ArrayList<ParcelableFlow<Object>> outputs = new ArrayList<ParcelableFlow<Object>>();
    outputs.add(new ParcelableFlow<Object>(Sort.STRING, "test21"));
    outputs.add(new ParcelableFlow<Object>(Sort.INTEGER, -11));
    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                            .with(factoryOf(Sort.class))
                            .buildRoutine();
    SparseArrayCompat<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlow<Object>, ParcelableFlow<Object>> channel;
    channel = routine.call();
    channelMap =
        SparseChannelsCompat.parcelableFlowInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                            .buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap = SparseChannelsCompat.parcelableFlowInput(channel, Sort.INTEGER, Sort.STRING)
                                     .buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap =
        SparseChannelsCompat.parcelableFlowInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                            .buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  public void testInputMapAbort() {

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                            .with(factoryOf(Sort.class))
                            .buildRoutine();
    SparseArrayCompat<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlow<Object>, ParcelableFlow<Object>> channel;
    channel = routine.call();
    channelMap =
        SparseChannelsCompat.parcelableFlowInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                            .buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap = SparseChannelsCompat.parcelableFlowInput(channel, Sort.INTEGER, Sort.STRING)
                                     .buildChannelArray();
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap =
        SparseChannelsCompat.parcelableFlowInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                            .buildChannelArray();
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

      SparseChannelsCompat.parcelableFlowOutput(0, 0,
          JRoutineLoaderCompat.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).call());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMap() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().applyChannelConfiguration()
                                       .withOrder(OrderType.SORTED)
                                       .configured();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().applyChannelConfiguration()
                                        .withOrder(OrderType.SORTED)
                                        .configured();
    final Channel<String, String> channel1 = builder1.buildChannel();
    final Channel<Integer, Integer> channel2 = builder2.buildChannel();

    final Channel<?, ? extends ParcelableFlow<Object>> channel =
        SparseChannelsCompat.mergeParcelable(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                            .buildChannel();
    final Channel<?, ParcelableFlow<Object>> output =
        JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                            .with(factoryOf(Sort.class))
                            .applyInvocationConfiguration()
                            .withInputOrder(OrderType.SORTED)
                            .configured()
                            .call(channel);
    final SparseArrayCompat<? extends Channel<?, Object>> channelMap =
        SparseChannelsCompat.parcelableFlowOutput(output, Sort.INTEGER, Sort.STRING)
                            .buildChannelArray();

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

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().applyChannelConfiguration()
                                       .withOrder(OrderType.SORTED)
                                       .configured();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().applyChannelConfiguration()
                                        .withOrder(OrderType.SORTED)
                                        .configured();
    final Channel<String, String> channel1 = builder1.buildChannel();
    final Channel<Integer, Integer> channel2 = builder2.buildChannel();
    final SparseArrayCompat<Channel<?, ?>> channelMap = new SparseArrayCompat<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlow<?>> outputChannel =
        SparseChannelsCompat.mergeParcelable(channelMap).buildChannel();
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlow<String>(7, "test3"), new ParcelableFlow<Integer>(-3, 111));
  }

  @SuppressWarnings("unchecked")
  public void testMergeAbort() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().applyChannelConfiguration()
                                       .withOrder(OrderType.SORTED)
                                       .configured();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().applyChannelConfiguration()
                                        .withOrder(OrderType.SORTED)
                                        .configured();
    final Channel<String, String> channel1 = builder1.buildChannel();
    final Channel<Integer, Integer> channel2 = builder2.buildChannel();
    final SparseArrayCompat<Channel<?, ?>> channelMap = new SparseArrayCompat<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlow<?>> outputChannel =
        SparseChannelsCompat.mergeParcelable(channelMap).buildChannel();
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

      SparseChannelsCompat.mergeParcelable(new SparseArrayCompat<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                            .with(factoryOf(Sort.class))
                            .buildRoutine();
    SparseArrayCompat<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlow<Object>> channel;
    channel = routine.call(new ParcelableFlow<Object>(Sort.STRING, "test21"),
        new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap =
        SparseChannelsCompat.parcelableFlowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                            .buildChannelArray();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.call(new ParcelableFlow<Object>(Sort.INTEGER, -11),
        new ParcelableFlow<Object>(Sort.STRING, "test21"));
    channelMap = SparseChannelsCompat.parcelableFlowOutput(channel, Sort.INTEGER, Sort.STRING)
                                     .buildChannelArray();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.call(new ParcelableFlow<Object>(Sort.STRING, "test21"),
        new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap =
        SparseChannelsCompat.parcelableFlowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                            .buildChannelArray();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                            .with(factoryOf(Sort.class))
                            .buildRoutine();
    SparseArrayCompat<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlow<Object>> channel;
    channel = routine.call()
                     .after(millis(100))
                     .pass(new ParcelableFlow<Object>(Sort.STRING, "test21"),
                         new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap =
        SparseChannelsCompat.parcelableFlowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                            .buildChannelArray();
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

    channel = routine.call()
                     .after(millis(100))
                     .pass(new ParcelableFlow<Object>(Sort.INTEGER, -11),
                         new ParcelableFlow<Object>(Sort.STRING, "test21"));
    channelMap = SparseChannelsCompat.parcelableFlowOutput(channel, Sort.INTEGER, Sort.STRING)
                                     .buildChannelArray();
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

    channel = routine.call()
                     .after(millis(100))
                     .pass(new ParcelableFlow<Object>(Sort.STRING, "test21"),
                         new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap =
        SparseChannelsCompat.parcelableFlowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                            .buildChannelArray();
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

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoaderCompat.on(loaderFrom(getActivity()))
                            .with(factoryOf(Sort.class))
                            .buildRoutine();
    final Channel<?, ParcelableFlow<Object>> channel = routine.call();
    final SparseArrayCompat<? extends Channel<?, Object>> channelMap =
        SparseChannelsCompat.parcelableFlowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                            .buildChannelArray();
    assertThat(toMap(channelMap)).isEqualTo(toMap(
        SparseChannelsCompat.parcelableFlowOutput(channel, Sort.INTEGER, Sort.STRING)
                            .buildChannelArray()));
  }

  public void testOutputMapError() {

    try {

      SparseChannelsCompat.parcelableFlowOutput(0, 0,
          JRoutineLoaderCompat.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).call());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputSelect() {

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        SparseChannelsCompat.parcelableFlowOutput(channel, 33).buildChannelArray().get(33);
    channel.pass(new ParcelableFlow<String>(33, "test1"), new ParcelableFlow<String>(-33, "test2"),
        new ParcelableFlow<String>(33, "test3"), new ParcelableFlow<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly("test1", "test3");
  }

  public void testOutputSelectAbort() {

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        SparseChannelsCompat.parcelableFlowOutput(channel, 33).buildChannelArray().get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  private static class PassingInteger extends TemplateContextInvocation<Integer, Integer> {

    public void onInput(final Integer i, @NotNull final Channel<Integer, ?> result) {

      result.pass(i);
    }
  }

  private static class PassingString extends TemplateContextInvocation<String, String> {

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {

      result.pass(s);
    }
  }

  private static class Sort
      extends TemplateContextInvocation<ParcelableFlow<Object>, ParcelableFlow<Object>> {

    private static final int INTEGER = 1;

    private static final int STRING = 0;

    public void onInput(final ParcelableFlow<Object> flow,
        @NotNull final Channel<ParcelableFlow<Object>, ?> result) {

      switch (flow.id) {

        case INTEGER:
          SparseChannelsCompat.<Object, Integer>parcelableFlowInput(result, INTEGER).buildChannel()
                                                                                    .pass(
                                                                                        (Integer)
                                                                                            flow.data)
                                                                                    .close();
          break;

        case STRING:
          SparseChannelsCompat.<Object, String>parcelableFlowInput(result, STRING).buildChannel()
                                                                                  .pass(
                                                                                      (String)
                                                                                          flow.data)
                                                                                  .close();
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
