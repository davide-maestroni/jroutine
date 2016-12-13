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
  public void testBlend() {

    Channel<?, String> channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    Channel<?, String> channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(
        Channels.blend(channel2, channel1).buildChannel().inMax(seconds(1)).all()).containsOnly(
        "test1", "test2", "test3", "test4", "test5", "test6");
    channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(Channels.blend(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannel()
                       .inMax(seconds(1))
                       .all()).containsOnly("test1", "test2", "test3", "test4", "test5", "test6");
  }

  @Test
  public void testBlendAbort() {

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

      routine.call(Channels.blend(channel1, channel2).buildChannel()).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(Channels.blend(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
             .inMax(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testBlendError() {

    try {

      Channels.blend();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.blend((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.blend(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.blend(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.blend((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.blend(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testCombine() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    final Channel<Integer, Integer> channel2 =
        JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.combine(channel1, channel2)
            .buildChannel()
            .pass(new Selectable<String>("test1", 0))
            .pass(new Selectable<Integer>(1, 1))
            .close();
    Channels.combine(3, channel1, channel2)
            .buildChannel()
            .pass(new Selectable<String>("test2", 3))
            .pass(new Selectable<Integer>(2, 4))
            .close();
    Channels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(new Selectable<String>("test3", 0))
            .pass(new Selectable<Integer>(3, 1))
            .close();
    Channels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(new Selectable<String>("test4", -5))
            .pass(new Selectable<Integer>(4, -4))
            .close();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    Channels.combine(map)
            .buildChannel()
            .pass(new Selectable<String>("test5", 31))
            .pass(new Selectable<Integer>(5, 17))
            .close();
    assertThat(channel1.close().inMax(seconds(1)).all()).containsExactly("test1", "test2", "test3",
        "test4", "test5");
    assertThat(channel2.close().inMax(seconds(1)).all()).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void testCombineAbort() {

    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.combine(channel1, channel2).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.combine(3, channel1, channel2).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    Channels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<Integer>factoryOf()).call().sorted();
    final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
    map.put(31, channel1);
    map.put(17, channel2);
    Channels.combine(map).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testCombineError() {

    try {

      Channels.combine();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.combine(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.combine(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.combine(0, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.combine(Collections.<Integer, Channel<?, ?>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.combine(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.combine(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.combine(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.combine(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.combine(Collections.<Integer, Channel<?, ?>>singletonMap(0, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testConcat() {

    Channel<?, String> channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    Channel<?, String> channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(
        Channels.concat(channel2, channel1).buildChannel().inMax(seconds(1)).all()).containsExactly(
        "test4", "test5", "test6", "test1", "test2", "test3");
    channel1 = JRoutineCore.of("test1", "test2", "test3").buildChannel();
    channel2 = JRoutineCore.of("test4", "test5", "test6").buildChannel();
    assertThat(Channels.concat(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannel()
                       .inMax(seconds(1))
                       .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
        "test6");
  }

  @Test
  public void testConcatAbort() {

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

      routine.call(Channels.concat(channel1, channel2).buildChannel()).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(Channels.concat(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
             .inMax(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testConcatError() {

    try {

      Channels.concat();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.concat((Channel<?, ?>[]) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.concat(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.concat(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.concat((List<Channel<?, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.concat(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConfiguration() {
    final Channel<?, String> channel = JRoutineCore.of("test").buildChannel();
    final TestLog testLog = new TestLog();
    assertThat(Channels.selectableOutput(channel, 3)
                       .applyChannelConfiguration()
                       .withLog(testLog)
                       .withLogLevel(Level.DEBUG)
                       .configured()
                       .buildChannel()
                       .all()).containsExactly(new Selectable<String>("test", 3));
    assertThat(testLog.mLogCount).isGreaterThan(0);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConfigurationMap() {
    final Channel<Selectable<String>, Selectable<String>> channel =
        JRoutineCore.<Selectable<String>>ofInputs().buildChannel();
    final TestLog testLog = new TestLog();
    Channels.selectInput(3, 1, channel)
            .applyChannelConfiguration()
            .withLog(testLog)
            .withLogLevel(Level.DEBUG)
            .configured()
            .buildChannelMap()
            .get(3)
            .pass("test")
            .close();
    assertThat(channel.close().all()).containsExactly(new Selectable<String>("test", 3));
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
  public void testDistribute() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute(channel1, channel2)
            .buildChannel()
            .pass(Arrays.asList("test1-1", "test1-2"))
            .close();
    Channels.distribute(Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(Arrays.asList("test2-1", "test2-2"))
            .close();
    Channels.distribute(channel1, channel2)
            .buildChannel()
            .pass(Collections.singletonList("test3-1"))
            .close();
    assertThat(channel1.close().inMax(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().inMax(seconds(1)).all()).containsExactly("test1-2", "test2-2");
  }

  @Test
  public void testDistributeAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute(channel1, channel2).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testDistributeError() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute(channel1).buildChannel().pass(Arrays.asList("test1-1", "test1-2")).close();

    try {

      channel1.close().inMax(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      Channels.distribute();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.distribute(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.distribute(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.distribute(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testDistributePlaceHolderError() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute((Object) null, channel1)
            .buildChannel()
            .pass(Arrays.asList("test1-1", "test1-2"))
            .close();

    try {

      channel1.close().inMax(seconds(1)).all();

      fail();

    } catch (final InvocationException ignored) {

    }

    try {

      Channels.distribute(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.distribute(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.distribute(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.distribute(new Object(), Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testDistributePlaceholder() {

    final Channel<String, String> channel1 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    final Channel<String, String> channel2 =
        JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute((Object) null, channel1, channel2)
            .buildChannel()
            .pass(Arrays.asList("test1-1", "test1-2"))
            .close();
    final String placeholder = "placeholder";
    Channels.distribute((Object) placeholder, Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .pass(Arrays.asList("test2-1", "test2-2"))
            .close();
    Channels.distribute(placeholder, channel1, channel2)
            .buildChannel()
            .pass(Collections.singletonList("test3-1"))
            .close();
    assertThat(channel1.close().inMax(seconds(1)).all()).containsExactly("test1-1", "test2-1",
        "test3-1");
    assertThat(channel2.close().inMax(seconds(1)).all()).containsExactly("test1-2", "test2-2",
        placeholder);
  }

  @Test
  public void testDistributePlaceholderAbort() {

    Channel<String, String> channel1;
    Channel<String, String> channel2;
    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute((Object) null, channel1, channel2).buildChannel().abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    channel2 = JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call().sorted();
    Channels.distribute(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
            .buildChannel()
            .abort();

    try {

      channel1.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channel2.close().inMax(seconds(1)).next();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testInputMap() {

    final ArrayList<Selectable<Object>> outputs = new ArrayList<Selectable<Object>>();
    outputs.add(new Selectable<Object>("test21", Sort.STRING));
    outputs.add(new Selectable<Object>(-11, Sort.INTEGER));
    final Routine<Selectable<Object>, Selectable<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<Selectable<Object>, Selectable<Object>> channel;
    channel = routine.call();
    channelMap =
        Channels.selectInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().inMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap = Channels.selectInput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().inMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
    channel = routine.call();
    channelMap =
        Channels.selectInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).pass("test21").close();
    assertThat(channel.close().inMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
  }

  @Test
  public void testInputMapAbort() {

    final Routine<Selectable<Object>, Selectable<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Map<Integer, ? extends Channel<Object, ?>> channelMap;
    Channel<Selectable<Object>, Selectable<Object>> channel;
    channel = routine.call();
    channelMap =
        Channels.selectInput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    channelMap.get(Sort.INTEGER).pass(-11).close();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap = Channels.selectInput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).pass("test21").close();

    try {

      channel.close().inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call();
    channelMap =
        Channels.selectInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
    channelMap.get(Sort.INTEGER).abort();
    channelMap.get(Sort.STRING).abort();

    try {

      channel.close().inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testInputMapError() {

    try {
      Channels.selectInput(0, 0, JRoutineCore.with(new Sort()).call());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInputSelect() {

    final Channel<Selectable<String>, Selectable<String>> channel =
        JRoutineCore.<Selectable<String>>ofInputs().buildChannel();
    Channels.selectInput(channel, 33).buildChannel().pass("test1", "test2", "test3").close();
    assertThat(channel.close().inMax(seconds(1)).all()).containsExactly(
        new Selectable<String>("test1", 33), new Selectable<String>("test2", 33),
        new Selectable<String>("test3", 33));
  }

  @Test
  public void testInputSelectAbort() {

    final Channel<Selectable<String>, Selectable<String>> channel =
        JRoutineCore.<Selectable<String>>ofInputs().buildChannel();
    Channels.selectInput(channel, 33).buildChannel().pass("test1", "test2", "test3").abort();

    try {

      channel.close().inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testInputSelectError() {

    final Channel<Selectable<String>, Selectable<String>> channel =
        JRoutineCore.<Selectable<String>>ofInputs().buildChannel();

    try {
      Channels.selectInput(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.selectInput(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.selectInput(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInputToSelectable() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    Channels.selectableInput(channel, 33)
            .buildChannel()
            .pass(new Selectable<String>("test1", 33), new Selectable<String>("test2", -33),
                new Selectable<String>("test3", 33), new Selectable<String>("test4", 333))
            .close();
    assertThat(channel.close().inMax(seconds(1)).all()).containsExactly("test1", "test3");
  }

  @Test
  public void testInputToSelectableAbort() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    Channels.selectableInput(channel, 33).buildChannel().abort();

    try {

      channel.close().inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

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
    assertThat(routine.call(Channels.join(channel1, channel2).buildChannel())
                      .inMax(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(
        routine.call(Channels.join(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
               .inMax(seconds(10))
               .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.call(Channels.join(channel1, channel2).buildChannel())
                      .inMax(seconds(10))
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

      routine.call(Channels.join(channel1, channel2).buildChannel()).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(Channels.join(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
             .inMax(seconds(1))
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
      routine.call(Channels.join(channel1, channel2)
                           .applyChannelConfiguration()
                           .withBackoff(afterCount(0).constantDelay(millis(100)))
                           .withMaxSize(1)
                           .configured()
                           .buildChannel()).inMax(seconds(10)).all();
      fail();

    } catch (final DeadlockException ignored) {

    }
  }

  @Test
  public void testJoinError() {

    try {

      Channels.join();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.join(Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.join(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.join(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

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
    assertThat(routine.call(Channels.join(new Object(), channel1, channel2).buildChannel())
                      .inMax(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();
    assertThat(routine.call(
        Channels.join(null, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel())
                      .inMax(seconds(10))
                      .all()).containsExactly('s', '2');
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().after(millis(100)).pass("testtest").pass("test2").pass("test3").close();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(Channels.join(new Object(), channel1, channel2).buildChannel())
             .inMax(seconds(10))
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

      routine.call(Channels.join((Object) null, channel1, channel2).buildChannel())
             .inMax(seconds(1))
             .all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    channel1.sorted().abort();
    channel2.sorted().after(millis(110)).pass(6).pass(4).close();

    try {

      routine.call(Channels.join(new Object(), Arrays.<Channel<?, ?>>asList(channel1, channel2))
                           .buildChannel()).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testJoinPlaceholderError() {

    try {

      Channels.join(new Object());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.join(null, Collections.<Channel<?, ?>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.join(new Object(), new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.join(new Object(), Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
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

    final Channel<?, ? extends Selectable<Object>> channel =
        Channels.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    final Channel<?, Selectable<Object>> output = JRoutineCore.with(new Sort())
                                                              .applyInvocationConfiguration()
                                                              .withInputOrder(OrderType.SORTED)
                                                              .configured()
                                                              .call(channel);
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        Channels.selectOutput(output, Sort.INTEGER, Sort.STRING).buildChannelMap();

    for (int i = 0; i < 4; i++) {

      final String input = Integer.toString(i);
      channel1.after(millis(20)).pass(input);
      channel2.after(millis(20)).pass(i);
    }

    channel1.close();
    channel2.close();

    assertThat(channelMap.get(Sort.STRING).inMax(seconds(1)).all()).containsExactly("0", "1", "2",
        "3");
    assertThat(channelMap.get(Sort.INTEGER).inMax(seconds(1)).all()).containsExactly(0, 1, 2, 3);
  }

  @Test
  public void testMerge() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().applyChannelConfiguration()
                                       .withOrder(OrderType.SORTED)
                                       .configured();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().applyChannelConfiguration()
                                        .withOrder(OrderType.SORTED)
                                        .configured();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends Selectable<?>> outputChannel;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.merge(-7, channel1, channel2).buildChannel();
    channel1.pass("test1").close();
    channel2.pass(13).close();
    assertThat(outputChannel.inMax(seconds(1)).all()).containsOnly(
        new Selectable<String>("test1", -7), new Selectable<Integer>(13, -6));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        Channels.merge(11, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel2.pass(13).close();
    channel1.pass("test1").close();
    assertThat(outputChannel.inMax(seconds(1)).all()).containsOnly(
        new Selectable<String>("test1", 11), new Selectable<Integer>(13, 12));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.merge(channel1, channel2).buildChannel();
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.inMax(seconds(1)).all()).containsOnly(
        new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel1.pass("test2").close();
    channel2.pass(-17).close();
    assertThat(outputChannel.inMax(seconds(1)).all()).containsOnly(
        new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = Channels.merge(channelMap).buildChannel();
    channel1.pass("test3").close();
    channel2.pass(111).close();
    assertThat(outputChannel.inMax(seconds(1)).all()).containsOnly(
        new Selectable<String>("test3", 7), new Selectable<Integer>(111, -3));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMerge4() {

    final ChannelBuilder<String, String> builder =
        JRoutineCore.<String>ofInputs().applyChannelConfiguration()
                                       .withOrder(OrderType.SORTED)
                                       .configured();
    final Channel<String, String> channel1 = builder.buildChannel();
    final Channel<String, String> channel2 = builder.buildChannel();
    final Channel<String, String> channel3 = builder.buildChannel();
    final Channel<String, String> channel4 = builder.buildChannel();

    final Routine<Selectable<String>, String> routine =
        JRoutineCore.with(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
    final Channel<?, String> outputChannel = routine.call(
        Channels.merge(Arrays.asList(channel1, channel2, channel3, channel4)).buildChannel());

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

    assertThat(outputChannel.inMax(seconds(10)).all()).containsExactly("0", "1", "2", "3");
  }

  @Test
  public void testMergeAbort() {

    final ChannelBuilder<String, String> builder1 =
        JRoutineCore.<String>ofInputs().applyChannelConfiguration()
                                       .withOrder(OrderType.SORTED)
                                       .configured();
    final ChannelBuilder<Integer, Integer> builder2 =
        JRoutineCore.<Integer>ofInputs().applyChannelConfiguration()
                                        .withOrder(OrderType.SORTED)
                                        .configured();
    Channel<String, String> channel1;
    Channel<Integer, Integer> channel2;
    Channel<?, ? extends Selectable<?>> outputChannel;
    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.merge(-7, channel1, channel2).buildChannel();
    channel1.pass("test1").close();
    channel2.abort();

    try {

      outputChannel.inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel =
        Channels.merge(11, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel2.abort();
    channel1.pass("test1").close();

    try {

      outputChannel.inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.merge(channel1, channel2).buildChannel();
    channel1.abort();
    channel2.pass(-17).close();

    try {

      outputChannel.inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    outputChannel = Channels.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannel();
    channel1.pass("test2").close();
    channel2.abort();

    try {

      outputChannel.inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel1 = builder1.buildChannel();
    channel2 = builder2.buildChannel();
    final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
    channelMap.put(7, channel1);
    channelMap.put(-3, channel2);
    outputChannel = Channels.merge(channelMap).buildChannel();
    channel1.abort();
    channel2.pass(111).close();

    try {

      outputChannel.inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testMergeError() {

    try {

      Channels.merge(0, Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.merge(0);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.merge(Collections.<Channel<?, Object>>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.merge(Collections.<Integer, Channel<?, Object>>emptyMap());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.merge();

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      Channels.merge(new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.merge(Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.merge(0, new Channel[]{null});

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.merge(0, Collections.<Channel<?, ?>>singletonList(null));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      Channels.merge(Collections.<Integer, Channel<?, ?>>singletonMap(1, null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputMap() {

    final Routine<Selectable<Object>, Selectable<Object>> routine = JRoutineCore.with(new Sort())
                                                                                .applyInvocationConfiguration()
                                                                                .withRunner(
                                                                                    Runners
                                                                                        .syncRunner())
                                                                                .configured()
                                                                                .buildRoutine();
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, Selectable<Object>> channel;
    channel = routine.call(new Selectable<Object>("test21", Sort.STRING),
        new Selectable<Object>(-11, Sort.INTEGER));
    channelMap =
        Channels.selectOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).inMax(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).inMax(seconds(1)).all()).containsOnly("test21");
    channel = routine.call(new Selectable<Object>(-11, Sort.INTEGER),
        new Selectable<Object>("test21", Sort.STRING));
    channelMap = Channels.selectOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).inMax(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).inMax(seconds(1)).all()).containsOnly("test21");
    channel = routine.call(new Selectable<Object>("test21", Sort.STRING),
        new Selectable<Object>(-11, Sort.INTEGER));
    channelMap =
        Channels.selectOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
    assertThat(channelMap.get(Sort.INTEGER).inMax(seconds(1)).all()).containsOnly(-11);
    assertThat(channelMap.get(Sort.STRING).inMax(seconds(1)).all()).containsOnly("test21");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputMapAbort() {

    final Routine<Selectable<Object>, Selectable<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Map<Integer, ? extends Channel<?, Object>> channelMap;
    Channel<?, Selectable<Object>> channel;
    channel = routine.call()
                     .after(millis(100))
                     .pass(new Selectable<Object>("test21", Sort.STRING),
                         new Selectable<Object>(-11, Sort.INTEGER));
    channelMap =
        Channels.selectOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    channel.abort();

    try {

      channelMap.get(Sort.STRING).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call()
                     .after(millis(100))
                     .pass(new Selectable<Object>(-11, Sort.INTEGER),
                         new Selectable<Object>("test21", Sort.STRING));
    channelMap = Channels.selectOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    channel.abort();

    try {

      channelMap.get(Sort.STRING).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    channel = routine.call()
                     .after(millis(100))
                     .pass(new Selectable<Object>("test21", Sort.STRING),
                         new Selectable<Object>(-11, Sort.INTEGER));
    channelMap =
        Channels.selectOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).buildChannelMap();
    channel.abort();

    try {

      channelMap.get(Sort.STRING).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      channelMap.get(Sort.INTEGER).inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  public void testOutputMapCache() {

    final Routine<Selectable<Object>, Selectable<Object>> routine =
        JRoutineCore.with(factoryOf(Sort.class)).buildRoutine();
    final Channel<?, Selectable<Object>> channel = routine.call();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        Channels.selectOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannelMap();
    assertThat(channelMap).isEqualTo(
        Channels.selectOutput(channel, Sort.INTEGER, Sort.STRING).buildChannelMap());
  }

  @Test
  public void testOutputMapError() {

    try {
      Channels.selectOutput(0, 0, JRoutineCore.with(new Sort()).call());
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputSelect() {

    final Channel<Selectable<String>, Selectable<String>> channel =
        JRoutineCore.<Selectable<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        Channels.selectOutput(channel, 33).buildChannelMap().get(33);
    channel.pass(new Selectable<String>("test1", 33), new Selectable<String>("test2", -33),
        new Selectable<String>("test3", 33), new Selectable<String>("test4", 333));
    channel.close();
    assertThat(outputChannel.inMax(seconds(1)).all()).containsExactly("test1", "test3");
  }

  @Test
  public void testOutputSelectAbort() {

    final Channel<Selectable<String>, Selectable<String>> channel =
        JRoutineCore.<Selectable<String>>ofInputs().buildChannel();
    final Channel<?, String> outputChannel =
        Channels.selectOutput(channel, 33).buildChannelMap().get(33);
    channel.abort();

    try {

      outputChannel.inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testOutputSelectError() {

    final Channel<Selectable<String>, Selectable<String>> channel =
        JRoutineCore.<Selectable<String>>ofInputs().buildChannel();

    try {
      Channels.selectOutput(channel, (int[]) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.selectOutput(channel, (List<Integer>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Channels.selectOutput(channel, Collections.<Integer>singletonList(null));
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOutputToSelectable() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    channel.pass("test1", "test2", "test3").close();
    assertThat(Channels.selectableOutput(channel, 33)
                       .buildChannel()
                       .inMax(seconds(1))
                       .all()).containsExactly(new Selectable<String>("test1", 33),
        new Selectable<String>("test2", 33), new Selectable<String>("test3", 33));
  }

  @Test
  public void testOutputToSelectableAbort() {

    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    channel.pass("test1", "test2", "test3").abort();

    try {

      Channels.selectableOutput(channel, 33).buildChannel().inMax(seconds(1)).all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testReplayError() {

    try {
      Channels.replay(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSelectMap() {

    final Routine<Selectable<Object>, Selectable<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    final Channel<Selectable<Object>, Selectable<Object>> inputChannel =
        JRoutineCore.<Selectable<Object>>ofInputs().buildChannel();
    final Channel<?, Selectable<Object>> outputChannel = routine.call(inputChannel);
    final Channel<?, Object> intChannel =
        Channels.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                .applyChannelConfiguration()
                .withLogLevel(Level.WARNING)
                .configured()
                .buildChannelMap()
                .get(Sort.INTEGER);
    final Channel<?, Object> strChannel =
        Channels.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                .applyChannelConfiguration()
                .withLogLevel(Level.WARNING)
                .configured()
                .buildChannelMap()
                .get(Sort.STRING);
    inputChannel.pass(new Selectable<Object>("test21", Sort.STRING),
        new Selectable<Object>(-11, Sort.INTEGER));
    assertThat(intChannel.inMax(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.inMax(seconds(10)).next()).isEqualTo("test21");
    inputChannel.pass(new Selectable<Object>(-11, Sort.INTEGER),
        new Selectable<Object>("test21", Sort.STRING));
    assertThat(intChannel.inMax(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.inMax(seconds(10)).next()).isEqualTo("test21");
    inputChannel.pass(new Selectable<Object>("test21", Sort.STRING),
        new Selectable<Object>(-11, Sort.INTEGER));
    assertThat(intChannel.inMax(seconds(10)).next()).isEqualTo(-11);
    assertThat(strChannel.inMax(seconds(10)).next()).isEqualTo("test21");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSelectMapAbort() {

    final Routine<Selectable<Object>, Selectable<Object>> routine =
        JRoutineCore.with(new Sort()).buildRoutine();
    Channel<Selectable<Object>, Selectable<Object>> inputChannel =
        JRoutineCore.<Selectable<Object>>ofInputs().buildChannel();
    Channel<?, Selectable<Object>> outputChannel = routine.call(inputChannel);
    Channels.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    inputChannel.after(millis(100))
                .pass(new Selectable<Object>("test21", Sort.STRING),
                    new Selectable<Object>(-11, Sort.INTEGER))
                .abort();

    try {

      Channels.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER)
              .buildChannelMap()
              .get(Sort.STRING)
              .inMax(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      Channels.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
              .buildChannelMap()
              .get(Sort.INTEGER)
              .inMax(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.<Selectable<Object>>ofInputs().buildChannel();
    outputChannel = routine.call(inputChannel);
    Channels.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING).buildChannelMap();
    inputChannel.after(millis(100))
                .pass(new Selectable<Object>(-11, Sort.INTEGER),
                    new Selectable<Object>("test21", Sort.STRING))
                .abort();

    try {

      Channels.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER)
              .buildChannelMap()
              .get(Sort.STRING)
              .inMax(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      Channels.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER)
              .buildChannelMap()
              .get(Sort.INTEGER)
              .inMax(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.<Selectable<Object>>ofInputs().buildChannel();
    outputChannel = routine.call(inputChannel);
    Channels.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER).buildChannelMap();
    inputChannel.after(millis(100))
                .pass(new Selectable<Object>("test21", Sort.STRING),
                    new Selectable<Object>(-11, Sort.INTEGER))
                .abort();

    try {

      Channels.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
              .buildChannelMap()
              .get(Sort.STRING)
              .inMax(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }

    try {

      Channels.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
              .buildChannelMap()
              .get(Sort.INTEGER)
              .inMax(seconds(1))
              .all();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  private static class Amb<DATA> extends TemplateInvocation<Selectable<DATA>, DATA> {

    private static final int NO_INDEX = Integer.MIN_VALUE;

    private int mFirstIndex;

    @Override
    public void onInput(final Selectable<DATA> input, @NotNull final Channel<DATA, ?> result) {

      if (mFirstIndex == NO_INDEX) {

        mFirstIndex = input.index;
        result.pass(input.data);

      } else if (mFirstIndex == input.index) {

        result.pass(input.data);
      }
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

  private static class Sort extends MappingInvocation<Selectable<Object>, Selectable<Object>> {

    private static final int INTEGER = 1;

    private static final int STRING = 0;

    /**
     * Constructor.
     */
    protected Sort() {

      super(null);
    }

    public void onInput(final Selectable<Object> selectable,
        @NotNull final Channel<Selectable<Object>, ?> result) {

      switch (selectable.index) {

        case INTEGER:
          Channels.<Object, Integer>selectInput(result, INTEGER).buildChannel()
                                                                .pass(selectable.<Integer>data())
                                                                .close();
          break;

        case STRING:
          Channels.<Object, String>selectInput(result, STRING).buildChannel()
                                                              .pass(selectable.<String>data())
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
