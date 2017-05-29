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

import com.github.dm.jrt.android.channel.io.ParcelableByteChannel.ParcelableByteChunk;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceSource;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.channel.config.ByteChunkStreamConfiguration.CloseActionType;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkOutputStream;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class JRoutineAndroidChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public JRoutineAndroidChannelsTest() {

    super(TestActivity.class);
  }

  public void testByteChannel() throws IOException {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> channel =
        JRoutineCore.channel().ofType();
    final ByteChunkOutputStream stream = JRoutineAndroidChannels.parcelableOutputStream()
                                                                .withStream()
                                                                .withChunkSize(4)
                                                                .withOnClose(
                                                                    CloseActionType.CLOSE_CHANNEL)
                                                                .configuration()
                                                                .of(channel);
    final byte[] b = new byte[16];
    stream.write(b);
    stream.close();
    ByteChunkInputStream inputStream =
        JRoutineAndroidChannels.parcelableInputStream(channel.next());
    assertThat(inputStream.available()).isEqualTo(4);
    assertThat(inputStream.skip(4)).isEqualTo(4);
    assertThat(inputStream.available()).isEqualTo(0);
    inputStream.close();
    inputStream = JRoutineAndroidChannels.parcelableInputStream(channel.next(), channel.next());
    assertThat(inputStream.available()).isEqualTo(8);
    assertThat(inputStream.readAll(new ByteArrayOutputStream())).isEqualTo(8);
    assertThat(inputStream.available()).isEqualTo(0);
    inputStream.close();
    inputStream =
        JRoutineAndroidChannels.parcelableInputStream(Collections.singleton(channel.next()));
    assertThat(inputStream.available()).isEqualTo(4);
    inputStream.close();
  }

  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<?, String> channel = JRoutineCore.channel().of("test");
    final TestLog testLog = new TestLog();
    assertThat(JRoutineAndroidChannels.channelHandler()
                                      .withChannel()
                                      .withLog(testLog)
                                      .withLogLevel(Level.DEBUG)
                                      .configuration()
                                      .outputParcelableFlowOf(channel, 3)
                                      .all()).containsExactly(
        new ParcelableFlowData<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  public void testConstructor() {

    boolean failed = false;
    try {
      new JRoutineAndroidChannels();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @SuppressWarnings("unchecked")
  public void testInputFlow() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineAndroidChannels.channelHandler()
                           .inputFlowOf(channel, 33)
                           .pass(new ParcelableFlowData<String>(33, "test1"),
                               new ParcelableFlowData<String>(-33, "test2"),
                               new ParcelableFlowData<String>(33, "test3"),
                               new ParcelableFlowData<String>(333, "test4"))
                           .close();
    channel.close();
    assertThat(channel.in(seconds(10)).all()).containsExactly("test1", "test3");
  }

  public void testInputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineAndroidChannels.channelHandler().inputFlowOf(channel, 33).abort();
    channel.close();

    try {

      channel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testInputSelect() {

    final Channel<ParcelableFlowData<String>, ParcelableFlowData<String>> channel =
        JRoutineCore.channel().ofType();
    JRoutineAndroidChannels.channelHandler()
                           .inputOfParcelableFlow(channel, 33)
                           .pass("test1", "test2", "test3")
                           .close();
    channel.close();
    assertThat(channel.in(seconds(10)).all()).containsExactly(
        new ParcelableFlowData<String>(33, "test1"), new ParcelableFlowData<String>(33, "test2"),
        new ParcelableFlowData<String>(33, "test3"));
  }

  public void testInputSelectAbort() {

    final Channel<ParcelableFlowData<String>, ParcelableFlowData<String>> channel =
        JRoutineCore.channel().ofType();
    JRoutineAndroidChannels.channelHandler()
                           .inputOfParcelableFlow(channel, 33)
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
    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(CharAt.class));
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(
                          JRoutineAndroidChannels.channelHandler().joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineAndroidChannels.channelHandler()
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
                          JRoutineAndroidChannels.channelHandler().joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
  }

  public void testJoinAbort() {
    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(CharAt.class));
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(JRoutineAndroidChannels.channelHandler().joinOutputOf(channel1, channel2))
             .close()
             .in(seconds(10))
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
             .pass(JRoutineAndroidChannels.channelHandler()
                                          .joinOutputOf(
                                              Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinError() {
    try {
      JRoutineAndroidChannels.channelHandler().joinOutputOf();
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      JRoutineAndroidChannels.channelHandler().joinOutputOf(Collections.<Channel<?, ?>>emptyList());
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  public void testJoinInput() {

    final Channel<String, String> channel1 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingString.class))
                       .invoke()
                       .sorted();
    final Channel<String, String> channel2 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingString.class))
                       .invoke()
                       .sorted();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf(channel1, channel2)
                           .pass(Arrays.asList("test1-1", "test1-2"))
                           .close();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                           .pass(Arrays.asList("test2-1", "test2-2"))
                           .close();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf(channel1, channel2)
                           .pass(Collections.singletonList("test3-1"))
                           .close();
    assertThat(channel1.close().in(seconds(10)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(10)).all()).containsExactly("test1-2", "test2-2");
  }

  public void testJoinInputAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler().joinInputOf(channel1, channel2).abort();

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

    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
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

    final Channel<String, String> channel1 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingString.class))
                       .invoke()
                       .sorted();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf(channel1)
                           .pass(Arrays.asList("test1-1", "test1-2"))
                           .close();

    try {

      channel1.close().in(seconds(10)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler().joinInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler().joinInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testJoinInputPlaceholder() {

    final Channel<String, String> channel1 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingString.class))
                       .invoke()
                       .sorted();
    final Channel<String, String> channel2 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingString.class))
                       .invoke()
                       .sorted();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf((Object) null, channel1, channel2)
                           .pass(Arrays.asList("test1-1", "test1-2"))
                           .close();
    final String placeholder = "placeholder";
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf((Object) placeholder,
                               Arrays.<Channel<?, ?>>asList(channel1, channel2))
                           .pass(Arrays.asList("test2-1", "test2-2"))
                           .close();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf(placeholder, channel1, channel2)
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
    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler().joinInputOf((Object) null, channel1, channel2).abort();

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

    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
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

    final Channel<String, String> channel1 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingString.class))
                       .invoke()
                       .sorted();
    JRoutineAndroidChannels.channelHandler()
                           .joinInputOf((Object) null, channel1)
                           .pass(Arrays.asList("test1-1", "test1-2"))
                           .close();

    try {

      channel1.close().in(seconds(10)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler().joinInputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler()
                             .joinInputOf(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testJoinPlaceholder() {
    final ChannelBuilder builder = JRoutineCore.channel();
    final Routine<List<?>, Character> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(CharAt.class));
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineAndroidChannels.channelHandler()
                                                   .joinOutputOf(new Object(), channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineAndroidChannels.channelHandler()
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
             .pass(JRoutineAndroidChannels.channelHandler()
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
    final Routine<List<?>, Character> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(CharAt.class));
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.invoke()
             .pass(JRoutineAndroidChannels.channelHandler()
                                          .joinOutputOf((Object) null, channel1, channel2))
             .close()
             .in(seconds(10))
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
             .pass(JRoutineAndroidChannels.channelHandler()
                                          .joinOutputOf(new Object(),
                                              Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public void testJoinPlaceholderError() {

    try {

      JRoutineAndroidChannels.channelHandler().joinOutputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler()
                             .joinOutputOf(null, Collections.<Channel<?, ?>>emptyList());

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
        JRoutineAndroidChannels.channelHandler()
                               .mergeParcelableOutputOf(
                                   Arrays.<Channel<?, ?>>asList(channel1, channel2));
    final Channel<?, ParcelableFlowData<Object>> output =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withInputOrder(OrderType.SORTED)
                       .configuration()
                       .of(factoryOf(Sort.class))
                       .invoke()
                       .pass(channel)
                       .close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineAndroidChannels.channelHandler().outputOfFlow(output, Sort.INTEGER, Sort.STRING);

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
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends ParcelableFlowData<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel =
        JRoutineAndroidChannels.channelHandler().mergeParcelableOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.pass(13).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlowData<String>(-7, "test1"), new ParcelableFlowData<Integer>(-6, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineAndroidChannels.channelHandler()
                                           .mergeParcelableOutputOf(11,
                                               Arrays.asList(channel1, channel2));
    channel2.pass(13).close();
    channel1.pass("test1").close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlowData<String>(11, "test1"), new ParcelableFlowData<Integer>(12, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel =
        JRoutineAndroidChannels.channelHandler().mergeParcelableOutputOf(channel1, channel2);
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlowData<String>(0, "test2"), new ParcelableFlowData<Integer>(1, -17));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineAndroidChannels.channelHandler()
                                           .mergeParcelableOutputOf(
                                               Arrays.asList(channel1, channel2));
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(
        new ParcelableFlowData<String>(0, "test2"), new ParcelableFlowData<Integer>(1, -17));
  }

  @SuppressWarnings("unchecked")
  public void testMerge4() {
    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<String, String> channel2 = builder.ofType();
    final Channel<String, String> channel3 = builder.ofType();
    final Channel<String, String> channel4 = builder.ofType();

    final Routine<ParcelableFlowData<String>, String> routine =
        JRoutineCore.routine().of(InvocationFactory.factoryOf(new ClassToken<Amb<String>>() {}));
    final Channel<?, String> outputChannel = routine.invoke()
                                                    .pass(JRoutineAndroidChannels.channelHandler()
                                                                                 .mergeParcelableOutputOf(
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

  @SuppressWarnings("unchecked")
  public void testMergeAbort() {
    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends ParcelableFlowData<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel =
        JRoutineAndroidChannels.channelHandler().mergeParcelableOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineAndroidChannels.channelHandler()
                                           .mergeParcelableOutputOf(11,
                                               Arrays.asList(channel1, channel2));
    channel2.abort();
    channel1.pass("test1").close();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel =
        JRoutineAndroidChannels.channelHandler().mergeParcelableOutputOf(channel1, channel2);
    channel1.abort();
    channel2.pass(-17).close();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineAndroidChannels.channelHandler()
                                           .mergeParcelableOutputOf(
                                               Arrays.asList(channel1, channel2));
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

      JRoutineAndroidChannels.channelHandler()
                             .mergeParcelableOutputOf(0,
                                 Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler().mergeParcelableOutputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler()
                             .mergeParcelableOutputOf(Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler().mergeParcelableOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMergeInput() {

    final Channel<String, String> channel1 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingString.class))
                       .invoke()
                       .sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(PassingInteger.class))
                       .invoke()
                       .sorted();
    JRoutineAndroidChannels.channelHandler()
                           .mergeInputOf(channel1, channel2)
                           .pass(new ParcelableFlowData<String>(0, "test1"))
                           .pass(new ParcelableFlowData<Integer>(1, 1))
                           .close();
    JRoutineAndroidChannels.channelHandler()
                           .mergeInputOf(3, channel1, channel2)
                           .pass(new ParcelableFlowData<String>(3, "test2"))
                           .pass(new ParcelableFlowData<Integer>(4, 2))
                           .close();
    JRoutineAndroidChannels.channelHandler()
                           .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                           .pass(new ParcelableFlowData<String>(0, "test3"))
                           .pass(new ParcelableFlowData<Integer>(1, 3))
                           .close();
    JRoutineAndroidChannels.channelHandler()
                           .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                           .pass(new ParcelableFlowData<String>(-5, "test4"))
                           .pass(new ParcelableFlowData<Integer>(-4, 4))
                           .close();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineAndroidChannels.channelHandler()
                           .mergeInputOf(map)
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
    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler().mergeInputOf(channel1, channel2).abort();

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

    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler().mergeInputOf(3, channel1, channel2).abort();

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

    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler()
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

    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    JRoutineAndroidChannels.channelHandler()
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

    channel1 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingString.class))
                              .invoke()
                              .sorted();
    channel2 = JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(PassingInteger.class))
                              .invoke()
                              .sorted();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineAndroidChannels.channelHandler().mergeInputOf(map).abort();

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

      JRoutineAndroidChannels.channelHandler().mergeInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler().mergeInputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler().mergeInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler()
                             .mergeInputOf(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineAndroidChannels.channelHandler()
                             .mergeInputOf(Collections.<Integer, Channel<?, ?>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputFlow() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").close();
    assertThat(JRoutineAndroidChannels.channelHandler()
                                      .outputParcelableFlowOf(channel, 33)
                                      .in(seconds(10))
                                      .all()).containsExactly(
        new ParcelableFlowData<String>(33, "test1"), new ParcelableFlowData<String>(33, "test2"),
        new ParcelableFlowData<String>(33, "test3"));
  }

  public void testOutputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").abort();

    try {

      JRoutineAndroidChannels.channelHandler()
                             .outputParcelableFlowOf(channel, 33)
                             .in(seconds(10))
                             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity())).of(factoryOf(Sort.class));
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlowData<Object>> channel;
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineAndroidChannels.channelHandler()
                                        .outputOfFlow(channel,
                                            Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.INTEGER, -11),
                         new ParcelableFlowData<Object>(Sort.STRING, "test21"))
                     .close();
    channelMap =
        JRoutineAndroidChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineAndroidChannels.channelHandler()
                                        .outputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                            channel);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(10)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(10)).all()).containsOnly("test21");
  }

  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity())).of(factoryOf(Sort.class));
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, ParcelableFlowData<Object>> channel;
    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new ParcelableFlowData<Object>(Sort.STRING, "test21"),
                         new ParcelableFlowData<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineAndroidChannels.channelHandler()
                                        .outputOfFlow(channel,
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
    channelMap =
        JRoutineAndroidChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
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
    channelMap = JRoutineAndroidChannels.channelHandler()
                                        .outputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                            channel);
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
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity())).of(factoryOf(Sort.class));
    final Channel<?, ParcelableFlowData<Object>> channel = routine.invoke();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineAndroidChannels.channelHandler()
                               .outputOfFlow(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap).isEqualTo(
        JRoutineAndroidChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING));
  }

  public void testOutputMapError() {

    try {

      JRoutineAndroidChannels.channelHandler()
                             .outputOfFlow(0, 0,
                                 JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                                                .of(factoryOf(Sort.class))
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
        JRoutineAndroidChannels.channelHandler().outputOfFlow(channel, 33).get(33);
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
        JRoutineAndroidChannels.channelHandler().outputOfFlow(channel, 33).get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(10)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  private static class Amb<DATA> extends TemplateContextInvocation<ParcelableFlowData<DATA>, DATA> {

    private static final int NO_INDEX = Integer.MIN_VALUE;

    private int mFirstIndex;

    @Override
    public void onInput(final ParcelableFlowData<DATA> input,
        @NotNull final Channel<DATA, ?> result) {
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
      extends TemplateContextInvocation<ParcelableFlowData<Object>, ParcelableFlowData<Object>> {

    private static final int INTEGER = 1;

    private static final int STRING = 0;

    @Override
    public void onInput(final ParcelableFlowData<Object> flow,
        @NotNull final Channel<ParcelableFlowData<Object>, ?> result) {
      switch (flow.id) {
        case INTEGER:
          JRoutineAndroidChannels.channelHandler().<Object, Integer>inputOfParcelableFlow(result,
              INTEGER).pass((Integer) flow.data).close();
          break;

        case STRING:
          JRoutineAndroidChannels.channelHandler().<Object, String>inputOfParcelableFlow(result,
              STRING).pass((String) flow.data).close();
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
