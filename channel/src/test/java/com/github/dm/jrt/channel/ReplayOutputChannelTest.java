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
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.common.TimeoutException;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.DurationMeasure.days;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Replay channel unit tests.
 * <p>
 * Created by davide-maestroni on 10/26/2014.
 */
public class ReplayOutputChannelTest {

  @Test
  public void testChannel() {

    Channel<?, String> channel =
        JRoutineChannels.channelHandler().replayOutputOf(JRoutineCore.channel().of("test"));
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.abort()).isFalse();
    assertThat(channel.abort(null)).isFalse();
    assertThat(channel.close().isOpen()).isFalse();
    assertThat(channel.isEmpty()).isFalse();
    assertThat(channel.getComplete()).isTrue();
    assertThat(channel.isBound()).isFalse();
    final ArrayList<String> results = new ArrayList<String>();
    assertThat(channel.in(1, TimeUnit.SECONDS).hasNext()).isTrue();
    channel.inNoTime().allInto(results);
    assertThat(results).containsExactly("test");
    channel = JRoutineChannels.channelHandler()
                              .replayOutputOf(JRoutineCore.channel().of("test1", "test2", "test3"));

    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    assertThat(channel.skipNext(1).next(1)).containsExactly("test2");
    assertThat(channel.eventuallyContinue().next(4)).containsExactly("test3");
    assertThat(channel.eventuallyContinue().nextOrElse("test4")).isEqualTo("test4");

    Iterator<String> iterator = JRoutineChannels.channelHandler()
                                                .replayOutputOf(JRoutineCore.channel()
                                                                            .of("test1", "test2",
                                                                                "test3"))

                                                .iterator();
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("test1");

