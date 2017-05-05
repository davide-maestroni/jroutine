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
import static com.github.dm.jrt.core.executor.ScheduledExecutors.immediateExecutor;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
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

    Channel<?, String> channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    Channel<?, String> channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineChannels.channelHandler()
                               .blendOutputOf(channel2, channel1)
                               .in(seconds(1))
                               .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
        "test6");
    channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineChannels.channelHandler()
                               .blendOutputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                               .in(seconds(1))
                               .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
        "test6");
  }

  @Test
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
             .pass(JRoutineChannels.channelHandler().blendOutputOf(channel1, channel2))
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
             .pass(JRoutineChannels.channelHandler()
                                   .blendOutputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
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

      JRoutineChannels.channelHandler().blendOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().blendOutputOf((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().blendOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().blendOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().blendOutputOf((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .blendOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testCallableAsync() {
    assertThat(JRoutineChannels.channelHandler().channelOf(new Callable<String>() {

      public String call() {
        return "test";
      }
    }).in(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testCallableSync() {
    assertThat(
        JRoutineChannels.channelHandlerOn(immediateExecutor()).channelOf(new Callable<String>() {

          public String call() throws InterruptedException {
            seconds(.3).sleepAtLeast();
            return "test";
          }
        }).next()).isEqualTo("test");
  }

  @Test
  public void testConcatOutput() {

    Channel<?, String> channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    Channel<?, String> channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineChannels.channelHandler()
                               .concatOutputOf(channel2, channel1)
                               .in(seconds(1))
                               .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
        "test3");
    channel1 = JRoutineCore.channel().of("test1", "test2", "test3");
    channel2 = JRoutineCore.channel().of("test4", "test5", "test6");
    assertThat(JRoutineChannels.channelHandler()
                               .concatOutputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                               .in(seconds(1))
                               .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
        "test6");
  }

  @Test
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
             .pass(JRoutineChannels.channelHandler().concatOutputOf(channel1, channel2))
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
             .pass(JRoutineChannels.channelHandler()
                                   .concatOutputOf(
                                       Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
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

      JRoutineChannels.channelHandler().concatOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().concatOutputOf((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().concatOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().concatOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().concatOutputOf((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .concatOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<?, String> channel = JRoutineCore.channel().of("test");
    final TestLog testLog = new TestLog();
    assertThat(JRoutineChannels.channelHandler()
                               .withChannel()
                               .withLog(testLog)
                               .withLogLevel(Level.DEBUG)
                               .configuration()
                               .outputFlowOf(channel, 3)
                               .all()).containsExactly(new Flow<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConfigurationMap() {
    final Channel<Flow<String>, Flow<String>> channel = JRoutineCore.channel().ofType();
    final TestLog testLog = new TestLog();
    JRoutineChannels.channelHandler()
                    .withChannel()
                    .withLog(testLog)
                    .withLogLevel(Level.DEBUG)
                    .configuration()
                    .inputOfFlow(3, 1, channel)
                    .get(3)
                    .pass("test")
                    .close();
    assertThat(channel.close().all()).containsExactly(new Flow<String>(3, "test"));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInputFlow() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineChannels.channelHandler()
                    .inputFlowOf(channel, 33)
                    .pass(new Flow<String>(33, "test1"), new Flow<String>(-33, "test2"),
                        new Flow<String>(33, "test3"), new Flow<String>(333, "test4"))
                    .close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  @Test
  public void testInputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    JRoutineChannels.channelHandler().inputFlowOf(channel, 33).abort();

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
    final Routine<Flow<Object>, Flow<Object>> routine = JRoutineCore.routine().of(new Sort());
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<Flow<Object>, Flow<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineChannels.channelHandler()
                                 .inputOfFlow(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineChannels.channelHandler().inputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.invoke();
    channelMap = JRoutineChannels.channelHandler()
                                 .inputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().in(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  @Test
  public void testInputMapAbort() {

    final Routine<Flow<Object>, Flow<Object>> routine = JRoutineCore.routine().of(new Sort());
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<Flow<Object>, Flow<Object>> channel;
    channel = routine.invoke();
    channelMap = JRoutineChannels.channelHandler()
                                 .inputOfFlow(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap = JRoutineChannels.channelHandler().inputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.invoke();
    channelMap = JRoutineChannels.channelHandler()
                                 .inputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
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
      JRoutineChannels.channelHandler()
                      .inputOfFlow(0, 0, JRoutineCore.routine().of(new Sort()).invoke());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInputSelect() {

    final Channel<Flow<String>, Flow<String>> channel = JRoutineCore.channel().ofType();
    JRoutineChannels.channelHandler()
                    .inputOfFlow(channel, 33)
                    .pass("test1", "test2", "test3")
                    .close();
    assertThat(channel.close().in(seconds(1)).all()).containsExactly(new Flow<String>(33, "test1"),
        new Flow<String>(33, "test2"), new Flow<String>(33, "test3"));
  }

  @Test
  public void testInputSelectAbort() {

    final Channel<Flow<String>, Flow<String>> channel = JRoutineCore.channel().ofType();
    JRoutineChannels.channelHandler()
                    .inputOfFlow(channel, 33)
                    .pass("test1", "test2", "test3")
                    .abort();

    try {

      channel.close().in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testInputSelectError() {

    final Channel<Flow<String>, Flow<String>> channel = JRoutineCore.channel().ofType();

    try {
      JRoutineChannels.channelHandler().inputOfFlow(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineChannels.channelHandler().inputOfFlow(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineChannels.channelHandler()
                      .inputOfFlow(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
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
                      .pass(JRoutineChannels.channelHandler().joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineChannels.channelHandler()
                                            .joinOutputOf(
                                                Arrays.<Channel<?, ?>>asList(channel1, channel2)))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineChannels.channelHandler().joinOutputOf(channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
  }

  @Test
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
             .pass(JRoutineChannels.channelHandler().joinOutputOf(channel1, channel2))
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
             .pass(JRoutineChannels.channelHandler()
                                   .joinOutputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
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
             .pass(JRoutineChannels.channelHandler()
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

  @Test
  public void testJoinError() {

    try {

      JRoutineChannels.channelHandler().joinOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinOutputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .joinOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testJoinInput() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineChannels.channelHandler()
                    .joinInputOf(channel1, channel2)
                    .pass(Arrays.asList("test1-1", "test1-2"))
                    .close();
    JRoutineChannels.channelHandler()
                    .joinInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                    .pass(Arrays.asList("test2-1", "test2-2"))
                    .close();
    JRoutineChannels.channelHandler()
                    .joinInputOf(channel1, channel2)
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
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineChannels.channelHandler().joinInputOf(channel1, channel2).abort();

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
    JRoutineChannels.channelHandler()
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

  @Test
  public void testJoinInputError() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineChannels.channelHandler()
                    .joinInputOf(channel1)
                    .pass(Arrays.asList("test1-1", "test1-2"))
                    .close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinInputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinInputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testJoinInputPlaceHolderError() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineChannels.channelHandler()
                    .joinInputOf((Object) null, channel1)
                    .pass(Arrays.asList("test1-1", "test1-2"))
                    .close();

    try {

      channel1.close().in(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinInputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinInputOf(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinInputOf(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .joinInputOf(new Object(), Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testJoinInputPlaceholder() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineChannels.channelHandler()
                    .joinInputOf((Object) null, channel1, channel2)
                    .pass(Arrays.asList("test1-1", "test1-2"))
                    .close();
    final String placeholder = "placeholder";
    JRoutineChannels.channelHandler()
                    .joinInputOf((Object) placeholder,
                        Arrays.<Channel<?, ?>>asList(channel1, channel2))
                    .pass(Arrays.asList("test2-1", "test2-2"))
                    .close();
    JRoutineChannels.channelHandler()
                    .joinInputOf(placeholder, channel1, channel2)
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
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    JRoutineChannels.channelHandler().joinInputOf((Object) null, channel1, channel2).abort();

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
    JRoutineChannels.channelHandler()
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

  @Test
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
                      .pass(JRoutineChannels.channelHandler()
                                            .joinOutputOf(new Object(), channel1, channel2))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.invoke()
                      .pass(JRoutineChannels.channelHandler()
                                            .joinOutputOf(null,
                                                Arrays.<Channel<?, ?>>asList(channel1, channel2)))
                      .close()
                      .in(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.invoke()
             .pass(JRoutineChannels.channelHandler().joinOutputOf(new Object(), channel1, channel2))
             .close()
             .in(seconds(10))
             .all();

      fail();

    } catch (final InvocationException ignored) {

    }
  }

  @Test
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
             .pass(
                 JRoutineChannels.channelHandler().joinOutputOf((Object) null, channel1, channel2))
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
             .pass(JRoutineChannels.channelHandler()
                                   .joinOutputOf(new Object(),
                                       Arrays.<Channel<?, ?>>asList(channel1, channel2)))
             .close()
             .in(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testJoinPlaceholderError() {

    try {

      JRoutineChannels.channelHandler().joinOutputOf(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinOutputOf(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().joinOutputOf(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .joinOutputOf(new Object(), Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testList() {
    final int count = 7;
    final List<? extends Channel<Object, Object>> channels =
        JRoutineChannels.channelHandlerOn(immediateExecutor()).channels(count);
    for (final Channel<Object, Object> channel : channels) {
      assertThat(channel.pass("test").next()).isEqualTo("test");
    }
  }

  @Test
  public void testListEmpty() {
    assertThat(JRoutineChannels.channelHandler().channels(0)).isEmpty();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListError() {
    JRoutineChannels.channelHandler().channels(-1);
  }

  @Test
  public void testMap() {

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<Integer, Integer> channel2 = builder.ofType();

    final Channel<?, ? extends Flow<Object>> channel = JRoutineChannels.channelHandler()
                                                                       .mergeOutputOf(
                                                                           Arrays.<Channel<?,
                                                                               ?>>asList(
                                                                               channel1, channel2));
    final Channel<?, Flow<Object>> output = JRoutineCore.routine()
                                                        .withInvocation()
                                                        .withInputOrder(OrderType.SORTED)
                                                        .configuration()
                                                        .of(new Sort())
                                                        .invoke()
                                                        .pass(channel)
                                                        .close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineChannels.channelHandler().outputOfFlow(output, Sort.INTEGER, Sort.STRING);

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

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends Flow<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineChannels.channelHandler().mergeOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.pass(13).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(-7, "test1"),
        new Flow<Integer>(-6, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineChannels.channelHandler()
                                    .mergeOutputOf(11,
                                        Arrays.<Channel<?, ?>>asList(channel1, channel2));
    channel2.pass(13).close();
    channel1.pass("test1").close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(11, "test1"),
        new Flow<Integer>(12, 13));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineChannels.channelHandler().mergeOutputOf(channel1, channel2);
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(0, "test2"),
        new Flow<Integer>(1, -17));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineChannels.channelHandler()
                                    .mergeOutputOf(
                                        Arrays.<Channel<?, ?>>asList(channel1, channel2));
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(0, "test2"),
        new Flow<Integer>(1, -17));
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = JRoutineChannels.channelHandler().mergeOutputOf(channelMap);
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.in(seconds(1)).all()).containsOnly(new Flow<String>(7, "test3"),
        new Flow<Integer>(-3, 111));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMerge4() {

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel1 = builder.ofType();
    final Channel<String, String> channel2 = builder.ofType();
    final Channel<String, String> channel3 = builder.ofType();
    final Channel<String, String> channel4 = builder.ofType();

    final Routine<Flow<String>, String> routine =
        JRoutineCore.routine().of(factoryOf(new ClassToken<Amb<String>>() {}));
    final Channel<?, String> outputChannel = routine.invoke()
                                                    .pass(JRoutineChannels.channelHandler()
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

  @Test
  public void testMergeAbort() {

    final ChannelBuilder builder =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends Flow<?>> outputChannel;
    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineChannels.channelHandler().mergeOutputOf(-7, channel1, channel2);
    channel1.pass("test1").close();
    channel2.abort();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineChannels.channelHandler()
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
    outputChannel = JRoutineChannels.channelHandler().mergeOutputOf(channel1, channel2);
    channel1.abort();
    channel2.pass(-17).close();

    try {

      outputChannel.in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder.ofType();
    channel2 = builder.ofType();
    outputChannel = JRoutineChannels.channelHandler()
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
    outputChannel = JRoutineChannels.channelHandler().mergeOutputOf(channelMap);
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

      JRoutineChannels.channelHandler()
                      .mergeOutputOf(0, Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeOutputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeOutputOf(Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeOutputOf(Collections.<Integer, Channel<?, Object>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeOutputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeOutputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeOutputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeOutputOf(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeOutputOf(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeOutputOf(Collections.<Integer, Channel<?, ?>>singletonMap(1, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testMergeInput() {

    final Channel<String, String> channel1 =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineChannels.channelHandler()
                    .mergeInputOf(channel1, channel2)
                    .pass(new Flow<String>(0, "test1"))
                    .pass(new Flow<Integer>(1, 1))
                    .close();
    JRoutineChannels.channelHandler()
                    .mergeInputOf(3, channel1, channel2)
                    .pass(new Flow<String>(3, "test2"))
                    .pass(new Flow<Integer>(4, 2))
                    .close();
    JRoutineChannels.channelHandler()
                    .mergeInputOf(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                    .pass(new Flow<String>(0, "test3"))
                    .pass(new Flow<Integer>(1, 3))
                    .close();
    JRoutineChannels.channelHandler()
                    .mergeInputOf(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                    .pass(new Flow<String>(-5, "test4"))
                    .pass(new Flow<Integer>(-4, 4))
                    .close();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    JRoutineChannels.channelHandler()
                    .mergeInputOf(map)
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
    channel1 = JRoutineCore.routine().of(IdentityInvocation.<String>factory()).invoke().sorted();
    channel2 = JRoutineCore.routine().of(IdentityInvocation.<Integer>factory()).invoke().sorted();
    JRoutineChannels.channelHandler().mergeInputOf(channel1, channel2).abort();

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
    JRoutineChannels.channelHandler().mergeInputOf(3, channel1, channel2).abort();

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
    JRoutineChannels.channelHandler()
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
    JRoutineChannels.channelHandler()
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
    JRoutineChannels.channelHandler().mergeInputOf(map).abort();

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

      JRoutineChannels.channelHandler().mergeInputOf();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeInputOf(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeInputOf(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeInputOf(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeInputOf(Collections.<Integer, Channel<?, ?>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeInputOf(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler().mergeInputOf(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeInputOf(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeInputOf(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .mergeInputOf(Collections.<Integer, Channel<?, ?>>singletonMap(0, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputFlow() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").close();
    assertThat(JRoutineChannels.channelHandler()
                               .outputFlowOf(channel, 33)
                               .in(seconds(1))
                               .all()).containsExactly(new Flow<String>(33, "test1"),
        new Flow<String>(33, "test2"), new Flow<String>(33, "test3"));
  }

  @Test
  public void testOutputFlowAbort() {

    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.pass("test1", "test2", "test3").abort();

    try {

      JRoutineChannels.channelHandler().outputFlowOf(channel, 33).in(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    final Routine<Flow<Object>, Flow<Object>> routine =
        JRoutineCore.routineOn(syncExecutor()).of(new Sort());
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, Flow<Object>> channel;
    channel = routine.invoke()
                     .pass(new Flow<Object>(Sort.STRING, "test21"),
                         new Flow<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineChannels.channelHandler()
                                 .outputOfFlow(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new Flow<Object>(Sort.INTEGER, -11),
                         new Flow<Object>(Sort.STRING, "test21"))
                     .close();
    channelMap = JRoutineChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
    channel = routine.invoke()
                     .pass(new Flow<Object>(Sort.STRING, "test21"),
                         new Flow<Object>(Sort.INTEGER, -11))
                     .close();
    channelMap = JRoutineChannels.channelHandler()
                                 .outputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
    assertThat(channelMap.get(Sort.INTEGER).in(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).in(seconds(1)).all()).containsOnly("test21");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    final Routine<Flow<Object>, Flow<Object>> routine = JRoutineCore.routine().of(new Sort());
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, Flow<Object>> channel;
    channel = routine.invoke()
                     .after(millis(100))
                     .pass(new Flow<Object>(Sort.STRING, "test21"),
                         new Flow<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineChannels.channelHandler()
                                 .outputOfFlow(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
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
                     .pass(new Flow<Object>(Sort.INTEGER, -11),
                         new Flow<Object>(Sort.STRING, "test21"));
    channelMap = JRoutineChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING);
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
                     .pass(new Flow<Object>(Sort.STRING, "test21"),
                         new Flow<Object>(Sort.INTEGER, -11));
    channelMap = JRoutineChannels.channelHandler()
                                 .outputOfFlow(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
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
        JRoutineCore.routine().of(factoryOf(Sort.class));
    final Channel<?, Flow<Object>> channel = routine.invoke();
    final Map<Integer, ? extends Channel<?, Object>> channelMap = JRoutineChannels.channelHandler()
                                                                                  .outputOfFlow(
                                                                                      channel,
                                                                                      Arrays.asList(
                                                                                          Sort.INTEGER,
                                                                                          Sort.STRING));
    assertThat(channelMap).isEqualTo(
        JRoutineChannels.channelHandler().outputOfFlow(channel, Sort.INTEGER, Sort.STRING));
  }

  @Test
  public void testOutputMapError() {

    try {
      JRoutineChannels.channelHandler()
                      .outputOfFlow(0, 0, JRoutineCore.routine().of(new Sort()).invoke());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputSelect() {

    final Channel<Flow<String>, Flow<String>> channel = JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineChannels.channelHandler().outputOfFlow(channel, 33).get(33);
    channel.pass(new Flow<String>(33, "test1"), new Flow<String>(-33, "test2"),
        new Flow<String>(33, "test3"), new Flow<String>(333, "test4"));
    channel.close();
    assertThat(outputChannel.in(seconds(1)).all()).containsExactly("test1", "test3");
  }

  @Test
  public void testOutputSelectAbort() {

    final Channel<Flow<String>, Flow<String>> channel = JRoutineCore.channel().ofType();
    final Channel<?, String> outputChannel =
        JRoutineChannels.channelHandler().outputOfFlow(channel, 33).get(33);
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

    final Channel<Flow<String>, Flow<String>> channel = JRoutineCore.channel().ofType();

    try {
      JRoutineChannels.channelHandler().outputOfFlow(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineChannels.channelHandler().outputOfFlow(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineChannels.channelHandler()
                      .outputOfFlow(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testReplayError() {

    try {
      JRoutineChannels.channelHandler().replayOutputOf(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSelectMap() {

    final Routine<Flow<Object>, Flow<Object>> routine = JRoutineCore.routine().of(new Sort());
    final Channel<Flow<Object>, Flow<Object>> inputChannel = JRoutineCore.channel().ofType();
    final Channel<?, Flow<Object>> outputChannel = routine.invoke().pass(inputChannel).close();
    final Channel<?, Object> intChannel = JRoutineChannels.channelHandler()
                                                          .withChannel()
                                                          .withLogLevel(Level.WARNING)
                                                          .configuration()
                                                          .outputOfFlow(outputChannel, Sort.INTEGER,
                                                              Sort.STRING)
                                                          .get(Sort.INTEGER);
    final Channel<?, Object> strChannel = JRoutineChannels.channelHandler()
                                                          .withChannel()
                                                          .withLogLevel(Level.WARNING)
                                                          .configuration()
                                                          .outputOfFlow(outputChannel, Sort.STRING,
                                                              Sort.INTEGER)
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

    final Routine<Flow<Object>, Flow<Object>> routine = JRoutineCore.routine().of(new Sort());
    Channel<Flow<Object>, Flow<Object>> inputChannel = JRoutineCore.channel().ofType();
    Channel<?, Flow<Object>> outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineChannels.channelHandler().outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING);
    inputChannel.after(millis(100))
                .pass(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      JRoutineChannels.channelHandler()
                      .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                      .get(Sort.STRING)
                      .in(seconds(1))
                      .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
                      .get(Sort.INTEGER)
                      .in(seconds(1))
                      .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.channel().ofType();
    outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineChannels.channelHandler().outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING);
    inputChannel.after(millis(100))
                .pass(new Flow<Object>(Sort.INTEGER, -11), new Flow<Object>(Sort.STRING, "test21"))
                .abort();

    try {

      JRoutineChannels.channelHandler()
                      .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                      .get(Sort.STRING)
                      .in(seconds(1))
                      .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER)
                      .get(Sort.INTEGER)
                      .in(seconds(1))
                      .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.channel().ofType();
    outputChannel = routine.invoke().pass(inputChannel).close();
    JRoutineChannels.channelHandler().outputOfFlow(outputChannel, Sort.STRING, Sort.INTEGER);
    inputChannel.after(millis(100))
                .pass(new Flow<Object>(Sort.STRING, "test21"), new Flow<Object>(Sort.INTEGER, -11))
                .abort();

    try {

      JRoutineChannels.channelHandler()
                      .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
                      .get(Sort.STRING)
                      .in(seconds(1))
                      .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      JRoutineChannels.channelHandler()
                      .outputOfFlow(outputChannel, Sort.INTEGER, Sort.STRING)
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
          JRoutineChannels.channelHandler().<Object, Integer>inputOfFlow(result, INTEGER).pass(
              flow.<Integer>data()).close();
          break;

        case STRING:
          JRoutineChannels.channelHandler().<Object, String>inputOfFlow(result, STRING).pass(
              flow.<String>data()).close();
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
