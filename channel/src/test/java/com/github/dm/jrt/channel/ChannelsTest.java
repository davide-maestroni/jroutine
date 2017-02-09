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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.DeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.github.dm.jrt.core.common.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 03/18/2015.
 */
public class ChannelsTest {

  @Test
  public void testBlendOutput() {

    Channel<?, String> channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    Channel<?, String> channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(
        Channels.blendOutput(channel2, channel1).buildChannel().in(seconds(1)).all()).containsOnly(
        "test1", "test2", "test3", "test4", "test5", "test6");
    channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(Channels.blendOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannel()
                       .in(seconds(1))
                       .all()).containsOnly("test1", "test2", "test3", "test4", "test5", "test6");
  }

  @Test
  public void testBlendOutputAbort() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofInputs();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofInputs();
    final Routine<Object, Object> routine =
        JRoutineCore.with(IdentityInvocation.factoryOf()).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.call(Channels.blendOutput(channel1, channel2).buildChannel()).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(
          Channels.blendOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testBlendOutputError() {

    try {

      Channels.blendOutput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.blendOutput((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.blendOutput(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.blendOutput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.blendOutput((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.blendOutput(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testCallableAsync() {
    assertThat(Channels.fromCallable(new Callable<String>() {

      public String call() {
        return "test";
      }
    }).buildChannel().in(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testCallableSync() {
    assertThat(Channels.fromCallable(new Callable<String>() {

      public String call() throws InterruptedException {
        seconds(.3).sleepAtLeast();
        return "test";
      }
    })
                       .channelConfiguration()
                       .withRunner(Runners.immediateRunner())
                       .apply()
                       .buildChannel()
                       .next()).isEqualTo("test");
  }

  @Test
  public void testConcatOutput() {

    Channel<?, String> channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    Channel<?, String> channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(Channels.concatOutput(channel2, channel1)
                       .buildChannel()
                       .in(seconds(1))
                       .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
        "test3");
    channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(Channels.concatOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannel()
                       .in(seconds(1))
                       .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
        "test6");
  }

  @Test
  public void testConcatOutputAbort() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofInputs();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofInputs();
    final Routine<Object, Object> routine =
        JRoutineCore.with(IdentityInvocation.factoryOf()).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.call(Channels.concatOutput(channel1, channel2).buildChannel()).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(
          Channels.concatOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testConcatOutputError() {

    try {

      Channels.concatOutput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.concatOutput((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.concatOutput(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.concatOutput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.concatOutput((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.concatOutput(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<?, String> channel = JRoutineCore.of("test").buildChannel();
    final TestLog testLog = new TestLog();
    assertThat(Channels.outputFlow(channel, 3)
                       .channelConfiguration()
                       .withLog(testLog)
                       .withLogLevel(Level.DEBUG)
                       .apply()
                       .buildChannel()
                       .all()).containsExactly(new Flow<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConfigurationMap() {
    final Channel<Flow<String>, Flow<String>> channel =
        JRoutineCore.<Flow<String>>ofInputs().buildChannel();
    final TestLog testLog = new TestLog();
    Channels.flowInput(3, 1, channel)
            .channelConfiguration()
            .withLog(testLog)
            .withLogLevel(Level.DEBUG)
            .apply()
            .buildChannelMap()
            .get(3)
            .pass("test")
            .close();
    assertThat(channel.close().all()).containsExactly(new Flow<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  @Test
  public void testConstructor() {

    boolean failed = false;
    try {
      new Channels();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInputFlow() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    Channels.inputFlow(channel, 33)
            .buildChannel()
            .pass(new Flow<String>(33, "test1"), new Flow<String>(-33, "test2"),
                new Flow<String>(33, "test3"), new Flow<String>(333, "test4"))
            .close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  @Test
  public void testInputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    Channels.inputFlow(channel, 33).buildChannel().abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testInputMap() {

    final ArrayList<Flow<Object>> outputs = new ArrayList<Flow<Object>>();
    outputs.add(new Flow<Object>(Sort.STRING, "test21"));
    outputs.add(new Flow<Object>(Sort.INTEGER, -11));
    final Routine<Flow<Object>, Flow<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<Flow<Object>, Flow<Object>> channel;
    channel = routine.call();
    channelMap =
        Channels.flowInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap = Channels.flowInput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap =
        Channels.flowInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  @Test
  public void testInputMapAbort() {

    final Routine<Flow<Object>, Flow<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<Flow<Object>, Flow<Object>> channel;
    channel = routine.call();
    channelMap =
        Channels.flowInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap = Channels.flowInput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap =
        Channels.flowInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testInputMapError() {

    try {
      Channels.flowInput(0, 0, JRoutineCore.with(new Sort()).call());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInputSelect() {

    final Channel<Flow<String>, Flow<String>> channel =
        JRoutineCore.<Flow<String>>ofInputs().buildChannel();
    Channels.flowInput(channel, 33).buildChannel().pass("test1", "test2", "test3").close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly(new Flow<String>(33, "test1"),
        new Flow<String>(33, "test2"), new Flow<String>(33, "test3"));
  }

  @Test
  public void testInputSelectAbort() {

    final Channel<Flow<String>, Flow<String>> channel =
        JRoutineCore.<Flow<String>>ofInputs().buildChannel();
    Channels.flowInput(channel, 33).buildChannel().pass("test1", "test2", "test3").abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testInputSelectError() {

    final Channel<Flow<String>, Flow<String>> channel =
        JRoutineCore.<Flow<String>>ofInputs().buildChannel();

    try {
      Channels.flowInput(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.flowInput(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.flowInput(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testJoin() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofInputs();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofInputs();
    final Routine<List<?>, Character> routine = JRoutineCore.with(new CharAt()).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.call(Channels.joinOutput(channel1, channel2).buildChannel())
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.call(
        Channels.joinOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.call(Channels.joinOutput(channel1, channel2).buildChannel())
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
  }

  @Test
  public void testJoinAbort() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofInputs();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofInputs();
    final Routine<List<?>, Character> routine = JRoutineCore.with(new CharAt()).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.call(Channels.joinOutput(channel1, channel2).buildChannel()).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(
          Channels.joinOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testJoinBackoff() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofInputs();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofInputs();
    final Routine<List<?>, Character> routine = JRoutineCore.with(new CharAt()).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.after(millis(100)).pass("test").pass("test").close();
    try {
      routine.call(Channels.joinOutput(channel1, channel2)
                           .channelConfiguration()
                           .withBackoff(afterCount(0).constantDelay(millis(100)))
                           .withMaxSize(1)
                           .apply()
                           .buildChannel()).in(seconds(10)).all();
      fail();

    } catch (final DeadlockException ignored) {

    }
  }

  @Test
  public void testJoinError() {

    try {

      Channels.joinOutput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinOutput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinOutput(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.joinOutput(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testJoinInput() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput(channel1, channel2)
            .buildChannel()
            .pass(Arrays.asList("test1-1", "test1-2"))
            .close();
    Channels.joinInput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(Arrays.asList("test2-1", "test2-2"))
            .close();
    Channels.joinInput(channel1, channel2)
            .buildChannel()
            .pass(Collections.singletonList("test3-1"))
            .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly("test1-2", "test2-2");
  }

  @Test
  public void testJoinInputAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput(channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel().abort();

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

  @Test
  public void testJoinInputError() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput(channel1).buildChannel().pass(Arrays.asList("test1-1", "test1-2")).close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      Channels.joinInput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinInput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinInput(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.joinInput(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testJoinInputPlaceHolderError() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput((Object) null, channel1)
            .buildChannel()
            .pass(Arrays.asList("test1-1", "test1-2"))
            .close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      Channels.joinInput(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinInput(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinInput(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.joinInput(new Object(), Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testJoinInputPlaceholder() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput((Object) null, channel1, channel2)
            .buildChannel()
            .pass(Arrays.asList("test1-1", "test1-2"))
            .close();
    final String placeholder = "placeholder";
    Channels.joinInput((Object) placeholder, Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(Arrays.asList("test2-1", "test2-2"))
            .close();
    Channels.joinInput(placeholder, channel1, channel2)
            .buildChannel()
            .pass(Collections.singletonList("test3-1"))
            .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly("test1-2", "test2-2",
        placeholder);
  }

  @Test
  public void testJoinInputPlaceholderAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput((Object) null, channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.joinInput(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
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

  @Test
  public void testJoinPlaceholder() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofInputs();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofInputs();
    final Routine<List<?>, Character> routine = JRoutineCore.with(new CharAt()).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.call(Channels.joinOutput(new Object(), channel1, channel2).buildChannel())
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.call(
        Channels.joinOutput(null, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(Channels.joinOutput(new Object(), channel1, channel2).buildChannel())
             .in(seconds(10))
             .all();

      fail();

    } catch (final InvocationException ignored) {

    }
  }

  @Test
  public void testJoinPlaceholderAbort() {

    final ChannelBuilder<String, String> builder1 = JRoutineCore.ofInputs();
    final ChannelBuilder<Integer, Integer> builder2 = JRoutineCore.ofInputs();
    final Routine<List<?>, Character> routine = JRoutineCore.with(new CharAt()).buildRoutine();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().abort();

    try {

      routine.call(Channels.joinOutput((Object) null, channel1, channel2).buildChannel())
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(
          Channels.joinOutput(new Object(), Arrays.<Channel<?, ?>>asList(channel1, channel2))
                  .buildChannel()).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testJoinPlaceholderError() {

    try {

      Channels.joinOutput(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinOutput(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.joinOutput(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.joinOutput(new Object(), Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testList() {
    final int count = 7;
    final List<? extends Channel<Object, Object>> channels = Channels.number(count)
                                                                     .channelConfiguration()
                                                                     .withRunner(
                                                                         Runners.immediateRunner())
                                                                     .apply()
                                                                     .buildChannels();
    for (final Channel<Object, Object> channel : channels) {
      assertThat(channel.pass("test").next()).isEqualTo("test");
    }
  }

  @Test
  public void testListEmpty() {
    assertThat(Channels.number(0).buildChannels()).isEmpty();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListError() {
    Channels.number(-1);
  }

  @Test
  public void testMap() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final Channel<String, String> channel1 = builder1.buildChannel();
    final Channel<Integer, Integer> channel2 = builder2.buildChannel();

    final Channel<?, ? extends Flow<Object>> channel =
        Channels.mergeOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    final Channel<?, Flow<Object>> output = JRoutineCore.with(new Sort())
                                                        .invocationConfiguration()
                                                        .withInputOrder(OrderType.SORTED)
                                                        .apply()
                                                        .call(channel);
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        Channels.flowOutput(output, Sort.INTEGER, Sort.STRING).buildChannelMap();

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

  @Test
  public void testMerge() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().channelConfiguration().withOrder(OrderType.SORTED).apply();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends Flow<?>> outputChannel;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.mergeOutput(-7, channel1, channel2).buildChannel();
    channel1.pass("test1").close();
    channel2.pass(13).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(-7, "test1"),
        new Flow<Integer>(-6, 13));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        Channels.mergeOutput(11, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel2.pass(13).close();
    channel1.pass("test1").close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(11, "test1"),
        new Flow<Integer>(12, 13));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.mergeOutput(channel1, channel2).buildChannel();
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(0, "test2"),
        new Flow<Integer>(1, -17));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        Channels.mergeOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(0, "test2"),
        new Flow<Integer>(1, -17));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = Channels.mergeOutput(channelMap).buildChannel();
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(7, "test3"),
        new Flow<Integer>(-3, 111));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMerge4() {

    final ChannelBuilder<String, String> builder =
        JRoutineCore.<String>ofInputs().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final Channel<String, String> channel1 = builder.buildChannel();
    final Channel<String, String> channel2 = builder.buildChannel();
    final Channel<String, String> channel3 = builder.buildChannel();
    final Channel<String, String> channel4 = builder.buildChannel();

    final Routine<Flow<String>, String> routine =
        JRoutineCore.with(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
    final Channel<?, String> outputChannel = routine.call(
        Channels.mergeOutput(Arrays.asList(channel1, channel2, channel3, channel4)).buildChannel());

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

  @Test
  public void testMergeAbort() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().channelConfiguration().withOrder(OrderType.SORTED).apply();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().channelConfiguration().withOrder(OrderType.SORTED).apply();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends Flow<?>> outputChannel;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.mergeOutput(-7, channel1, channel2).buildChannel();
    channel1.pass("test1").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        Channels.mergeOutput(11, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel2.abort();
    channel1.pass("test1").close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.mergeOutput(channel1, channel2).buildChannel();
    channel1.abort();
    channel2.pass(-17).close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        Channels.mergeOutput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel1.pass("test2").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = Channels.mergeOutput(channelMap).buildChannel();
    channel1.abort();
    channel2.pass(111).close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testMergeError() {

    try {

      Channels.mergeOutput(0, Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeOutput(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeOutput(Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeOutput(Collections.<Integer, Channel<?, Object>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeOutput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeOutput(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeOutput(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeOutput(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeOutput(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeOutput(Collections.<Integer, Channel<?, ?>>singletonMap(1, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testMergeInput() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.mergeInput(channel1, channel2)
            .buildChannel()
            .pass(new Flow<String>(0, "test1"))
            .pass(new Flow<Integer>(1, 1))
            .close();
    Channels.mergeInput(3, channel1, channel2)
            .buildChannel()
            .pass(new Flow<String>(3, "test2"))
            .pass(new Flow<Integer>(4, 2))
            .close();
    Channels.mergeInput(Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(new Flow<String>(0, "test3"))
            .pass(new Flow<Integer>(1, 3))
            .close();
    Channels.mergeInput(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(new Flow<String>(-5, "test4"))
            .pass(new Flow<Integer>(-4, 4))
            .close();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    Channels.mergeInput(map)
            .buildChannel()
            .pass(new Flow<String>(31, "test5"))
            .pass(new Flow<Integer>(17, 5))
            .close();
    assertThat(channel1.close().in(seconds(1)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().in(seconds(1)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void testMergeInputAbort() {

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.mergeInput(channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.mergeInput(3, channel1, channel2).buildChannel().abort();

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

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.mergeInput(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel().abort();

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

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.mergeInput(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
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

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    Channels.mergeInput(map).buildChannel().abort();

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

  @Test
  public void testMergeInputError() {

    try {

      Channels.mergeInput();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeInput(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeInput(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeInput(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeInput(Collections.<Integer, Channel<?, ?>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.mergeInput(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeInput(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeInput(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeInput(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.mergeInput(Collections.<Integer, Channel<?, ?>>singletonMap(0, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputFlow() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    channel.pass("test1", "test2", "test3").close();
    assertThat(
        Channels.outputFlow(channel, 33).buildChannel().in(seconds(1)).all()).containsExactly(
        new Flow<String>(33, "test1"), new Flow<String>(33, "test2"),
        new Flow<String>(33, "test3"));
  }

  @Test
  public void testOutputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    channel.pass("test1", "test2", "test3").abort();

    try {

      Channels.outputFlow(channel, 33).buildChannel().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    final Routine<Flow<Object>, Flow<Object>> routine = JRoutineCore.with(new Sort())
                                                                    .invocationConfiguration()
                                                                    .withRunner(
                                                                        Runners.syncRunner())
                                                                    .apply()
                                                                    .buildRoutine();
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, Flow<Object>> channel;
    channel =
        routine.call(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11));
    channelMap =
        Channels.flowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel =
        routine.call(new Flow<Object>(Sort.INTEGER, -11), new Flow<Object>(Sort.STRING, "test21"));
    channelMap = Channels.flowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel =
        routine.call(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11));
    channelMap =
        Channels.flowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    final Routine<Flow<Object>, Flow<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, Flow<Object>> channel;
    channel = routine.call()
                     .after(millis(100))
                     .pass(new Flow<Object>(Sort.STRING, "test21"),
                         new Flow<Object>(Sort.INTEGER, -11));
    channelMap =
        Channels.flowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
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

    channel = routine.call()
                     .after(millis(100))
                     .pass(new Flow<Object>(Sort.INTEGER, -11),
                         new Flow<Object>(Sort.STRING, "test21"));
    channelMap = Channels.flowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
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

    channel = routine.call()
                     .after(millis(100))
                     .pass(new Flow<Object>(Sort.STRING, "test21"),
                         new Flow<Object>(Sort.INTEGER, -11));
    channelMap =
        Channels.flowOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
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

  @Test
  public void testOutputMapCache() {

    final Routine<Flow<Object>, Flow<Object>> routine =
        JRoutineCore.with(factoryOf(Sort.class)).buildRoutine();
    final Channel<?, Flow<Object>> channel = routine.call();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        Channels.flowOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    assertThat(channelMap).isEqualTo(
        Channels.flowOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap());
  }

  @Test
  public void testOutputMapError() {

    try {
      Channels.flowOutput(0, 0, JRoutineCore.with(new Sort()).call());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputSelect() {

    final Channel<Flow<String>, Flow<String>> channel =
        JRoutineCore.<Flow<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        Channels.flowOutput(channel, 33).buildChannelMap().get(33);
    channel.pass(new Flow<String>(33, "test1"), new Flow<String>(-33, "test2"),
        new Flow<String>(33, "test3"), new Flow<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  @Test
  public void testOutputSelectAbort() {

    final Channel<Flow<String>, Flow<String>> channel =
        JRoutineCore.<Flow<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        Channels.flowOutput(channel, 33).buildChannelMap().get(33);
    channel.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testOutputSelectError() {

    final Channel<Flow<String>, Flow<String>> channel =
        JRoutineCore.<Flow<String>>ofInputs().buildChannel();

    try {
      Channels.flowOutput(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.flowOutput(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.flowOutput(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testReplayError() {

    try {
      Channels.replayOutput(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSelectMap() {

    final Routine<Flow<Object>, Flow<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    final Channel<Flow<Object>, Flow<Object>> inputChannel =
        JRoutineCore.<Flow<Object>>ofInputs().buildChannel();
    final Channel<?, Flow<Object>> outputChannel = routine.call(inputChannel);
    final Channel<?, Object> intChannel =
        Channels.flowOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                .channelConfiguration()
                .withLogLevel(Level.WARNING)
                .apply()
                .buildChannelMap()
                .get(Sort.INTEGER);
    final Channel<?, Object> strChannel =
        Channels.flowOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                .channelConfiguration()
                .withLogLevel(Level.WARNING)
                .apply()
                .buildChannelMap()
                .get(Sort.STRING);
    inputChannel.pass(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11));
    assertThat(intChannel.in(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.in(seconds(10)).next()).isEqualTo("test21");
    inputChannel.pass(new Flow<Object>(Sort.INTEGER, -11), new Flow<Object>(Sort.STRING, "test21"));
    assertThat(intChannel.in(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.in(seconds(10)).next()).isEqualTo("test21");
    inputChannel.pass(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11));
    assertThat(intChannel.in(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.in(seconds(10)).next()).isEqualTo("test21");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSelectMapAbort() {

    final Routine<Flow<Object>, Flow<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Channel<Flow<Object>, Flow<Object>> inputChannel =
        JRoutineCore.<Flow<Object>>ofInputs().buildChannel();
    Channel<?, Flow<Object>> outputChannel = routine.call(inputChannel);
    Channels.flowOutput(outputChannel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    inputChannel.after(millis(100))
                .pass(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      Channels.flowOutput(outputChannel, Sort.STRING, Sort.INTEGER)
              .buildChannelMap()
              .get(Sort.STRING)
              .in(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      Channels.flowOutput(outputChannel, Sort.INTEGER, Sort.STRING)
              .buildChannelMap()
              .get(Sort.INTEGER)
              .in(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.<Flow<Object>>ofInputs().buildChannel();
    outputChannel = routine.call(inputChannel);
    Channels.flowOutput(outputChannel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    inputChannel.after(millis(100))
                .pass(new Flow<Object>(Sort.INTEGER, -11), new Flow<Object>(Sort.STRING, "test21"))
                .abort();

    try {

      Channels.flowOutput(outputChannel, Sort.STRING, Sort.INTEGER)
              .buildChannelMap()
              .get(Sort.STRING)
              .in(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      Channels.flowOutput(outputChannel, Sort.STRING, Sort.INTEGER)
              .buildChannelMap()
              .get(Sort.INTEGER)
              .in(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.<Flow<Object>>ofInputs().buildChannel();
    outputChannel = routine.call(inputChannel);
    Channels.flowOutput(outputChannel, Sort.STRING, Sort.INTEGER).buildChannelMap();
    inputChannel.after(millis(100))
                .pass(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      Channels.flowOutput(outputChannel, Sort.INTEGER, Sort.STRING)
              .buildChannelMap()
              .get(Sort.STRING)
              .in(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      Channels.flowOutput(outputChannel, Sort.INTEGER, Sort.STRING)
              .buildChannelMap()
              .get(Sort.INTEGER)
              .in(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  private static class Amb<DATA> extends TemplateInvocation<Flow<DATA>, DATA> {

    private static final int NO_INDEX = Integer.MIN_VALUE;

    private int mFirstIndex;

    @Override
    public void onInput(final Flow<DATA> input, @NotNull final Channel<DATA, ?> result) {
      if (mFirstIndex == NO_INDEX) {
        mFirstIndex = input.id;
        result.pass(input.data);

      } else if (mFirstIndex == input.id) {
        result.pass(input.data);
      }
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }

    @Override
    public void onRestart() {
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

  private static class Sort extends MappingInvocation<Flow<Object>, Flow<Object>> {

    private static final int INTEGER = 1;

    private static final int STRING = 0;

    /**
     * Constructor.
     */
    protected Sort() {

      super(null);
    }

    public void onInput(final Flow<Object> flow, @NotNull final Channel<Flow<Object>, ?> result) {

      switch (flow.id) {

        case INTEGER:
          Channels.<Object, Integer>flowInput(result, INTEGER).buildChannel()
                                                              .pass(flow.<Integer>data())
                                                              .close();
          break;

        case STRING:
          Channels.<Object, String>flowInput(result, STRING).buildChannel()
                                                            .pass(flow.<String>data())
                                                            .close();
          break;
      }
    }
  }

  private static class TestLog implements Log {

    private int mLogCount;

    public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mLogCount;
    }

    public void err(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mLogCount;
    }

    public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mLogCount;
    }
  }
}
