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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.common.DeadlockException;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.common.BackoffBuilder.noDelay;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Channel unit tests.
 * <p>
 * Created by davide-maestroni on 10/26/2014.
 */
public class ChannelTest {

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testAbort() {
    final DurationMeasure timeout = seconds(1);
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.abort(new IllegalStateException());
    try {
      channel.in(timeout).throwError();

    } catch (final AbortException ex) {
      assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testAbortDelay() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test");
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    final ArrayList<String> results = new ArrayList<String>();
    channel.in(10, TimeUnit.MILLISECONDS).allInto(results);
    assertThat(results).isEmpty();
    assertThat(channel.inNoTime().eventuallyContinue().getComplete()).isFalse();
    assertThat(channel.afterNoDelay().abort()).isTrue();
    try {
      channel.next();
      fail();

    } catch (final AbortException ignored) {
    }

    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testAllIntoTimeout() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyAbort().eventuallyFail();
    try {
      channel.allInto(new ArrayList<String>());
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAllIntoTimeout2() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.allInto(new ArrayList<String>());
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAllTimeout() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.all();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAllTimeout2() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.all();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAsynchronousInput() {
    final DurationMeasure timeout = seconds(1);
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    new Thread() {

      @Override
      public void run() {
        try {
          Thread.sleep(200);

        } catch (final InterruptedException ignored) {

        } finally {
          channel.pass("test").close();
        }
      }
    }.start();
    final Channel<String, String> outputChannel = JRoutineCore.routine()
                                                              .of(IdentityInvocation
                                                                  .<String>factory())
                                                              .invoke()
                                                              .pass(channel)
                                                              .close();
    assertThat(outputChannel.in(timeout).next()).isEqualTo("test");
    assertThat(outputChannel.getComplete()).isTrue();
  }

  @Test
  public void testAsynchronousInput2() {
    final DurationMeasure timeout = seconds(1);
    final Channel<String, String> channel1 =
        JRoutineCore.channel().withChannel().withOrder(OrderType.SORTED).configuration().ofType();
    new Thread() {

      @Override
      public void run() {
        channel1.after(1, TimeUnit.MILLISECONDS)
                .after(millis(200))
                .pass("test1", "test2")
                .pass(Collections.singleton("test3"))
                .afterNoDelay()
                .close();
      }
    }.start();
    final Channel<String, String> outputChannel1 = JRoutineCore.routine()
                                                               .of(IdentityInvocation
                                                                   .<String>factory())
                                                               .invoke()
                                                               .pass(channel1)
                                                               .close();
    assertThat(outputChannel1.in(timeout).all()).containsExactly("test1", "test2", "test3");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testConfigurationErrors() {
    try {
      new DefaultChannelBuilder(defaultExecutor()).withConfiguration(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testDelayedClose() {
    final DurationMeasure timeout = seconds(1);
    final Channel<String, String> channel1 = JRoutineCore.channel().ofType();
    channel1.after(seconds(2)).close();
    assertThat(channel1.afterNoDelay().pass("test").in(timeout).next()).isEqualTo("test");
    assertThat(channel1.isOpen()).isTrue();
    final Channel<String, String> channel2 = JRoutineCore.channel().ofType();
    channel2.after(millis(100)).close();
    assertThat(channel2.after(millis(200)).pass("test").in(timeout).all()).containsExactly("test");
    final Channel<String, String> channel3 = JRoutineCore.channel().ofType();
    channel3.after(millis(200)).close();
    assertThat(channel3.afterNoDelay().pass("test").in(timeout).all()).containsExactly("test");
  }

  @Test
  public void testDelayedConsumer() {
    final Channel<String, String> channel1 = JRoutineCore.channel().ofType();
    final Channel<String, String> channel2 = JRoutineCore.channel().ofType();
    channel2.after(millis(300)).pass(channel1).afterNoDelay().close();
    channel1.pass("test").close();
    long startTime = System.currentTimeMillis();
    assertThat(channel2.in(seconds(1)).all()).containsExactly("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(300);
    final Channel<String, String> channel3 = JRoutineCore.channel().ofType();
    final Channel<String, String> channel4 = JRoutineCore.channel().ofType();
    channel4.after(millis(300)).pass(channel3).afterNoDelay().close();
    startTime = System.currentTimeMillis();
    channel3.abort();
    assertThat(channel4.in(seconds(1)).getComplete()).isTrue();
    assertThat(channel4.getError()).isNotNull();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(300);
  }

  @Test
  public void testEmpty() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.pass("test").isEmpty()).isFalse();
    channel.in(seconds(1)).next();
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.after(millis(100)).pass("test").isEmpty()).isFalse();
    assertThat(channel.close().in(seconds(10)).getComplete()).isTrue();
    assertThat(channel.isEmpty()).isFalse();
  }

  @Test
  public void testEmptyAbort() {
    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.pass("test").isEmpty()).isFalse();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.isEmpty()).isFalse();
  }

  @Test
  public void testGet() {
    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    try {
      channel.pass("test1", "test2").eventuallyContinue().get();
      fail();

    } catch (final NoSuchElementException ignored) {
    }

    assertThat(channel.close().get()).isEqualTo("test2");
  }

  @Test
  public void testHasNextIteratorTimeout() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.iterator().hasNext();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testHasNextIteratorTimeout2() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.iterator().hasNext();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testIllegalBind() {
    final Channel<Object, Object> invocationChannel =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    invocationChannel.consume(new TemplateChannelConsumer<Object>() {});
    try {
      channel.pass(invocationChannel);
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  @Test
  public void testMaxSize() {
    try {
      JRoutineCore.channel()
                  .withChannel()
                  .withMaxSize(1)
                  .configuration()
                  .ofType()
                  .pass("test1", "test2");
      fail();

    } catch (final DeadlockException ignored) {
    }
  }

  @Test
  public void testNextIteratorTimeout() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.iterator().next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testNextIteratorTimeout2() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.iterator().next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testNextList() {
    assertThat(JRoutineCore.channel()
                           .ofType()
                           .pass("test1", "test2", "test3", "test4")
                           .close()
                           .in(seconds(1))
                           .next(2)).containsExactly("test1", "test2");
    assertThat(JRoutineCore.channel()
                           .ofType()
                           .pass("test1")
                           .close()
                           .eventuallyContinue()
                           .in(seconds(1))
                           .next(2)).containsExactly("test1");
    try {
      JRoutineCore.channel().ofType().pass("test1").eventuallyAbort().in(seconds(1)).next(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.channel()
                  .ofType()
                  .pass("test1")
                  .eventuallyAbort(new IllegalStateException())
                  .in(seconds(1))
                  .next(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.channel().ofType().pass("test1").eventuallyFail().in(seconds(1)).next(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testNextOr() {
    assertThat(
        JRoutineCore.channel().ofType().pass("test1").in(seconds(1)).nextOrElse(2)).isEqualTo(
        "test1");
    assertThat(JRoutineCore.channel()
                           .ofType()
                           .eventuallyContinue()
                           .in(seconds(1))
                           .nextOrElse(2)).isEqualTo(2);
    try {
      JRoutineCore.channel().ofType().eventuallyAbort().in(millis(100)).nextOrElse("test2");
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.channel()
                  .ofType()
                  .eventuallyAbort(new IllegalStateException())
                  .in(millis(100))
                  .nextOrElse("test2");
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.channel().ofType().eventuallyFail().in(millis(100)).nextOrElse("test2");
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testNextTimeout() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testNextTimeout2() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.after(seconds(3)).pass("test").close();
    assertThat(channel.eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testOf() {
    final Channel<?, Integer> channel = JRoutineCore.channel().of(2);
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.in(seconds(1)).all()).containsExactly(2);
    assertThat(JRoutineCore.channel().of().in(seconds(1)).all()).isEmpty();
    assertThat(JRoutineCore.channel().of(-11, 73).in(seconds(1)).all()).containsExactly(-11, 73);
    assertThat(
        JRoutineCore.channel().of(Arrays.asList(3, 12, -7)).in(seconds(1)).all()).containsExactly(3,
        12, -7);
    assertThat(JRoutineCore.channel().of((Object[]) null).all()).isEmpty();
    assertThat(JRoutineCore.channel().of((List<Object>) null).all()).isEmpty();
  }

  @Test
  public void testOrderType() {
    final DurationMeasure timeout = seconds(1);
    final Channel<Object, Object> channel = JRoutineCore.channel()
                                                        .withChannel()
                                                        .withOrder(OrderType.SORTED)
                                                        .withMaxSize(1)
                                                        .withBackoff(noDelay())
                                                        .withLogLevel(Level.DEBUG)
                                                        .withLog(new NullLog())
                                                        .configuration()
                                                        .ofType();
    channel.pass(-77L);
    assertThat(channel.in(timeout).next()).isEqualTo(-77L);
    final Channel<Object, Object> channel1 = JRoutineCore.channel().ofType();
    channel1.after(millis(200)).pass(23).afterNoDelay().pass(-77L).close();
    assertThat(channel1.in(timeout).all()).containsOnly(23, -77L);
    final Channel<Object, Object> channel2 = JRoutineCore.channel().ofType();
    channel2.unsorted().sorted();
    channel2.after(millis(200)).pass(23).afterNoDelay().pass(-77L).close();
    assertThat(channel2.in(timeout).all()).containsExactly(23, -77L);
  }

  @Test
  public void testPartialOut() {
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    new Thread() {

      @Override
      public void run() {
        channel.pass("test");
      }
    }.start();
    final long startTime = System.currentTimeMillis();
    final Channel<String, String> outputChannel = JRoutineCore.routine()
                                                              .of(IdentityInvocation
                                                                  .<String>factory())
                                                              .invoke()
                                                              .pass(channel)
                                                              .close()
                                                              .eventuallyContinue();
    assertThat(outputChannel.in(millis(500)).all()).containsExactly("test");
    assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);
    assertThat(outputChannel.inNoTime().getComplete()).isFalse();
    channel.close();
    assertThat(channel.isOpen()).isFalse();
    assertThat(outputChannel.in(millis(500)).getComplete()).isTrue();
  }

  @Test
  public void testPassTimeout() {
    final Channel<Object, Object> channel1 = JRoutineCore.channel()
                                                         .withChannel()
                                                         .withOutputTimeout(millis(10))
                                                         .withOutputTimeoutAction(
                                                             TimeoutActionType.CONTINUE)
                                                         .configuration()
                                                         .ofType();
    assertThat(channel1.all()).isEmpty();
  }

  @Test
  public void testPassTimeout2() {
    final Channel<Object, Object> channel2 = JRoutineCore.channel()
                                                         .withChannel()
                                                         .withOutputTimeout(millis(10))
                                                         .withOutputTimeoutAction(
                                                             TimeoutActionType.ABORT)
                                                         .configuration()
                                                         .ofType();
    try {
      channel2.all();
      fail();

    } catch (final AbortException ignored) {
    }
  }

  @Test
  public void testPassTimeout3() {
    final Channel<Object, Object> channel3 = JRoutineCore.channel()
                                                         .withChannel()
                                                         .withOutputTimeout(millis(10))
                                                         .withOutputTimeoutAction(
                                                             TimeoutActionType.FAIL)
                                                         .configuration()
                                                         .ofType();
    try {
      channel3.all();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }
  }

  @Test
  public void testPendingInputs() {
    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    assertThat(channel.isOpen()).isTrue();
    channel.pass("test");
    assertThat(channel.isOpen()).isTrue();
    channel.after(millis(500)).pass("test");
    assertThat(channel.isOpen()).isTrue();
    final Channel<Object, Object> outputChannel = JRoutineCore.channel().ofType();
    channel.pass(outputChannel);
    assertThat(channel.isOpen()).isTrue();
    channel.afterNoDelay().close();
    assertThat(channel.isOpen()).isFalse();
    outputChannel.close();
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testPendingInputsAbort() {
    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    assertThat(channel.isOpen()).isTrue();
    channel.pass("test");
    assertThat(channel.isOpen()).isTrue();
    channel.after(millis(500)).pass("test");
    assertThat(channel.isOpen()).isTrue();
    final Channel<Object, Object> outputChannel = JRoutineCore.channel().ofType();
    channel.pass(outputChannel);
    assertThat(channel.isOpen()).isTrue();
    channel.afterNoDelay().abort();
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testReadFirst() {
    final DurationMeasure timeout = seconds(1);
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    new WeakThread(channel).start();
    final Channel<String, String> outputChannel = JRoutineCore.routine()
                                                              .of(IdentityInvocation
                                                                  .<String>factory())
                                                              .invoke()
                                                              .pass(channel)
                                                              .close();
    assertThat(outputChannel.in(timeout).next()).isEqualTo("test");
  }

  @Test
  public void testSize() {
    final Channel<Object, Object> channel =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    assertThat(channel.size()).isEqualTo(0);
    channel.after(millis(500)).pass("test");
    assertThat(channel.size()).isEqualTo(1);
    channel.afterNoDelay().close();
    assertThat(channel.in(seconds(1)).getComplete()).isTrue();
    assertThat(channel.size()).isEqualTo(1);
    assertThat(channel.skipNext(1).size()).isEqualTo(0);

    final Channel<Object, Object> channel1 = JRoutineCore.channel().ofType();
    assertThat(channel1.size()).isEqualTo(0);
    channel1.after(millis(500)).pass("test");
    assertThat(channel1.size()).isEqualTo(1);
    channel1.afterNoDelay().close();
    assertThat(channel1.in(seconds(1)).getComplete()).isTrue();
    assertThat(channel1.size()).isEqualTo(1);
    assertThat(channel1.skipNext(1).size()).isEqualTo(0);
  }

  @Test
  public void testSkip() {
    assertThat(JRoutineCore.channel()
                           .ofType()
                           .pass("test1", "test2", "test3", "test4")
                           .close()
                           .in(seconds(1))
                           .skipNext(2)
                           .all()).containsExactly("test3", "test4");
    assertThat(JRoutineCore.channel()
                           .ofType()
                           .pass("test1")
                           .close()
                           .eventuallyContinue()
                           .in(seconds(1))
                           .skipNext(2)
                           .all()).isEmpty();
    try {
      JRoutineCore.channel()
                  .ofType()
                  .pass("test1")
                  .close()
                  .eventuallyAbort()
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.channel()
                  .ofType()
                  .pass("test1")
                  .close()
                  .eventuallyAbort(new IllegalStateException())
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.channel()
                  .ofType()
                  .pass("test1")
                  .close()
                  .eventuallyFail()
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @SuppressWarnings("unused")
  private static class CountLog implements Log {

    private int mDgbCount;

    private int mErrCount;

    private int mWrnCount;

    public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mDgbCount;
    }

    public void err(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mErrCount;
    }

    public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {
      ++mWrnCount;
    }

    public int getDgbCount() {
      return mDgbCount;
    }

    public int getErrCount() {
      return mErrCount;
    }

    public int getWrnCount() {
      return mWrnCount;
    }
  }

  private static class WeakThread extends Thread {

    private final WeakReference<Channel<String, String>> mChannelRef;

    public WeakThread(final Channel<String, String> channel) {
      mChannelRef = new WeakReference<Channel<String, String>>(channel);
    }

    @Override
    public void run() {
      try {
        Thread.sleep(500);

      } catch (final InterruptedException ignored) {

      } finally {
        final Channel<String, String> channel = mChannelRef.get();
        if (channel != null) {
          channel.pass("test");
        }
      }
    }
  }
}