    try {
      iterator.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    iterator = JRoutineChannels.channelHandler()
                               .replayOutputOf(JRoutineCore.channel().of("test1", "test2", "test3"))

                               .expiringIterator();
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("test1");

    try {
      iterator.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    channel = JRoutineChannels.channelHandler()
                              .replayOutputOf(JRoutineCore.channel().<String>ofType().after(days(1))
                                                                                     .pass("test"));

    try {
      channel.eventuallyFail().next();
      fail();

    } catch (final TimeoutException ignored) {

    }

    try {
      channel.eventuallyContinue().next();
      fail();

    } catch (final NoSuchElementException ignored) {

    }

    try {
      channel.eventuallyAbort().next();
      fail();

    } catch (final AbortException ignored) {

    }

    try {
      channel.eventuallyAbort(new IllegalArgumentException()).next();
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isNull();
    }

    channel = JRoutineChannels.channelHandler()
                              .replayOutputOf(
                                  JRoutineCore.channel().<String>ofType().after(seconds(1))
                                                                         .pass("test"));

    try {
      channel.eventuallyAbort(new IllegalArgumentException()).next();
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInvalidCalls() {
    final Channel<String, String> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Object, String> channel =
        (Channel<Object, String>) JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    try {
      channel.sorted().pass("test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.unsorted().pass("test", "test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.pass(Collections.singleton("test"));
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.pass(JRoutineCore.channel().ofType());
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  @Test
  public void testReplay() {

    final Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    final Channel<?, Object> channel =
        JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    assertThat(channel.isBound()).isFalse();
    assertThat(channel.isEmpty()).isTrue();
    inputChannel.pass("test1", "test2");
    final Channel<Object, Object> output1 = JRoutineCore.channel().ofType();
    channel.pipe(output1);
    assertThat(output1.next()).isEqualTo("test1");
    final Channel<Object, Object> output2 = JRoutineCore.channel().ofType();
    channel.pipe(output2);
    assertThat(channel.isOpen()).isFalse();
    channel.close();
    assertThat(channel.isOpen()).isFalse();
    inputChannel.pass("test3").close();
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.getComplete()).isTrue();
    channel.pipe(output1);
    assertThat(output2.close().all()).containsExactly("test1", "test2", "test3");
    assertThat(output1.close().all()).containsExactly("test2", "test3");
    assertThat(channel.isEmpty()).isFalse();
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testReplayAbort() {

    Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    Channel<?, Object> channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    inputChannel.pass("test1", "test2");
    final Channel<Object, Object> output1 = JRoutineCore.channel().ofType();
    channel.pipe(output1);
    assertThat(output1.next()).isEqualTo("test1");
    final Channel<Object, Object> output2 = JRoutineCore.channel().ofType();
    channel.pipe(output2);
    inputChannel.abort();

    try {
      output1.all();
      fail();

    } catch (final AbortException ignored) {

    }

    try {
      output2.all();
      fail();

    } catch (final AbortException ignored) {

    }

    inputChannel = JRoutineCore.channel().ofType();
    channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    inputChannel.pass("test").close();
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onOutput(final Object output) throws Exception {

        throw new IllegalAccessError();
      }
    });
    assertThat(channel.getError()).isNull();
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testReplayAbortException() {

    Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    Channel<?, Object> channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onError(@NotNull final RoutineException error) throws Exception {

        throw new UnsupportedOperationException();
      }
    });
    inputChannel.abort();
    assertThat(channel.getError().getCause()).isNull();
    inputChannel = JRoutineCore.channel().ofType();
    channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onError(@NotNull final RoutineException error) throws Exception {

        throw new RoutineException();
      }
    });
    inputChannel.abort();
    assertThat(channel.getError().getCause()).isNull();

    inputChannel = JRoutineCore.channel().ofType();
    channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    inputChannel.abort();
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onError(@NotNull final RoutineException error) throws Exception {

        throw new UnsupportedOperationException();
      }
    });
    assertThat(channel.getError().getCause()).isNull();
    inputChannel = JRoutineCore.channel().ofType();
    channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    inputChannel.abort();
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onError(@NotNull final RoutineException error) throws Exception {

        throw new RoutineException();
      }
    });
    assertThat(channel.getError().getCause()).isNull();
  }

  @Test
  public void testReplayConsumer() {

    final Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    final Channel<?, Object> channel =
        JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    assertThat(channel.isBound()).isFalse();
    assertThat(channel.isEmpty()).isTrue();
    inputChannel.pass("test1", "test2");
    final ArrayList<Object> outputs = new ArrayList<Object>();
    final TemplateChannelConsumer<Object> consumer = new TemplateChannelConsumer<Object>() {

      @Override
      public void onOutput(final Object output) throws Exception {

        outputs.add(output);
      }
    };
    channel.consume(consumer);
    assertThat(channel.isOpen()).isFalse();
    inputChannel.pass("test3").close();
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.getComplete()).isTrue();
    channel.consume(consumer);
    assertThat(outputs).containsExactly("test1", "test2", "test3");
    assertThat(channel.isEmpty()).isFalse();
  }

  @Test
  public void testReplayError() {

    final Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    final Channel<?, Object> channel =
        JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    channel.eventuallyContinue();
    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    channel.eventuallyAbort();
    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    channel.eventuallyAbort(new NullPointerException());
    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    channel.eventuallyFail();
    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    channel.inNoTime();
    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    channel.in(seconds(3));
    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    channel.in(3, TimeUnit.SECONDS);
    try {
      channel.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }
  }

  @Test
  public void testReplayException() {

    Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType();
    Channel<?, Object> channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onComplete() throws Exception {

        throw new UnsupportedOperationException();
      }
    });
    inputChannel.pass("test").close().throwError();
    inputChannel = JRoutineCore.channel().ofType();
    channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onComplete() throws Exception {

        throw new RoutineException();
      }
    });
    inputChannel.pass("test").close().throwError();

    inputChannel = JRoutineCore.channel().ofType();
    channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    inputChannel.pass("test").close();
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onComplete() throws Exception {

        throw new UnsupportedOperationException();
      }
    });
    channel.throwError();
    inputChannel = JRoutineCore.channel().ofType();
    channel = JRoutineChannels.channelHandler().replayOutputOf(inputChannel);
    inputChannel.pass("test").close();
    channel.consume(new TemplateChannelConsumer<Object>() {

      @Override
      public void onComplete() throws Exception {

        throw new RoutineException();
      }
    });
    channel.throwError();
  }

  @Test
  public void testSize() {

    final Channel<Object, Object> channel =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    assertThat(channel.inputSize()).isEqualTo(0);
    channel.after(millis(500)).pass("test");
    assertThat(channel.inputSize()).isEqualTo(1);
    assertThat(channel.outputSize()).isEqualTo(0);
    final Channel<?, Object> result =
        JRoutineChannels.channelHandler().replayOutputOf(channel.afterNoDelay().close());
    assertThat(result.in(seconds(1)).getComplete()).isTrue();
    assertThat(result.inputSize()).isEqualTo(0);
    assertThat(result.outputSize()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.skipNext(1).outputSize()).isEqualTo(0);
  }
}
