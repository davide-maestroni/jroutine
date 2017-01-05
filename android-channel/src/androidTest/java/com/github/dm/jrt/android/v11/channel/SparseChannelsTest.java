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

import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
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
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 08/03/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class SparseChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public SparseChannelsTest() {

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

  public void testCombine() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Channel<String, String> channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                           .with(factoryOf(PassingString.class))
                                                           .call()
                                                           .sorted();
    final Channel<Integer, Integer> channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                             .with(factoryOf(PassingInteger.class))
                                                             .call()
                                                             .sorted();
    SparseChannels.combine(channel1, channel2)
                  .buildChannel()
                  .pass(new ParcelableFlow<String>(0, "test1"))
                  .pass(new ParcelableFlow<Integer>(1, 1))
                  .close();
    SparseChannels.combine(3, channel1, channel2)
                  .buildChannel()
                  .pass(new ParcelableFlow<String>(3, "test2"))
                  .pass(new ParcelableFlow<Integer>(4, 2))
                  .close();
    SparseChannels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                  .buildChannel()
                  .pass(new ParcelableFlow<String>(0, "test3"))
                  .pass(new ParcelableFlow<Integer>(1, 3))
                  .close();
    SparseChannels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                  .buildChannel()
                  .pass(new ParcelableFlow<String>(-5, "test4"))
                  .pass(new ParcelableFlow<Integer>(-4, 4))
                  .close();
    final SparseArray<Channel<?, ?>> map = new SparseArray<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    SparseChannels.combine(map)
                  .buildChannel()
                  .pass(new ParcelableFlow<String>(31, "test5"))
                  .pass(new ParcelableFlow<Integer>(17, 5))
                  .close();
    assertThat(channel1.close().in(seconds(10)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().in(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  public void testCombineAbort() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingString.class))
                             .call()
                             .sorted();
    channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingInteger.class))
                             .call()
                             .sorted();
    SparseChannels.combine(channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingString.class))
                             .call()
                             .sorted();
    channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingInteger.class))
                             .call()
                             .sorted();
    SparseChannels.combine(3, channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingString.class))
                             .call()
                             .sorted();
    channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingInteger.class))
                             .call()
                             .sorted();
    SparseChannels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel().abort();

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

    channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingString.class))
                             .call()
                             .sorted();
    channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingInteger.class))
                             .call()
                             .sorted();
    SparseChannels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
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

    channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingString.class))
                             .call()
                             .sorted();
    channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(factoryOf(PassingInteger.class))
                             .call()
                             .sorted();
    final SparseArray<Channel<?, ?>> map = new SparseArray<Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    SparseChannels.combine(map).buildChannel().abort();

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

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    try {

      SparseChannels.combine();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannels.combine(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannels.combine(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannels.combine(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      SparseChannels.combine(new SparseArray<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofInputs().buildChannel();
    final TestLog testLog = new TestLog();
    SparseChannels.parcelableFlowInput(3, 1, channel)
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
      new SparseChannels();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  public void testInputMap() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final ArrayList<ParcelableFlow<Object>> outputs = new ArrayList<ParcelableFlow<Object>>();
    outputs.add(new ParcelableFlow<Object>(Sort.STRING, "test21"));
    outputs.add(new ParcelableFlow<Object>(Sort.INTEGER, -11));
    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoader.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    SparseArray<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlow<Object>, ParcelableFlow<Object>> channel;
    channel = routine.call();
    channelMap =
        SparseChannels.parcelableFlowInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                      .buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap =
        SparseChannels.parcelableFlowInput(channel, Sort.INTEGER, Sort.STRING).buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap = SparseChannels.parcelableFlowInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                               .buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  public void testInputMapAbort() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoader.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    SparseArray<? extends Channel<Object, ?>> channelMap;
    Channel<ParcelableFlow<Object>, ParcelableFlow<Object>> channel;
    channel = routine.call();
    channelMap =
        SparseChannels.parcelableFlowInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                      .buildChannelArray();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap =
        SparseChannels.parcelableFlowInput(channel, Sort.INTEGER, Sort.STRING).buildChannelArray();
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap = SparseChannels.parcelableFlowInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
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

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    try {

      SparseChannels.parcelableFlowInput(0, 0,
          JRoutineLoader.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).call());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMap() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

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
        SparseChannels.mergeParcelable(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                      .buildChannel();
    final Channel<?, ParcelableFlow<Object>> output = JRoutineLoader.on(loaderFrom(getActivity()))
                                                                    .with(factoryOf(Sort.class))
                                                                    .applyInvocationConfiguration()
                                                                    .withInputOrder(
                                                                        OrderType.SORTED)
                                                                    .configured()
                                                                    .call(channel);
    final SparseArray<? extends Channel<?, Object>> channelMap =
        SparseChannels.parcelableFlowOutput(output, Sort.INTEGER, Sort.STRING).buildChannelArray();

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

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

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
    final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlow<?>> outputChannel =
        SparseChannels.mergeParcelable(channelMap).buildChannel();
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlow<String>(7, "test3"), new ParcelableFlow<Integer>(-3, 111));
  }

  @SuppressWarnings("unchecked")
  public void testMergeAbort() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

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
    final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    final Channel<?, ? extends ParcelableFlow<?>> outputChannel =
        SparseChannels.mergeParcelable(channelMap).buildChannel();
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

      SparseChannels.mergeParcelable(new SparseArray<Channel<?, ?>>(0));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoader.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    SparseArray<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlow<Object>> channel;
    channel = routine.call(new ParcelableFlow<Object>(Sort.STRING, "test21"),
        new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap =
        SparseChannels.parcelableFlowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                      .buildChannelArray();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.call(new ParcelableFlow<Object>(Sort.INTEGER, -11),
        new ParcelableFlow<Object>(Sort.STRING, "test21"));
    channelMap =
        SparseChannels.parcelableFlowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelArray();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.call(new ParcelableFlow<Object>(Sort.STRING, "test21"),
        new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap =
        SparseChannels.parcelableFlowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                      .buildChannelArray();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoader.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    SparseArray<? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlow<Object>> channel;
    channel = routine.call()
                     .after(millis(100))
                     .pass(new ParcelableFlow<Object>(Sort.STRING, "test21"),
                         new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap =
        SparseChannels.parcelableFlowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
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
    channelMap =
        SparseChannels.parcelableFlowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelArray();
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
        SparseChannels.parcelableFlowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
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

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineLoader.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    final Channel<?, ParcelableFlow<Object>> channel = routine.call();
    final SparseArray<? extends Channel<?, Object>> channelMap =
        SparseChannels.parcelableFlowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                      .buildChannelArray();
    assertThat(toMap(channelMap)).isEqualTo(toMap(
        SparseChannels.parcelableFlowOutput(channel, Sort.INTEGER, Sort.STRING)
                      .buildChannelArray()));
  }

  public void testOutputMapError() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    try {

      SparseChannels.parcelableFlowOutput(0, 0,
          JRoutineLoader.on(loaderFrom(getActivity())).with(factoryOf(Sort.class)).call());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputSelect() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        SparseChannels.parcelableFlowOutput(channel, 33).buildChannelArray().get(33);
    channel.pass(new ParcelableFlow<String>(33, "test1"), new ParcelableFlow<String>(-33, "test2"),
        new ParcelableFlow<String>(33, "test3"), new ParcelableFlow<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly("test1", "test3");
  }

  public void testOutputSelectAbort() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

      return;
    }

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        SparseChannels.parcelableFlowOutput(channel, 33).buildChannelArray().get(33);
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
          SparseChannels.<Object, Integer>parcelableFlowInput(result, INTEGER).buildChannel()
                                                                              .pass(
                                                                                  (Integer) flow
                                                                                      .data)
                                                                              .close();
          break;

        case STRING:
          SparseChannels.<Object, String>parcelableFlowInput(result, STRING).buildChannel()
                                                                            .pass(
                                                                                (String) flow.data)
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
