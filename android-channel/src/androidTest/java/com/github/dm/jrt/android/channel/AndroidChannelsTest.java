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

package com.github.dm.jrt.android.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceSource;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.android.core.ServiceSource.serviceFrom;
import static com.github.dm.jrt.android.core.invocation.InvocationFactoryReference.factoryOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 06/18/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class AndroidChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public AndroidChannelsTest() {

    super(TestActivity.class);
  }

  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<?, String> channel = JRoutineCore.of("test").buildChannel();
    final TestLog testLog = new TestLog();
    assertThat(AndroidChannels.outputParcelableFlow(channel, 3)
                              .withChannel()
                              .withLog(testLog)
                              .withLogLevel(Level.DEBUG)
                              .configuration()
                              .buildChannel()
                              .all()).containsExactly(new ParcelableFlow<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  public void testConstructor() {

    boolean failed = false;
    try {
      new AndroidChannels();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @SuppressWarnings("unchecked")
  public void testInputFlow() {

    final Channel<String, String> channel = JRoutineCore.<String>ofData().buildChannel();
    AndroidChannels.inputFlow(channel, 33)
                   .buildChannel()
                   .pass(new ParcelableFlow<String>(33, "test1"),
                       new ParcelableFlow<String>(-33, "test2"),
                       new ParcelableFlow<String>(33, "test3"),
                       new ParcelableFlow<String>(333, "test4"))
                   .close();
    channel.close();
    assertThat(channel.in(seconds(10)).all()).containsExactly("test1", "test3");
  }

  public void testInputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.<String>ofData().buildChannel();
    AndroidChannels.inputFlow(channel, 33).buildChannel().abort();
    channel.close();

    try {

      channel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testInputSelect() {

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofData().buildChannel();
    AndroidChannels.parcelableFlowInput(channel, 33)
                   .buildChannel()
                   .pass("test1", "test2", "test3")
                   .close();
    channel.close();
    assertThat(channel.in(seconds(10)).all()).containsExactly(
        new ParcelableFlow<String>(33, "test1"), new ParcelableFlow<String>(33, "test2"),
        new ParcelableFlow<String>(33, "test3"));
  }

  public void testInputSelectAbort() {

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofData().buildChannel();
    AndroidChannels.parcelableFlowInput(channel, 33)
                   .buildChannel()
                   .pass("test1", "test2", "test3")
                   .abort();
    channel.close();

    try {

      channel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoin() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofData();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofData();
    final Routine<List<?>, Character> routine =
        JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(CharAt.class)).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(AndroidChannels.joinOutput(channel1, channel2).buildChannel())
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(AndroidChannels.joinOutput(
                          Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(AndroidChannels.joinOutput(channel1, channel2).buildChannel())
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
  }

  public void testJoinAbort() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofData();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofData();
    final Routine<List<?>, Character> routine =
        JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(CharAt.class)).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(AndroidChannels.joinOutput(channel1, channel2).buildChannel())
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(AndroidChannels.joinOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                  .buildChannel())
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinError() {

    try {

      AndroidChannels.joinOutput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.joinOutput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testJoinInput() {

    final Channel<String, String> channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                            .with(factoryOf(PassingString.class))
                                                            .invoke()
                                                            .sorted();
    final Channel<String, String> channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                            .with(factoryOf(PassingString.class))
                                                            .invoke()
                                                            .sorted();
    AndroidChannels.joinInput(channel1, channel2)
                   .buildChannel()
                   .pass(Arrays.asList("test1-1", "test1-2"))
                   .close();
    AndroidChannels.joinInput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                   .buildChannel()
                   .pass(Arrays.asList("test2-1", "test2-2"))
                   .close();
    AndroidChannels.joinInput(channel1, channel2)
                   .buildChannel()
                   .pass(Collections.singletonList("test3-1"))
                   .close();
    assertThat(channel1.close().in(seconds(10)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(10)).all()).containsExactly("test1-2", "test2-2");
  }

  public void testJoinInputAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    AndroidChannels.joinInput(channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    AndroidChannels.joinInput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
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
  }

  public void testJoinInputError() {

    final Channel<String, String> channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                            .with(factoryOf(PassingString.class))
                                                            .invoke()
                                                            .sorted();
    AndroidChannels.joinInput(channel1)
                   .buildChannel()
                   .pass(Arrays.asList("test1-1", "test1-2"))
                   .close();

    try {

      channel1.close().in(seconds(10)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      AndroidChannels.joinInput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.joinInput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testJoinInputPlaceholder() {

    final Channel<String, String> channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                            .with(factoryOf(PassingString.class))
                                                            .invoke()
                                                            .sorted();
    final Channel<String, String> channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                            .with(factoryOf(PassingString.class))
                                                            .invoke()
                                                            .sorted();
    AndroidChannels.joinInput((Object) null, channel1, channel2)
                   .buildChannel()
                   .pass(Arrays.asList("test1-1", "test1-2"))
                   .close();
    final String placeholder = "placeholder";
    AndroidChannels.joinInput((Object) placeholder,
        Arrays.<Channel<?, ?>>asList(channel1, channel2))
                   .buildChannel()
                   .pass(Arrays.asList("test2-1", "test2-2"))
                   .close();
    AndroidChannels.joinInput(placeholder, channel1, channel2)
                   .buildChannel()
                   .pass(Collections.singletonList("test3-1"))
                   .close();
    assertThat(channel1.close().in(seconds(10)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(10)).all()).containsExactly("test1-2", "test2-2",
        placeholder);
  }

  public void testJoinInputPlaceholderAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    AndroidChannels.joinInput((Object) null, channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    AndroidChannels.joinInput(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
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
  }

  public void testJoinInputPlaceholderError() {

    final Channel<String, String> channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                            .with(factoryOf(PassingString.class))
                                                            .invoke()
                                                            .sorted();
    AndroidChannels.joinInput((Object) null, channel1)
                   .buildChannel()
                   .pass(Arrays.asList("test1-1", "test1-2"))
                   .close();

    try {

      channel1.close().in(seconds(10)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      AndroidChannels.joinInput(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.joinInput(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testJoinPlaceholder() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofData();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofData();
    final Routine<List<?>, Character> routine =
        JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(CharAt.class)).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(AndroidChannels.joinOutput(new Object(), channel1, channel2)
                                           .buildChannel())
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(AndroidChannels.joinOutput(null,
                          Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(AndroidChannels.joinOutput(new Object(), channel1, channel2).buildChannel())
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final InvocationException ignored) {

    }
  }

  public void testJoinPlaceholderAbort() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofData();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofData();
    final Routine<List<?>, Character> routine =
        JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(CharAt.class)).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(AndroidChannels.joinOutput((Object) null, channel1, channel2).buildChannel())
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(AndroidChannels.joinOutput(new Object(),
                 Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinPlaceholderError() {

    try {

      AndroidChannels.joinOutput(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.joinOutput(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMap() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofData().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofData().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final Channel<String, String> channel1 = builder1.buildChannel();
    final Channel<Integer, Integer> channel2 = builder2.buildChannel();

    final Channel<?, ? extends ParcelableFlow<Object>> channel =
        AndroidChannels.mergeParcelableOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannel();
    final Channel<?, ParcelableFlow<Object>> output = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                                     .with(factoryOf(Sort.class))
                                                                     .withInvocation()
                                                                     .withInputOrder(
                                                                         OrderType.SORTED)
                                                                     .configuration()
                                                                     .invoke()
                                                                     .pass(channel)
                                                                     .close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        AndroidChannels.flowOutput(output, Sort.INTEGER, Sort.STRING).buildChannelMap();

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
        JRoutineCore.<String>ofData().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofData().channelConfiguration().withOrder(OrderType.SORTED).apply();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends ParcelableFlow<?>> outputChannel;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = AndroidChannels.mergeParcelableOutput(-7, channel1, channel2).buildChannel();
    channel1.pass("test1").close();
    channel2.pass(13).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlow<String>(-7, "test1"), new ParcelableFlow<Integer>(-6, 13));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        AndroidChannels.mergeParcelableOutput(11, Arrays.asList(channel1, channel2)).buildChannel();
    channel2.pass(13).close();
    channel1.pass("test1").close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlow<String>(11, "test1"), new ParcelableFlow<Integer>(12, 13));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = AndroidChannels.mergeParcelableOutput(channel1, channel2).buildChannel();
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlow<String>(0, "test2"), new ParcelableFlow<Integer>(1, -17));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        AndroidChannels.mergeParcelableOutput(Arrays.asList(channel1, channel2)).buildChannel();
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlow<String>(0, "test2"), new ParcelableFlow<Integer>(1, -17));
  }

  @SuppressWarnings("unchecked")
  public void testMerge4() {

    final ChannelBuilder<String, String> builder =
        JRoutineCore.<String>ofData().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final Channel<String, String> channel1 = builder.buildChannel();
    final Channel<String, String> channel2 = builder.buildChannel();
    final Channel<String, String> channel3 = builder.buildChannel();
    final Channel<String, String> channel4 = builder.buildChannel();

    final Routine<ParcelableFlow<String>, String> routine =
        JRoutineCore.with(InvocationFactory.factoryOf(new ClassToken<Amb<String>>() {}))
                    .buildRoutine();
    final Channel<?, String> outputChannel = routine.invoke()
                                                    .pass(AndroidChannels.mergeParcelableOutput(
                                                        Arrays.asList(channel1, channel2, channel3,
                                                            channel4)).buildChannel())
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

  @SuppressWarnings("unchecked")
  public void testMergeAbort() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofData().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofData().channelConfiguration().withOrder(OrderType.SORTED).apply();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends ParcelableFlow<?>> outputChannel;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = AndroidChannels.mergeParcelableOutput(-7, channel1, channel2).buildChannel();
    channel1.pass("test1").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        AndroidChannels.mergeParcelableOutput(11, Arrays.asList(channel1, channel2)).buildChannel();
    channel2.abort();
    channel1.pass("test1").close();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = AndroidChannels.mergeParcelableOutput(channel1, channel2).buildChannel();
    channel1.abort();
    channel2.pass(-17).close();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        AndroidChannels.mergeParcelableOutput(Arrays.asList(channel1, channel2)).buildChannel();
    channel1.pass("test2").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testMergeError() {

    try {

      AndroidChannels.mergeParcelableOutput(0, Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.mergeParcelableOutput(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.mergeParcelableOutput(Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.mergeParcelableOutput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMergeInput() {

    final Channel<String, String> channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                            .with(factoryOf(PassingString.class))
                                                            .invoke()
                                                            .sorted();
    final Channel<Integer, Integer> channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                                                              .with(factoryOf(PassingInteger.class))
                                                              .invoke()
                                                              .sorted();
    AndroidChannels.mergeInput(channel1, channel2)
                   .buildChannel()
                   .pass(new ParcelableFlow<String>(0, "test1"))
                   .pass(new ParcelableFlow<Integer>(1, 1))
                   .close();
    AndroidChannels.mergeInput(3, channel1, channel2)
                   .buildChannel()
                   .pass(new ParcelableFlow<String>(3, "test2"))
                   .pass(new ParcelableFlow<Integer>(4, 2))
                   .close();
    AndroidChannels.mergeInput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                   .buildChannel()
                   .pass(new ParcelableFlow<String>(0, "test3"))
                   .pass(new ParcelableFlow<Integer>(1, 3))
                   .close();
    AndroidChannels.mergeInput(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                   .buildChannel()
                   .pass(new ParcelableFlow<String>(-5, "test4"))
                   .pass(new ParcelableFlow<Integer>(-4, 4))
                   .close();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    AndroidChannels.mergeInput(map)
                   .buildChannel()
                   .pass(new ParcelableFlow<String>(31, "test5"))
                   .pass(new ParcelableFlow<Integer>(17, 5))
                   .close();
    assertThat(channel1.close().in(seconds(10)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().in(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  public void testMergeInputAbort() {

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    AndroidChannels.mergeInput(channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    AndroidChannels.mergeInput(3, channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    AndroidChannels.mergeInput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
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

    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    AndroidChannels.mergeInput(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
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

    channel1 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.on(ServiceSource.serviceOf(getActivity()))
                              .with(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    AndroidChannels.mergeInput(map).buildChannel().abort();

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

      AndroidChannels.mergeInput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.mergeInput(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.mergeInput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.mergeInput(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      AndroidChannels.mergeInput(Collections.<Integer, Channel<?, ?>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputFlow() {

    final Channel<String, String> channel = JRoutineCore.<String>ofData().buildChannel();
    channel.pass("test1", "test2", "test3").close();
    assertThat(AndroidChannels.outputParcelableFlow(channel, 33)
                              .buildChannel()
                              .in(seconds(10))
                              .all()).containsExactly(new ParcelableFlow<String>(33, "test1"),
        new ParcelableFlow<String>(33, "test2"), new ParcelableFlow<String>(33, "test3"));
  }

  public void testOutputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.<String>ofData().buildChannel();
    channel.pass("test1", "test2", "test3").abort();

    try {

      AndroidChannels.outputParcelableFlow(channel, 33).buildChannel().in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlow<Object>> channel;
    channel = routine.invoke()
                     .pass(new ParcelableFlow<Object>(Sort.STRING, "test21"),
                         new ParcelableFlow<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = AndroidChannels.flowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                                .buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlow<Object>(Sort.INTEGER, -11),
                         new ParcelableFlow<Object>(Sort.STRING, "test21"))
                     .close();
    channelMap = AndroidChannels.flowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlow<Object>(Sort.STRING, "test21"),
                         new ParcelableFlow<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = AndroidChannels.flowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                                .buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine =
        JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlow<Object>> channel;
    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new ParcelableFlow<Object>(Sort.STRING, "test21"),
                         new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap = AndroidChannels.flowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                                .buildChannelMap();
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
                     .pass(new ParcelableFlow<Object>(Sort.INTEGER, -11),
                         new ParcelableFlow<Object>(Sort.STRING, "test21"));
    channelMap = AndroidChannels.flowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
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
                     .pass(new ParcelableFlow<Object>(Sort.STRING, "test21"),
                         new ParcelableFlow<Object>(Sort.INTEGER, -11));
    channelMap = AndroidChannels.flowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                                .buildChannelMap();
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
        JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(Sort.class)).buildRoutine();
    final Channel<?, ParcelableFlow<Object>> channel = routine.invoke();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        AndroidChannels.flowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                       .buildChannelMap();
    assertThat(channelMap).isEqualTo(
        AndroidChannels.flowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap());
  }

  public void testOutputMapError() {

    try {

      AndroidChannels.flowOutput(0, 0,
          JRoutineService.on(ServiceSource.serviceOf(getActivity())).with(factoryOf(Sort.class)).invoke());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputSelect() {

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofData().buildChannel();
    final Channel<?, String> outputChannel =
        AndroidChannels.flowOutput(channel, 33).buildChannelMap().get(33);
    channel.pass(new ParcelableFlow<String>(33, "test1"), new ParcelableFlow<String>(-33, "test2"),
        new ParcelableFlow<String>(33, "test3"), new ParcelableFlow<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly("test1", "test3");
  }

  public void testOutputSelectAbort() {

    final Channel<ParcelableFlow<String>, ParcelableFlow<String>> channel =
        JRoutineCore.<ParcelableFlow<String>>ofData().buildChannel();
    final Channel<?, String> outputChannel =
        AndroidChannels.flowOutput(channel, 33).buildChannelMap().get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  private static class Amb<DATA> extends TemplateContextInvocation<ParcelableFlow<DATA>, DATA> {

    private static final int NO_INDEX = Integer.MIN_VALUE;

    private int mFirstIndex;

    @Override
    public void onInput(final ParcelableFlow<DATA> input, @NotNull final Channel<DATA, ?> result) {
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

  private static class CharAt extends TemplateContextInvocation<List<?>, Character> {

    @Override
    public void onInput(final List<?> objects, @NotNull final Channel<Character, ?> result) {
      final String text = (String) objects.get(0);
      final int index = ((Integer) objects.get(1));
      result.pass(text.charAt(index));
    }

    @Override
    public boolean onRecycle() {
      return true;
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
      extends TemplateContextInvocation<ParcelableFlow<Object>, ParcelableFlow<Object>> {

    private static final int INTEGER = 1;

    private static final int STRING = 0;

    @Override
    public void onInput(final ParcelableFlow<Object> flow,
        @NotNull final Channel<ParcelableFlow<Object>, ?> result) {
      switch (flow.id) {
        case INTEGER:
          AndroidChannels.<Object, Integer>parcelableFlowInput(result, INTEGER).buildChannel()
                                                                               .pass(
                                                                                   (Integer) flow
                                                                                       .data)
                                                                               .close();
          break;

        case STRING:
          AndroidChannels.<Object, String>parcelableFlowInput(result, STRING).buildChannel()
                                                                             .pass(
                                                                                 (String) flow.data)
                                                                             .close();
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
