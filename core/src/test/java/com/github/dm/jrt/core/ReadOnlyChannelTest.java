package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by davide-maestroni on 05/05/2017.
 */
public class ReadOnlyChannelTest {

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testAbort() {
    final DurationMeasure timeout = seconds(1);
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
  public void testBound() {
    final Channel<Object, Object> channel = JRoutineCore.readOnly(JRoutineCore.channel().ofType());
    assertThat(channel.isBound()).isFalse();
    assertThat(channel.consume(new TemplateChannelConsumer<Object>() {}).isBound()).isTrue();
  }

  @Test
  public void testClose() {
    final Channel<String, String> wrapped =
        JRoutineCore.channel().<String>ofType().after(millis(200)).pass("test");
    final Channel<String, String> channel = JRoutineCore.readOnly(wrapped);
    assertThat(channel.isOpen()).isTrue();
    assertThat(channel.close().isOpen()).isTrue();
    channel.after(10, TimeUnit.MILLISECONDS).close();
    assertThat(channel.in(millis(100)).getComplete()).isFalse();
    assertThat(channel.isOpen()).isTrue();
  }

  @Test
  public void testConsumer() {
    final Channel<?, String> channel1 = JRoutineCore.readOnly(JRoutineCore.channel().of("test"));
    final AtomicBoolean isClosed = new AtomicBoolean(false);
    final ArrayList<String> outputs = new ArrayList<String>();
    channel1.consume(new TemplateChannelConsumer<String>() {

      public void onComplete() {
        isClosed.set(true);
      }

      public void onOutput(final String output) {
        outputs.add(output);
      }
    });
    assertThat(isClosed.get()).isTrue();
    assertThat(outputs).containsOnly("test");
  }

  @Test
  public void testDelayedClose() {
    final DurationMeasure timeout = seconds(1);
    final Channel<?, String> channel1 = JRoutineCore.readOnly(JRoutineCore.channel().of("test"));
    channel1.after(seconds(2)).close();
    assertThat(channel1.afterNoDelay().in(timeout).next()).isEqualTo("test");
    assertThat(channel1.isOpen()).isFalse();
    final Channel<String, String> wrapped =
        JRoutineCore.channel().<String>ofType().after(millis(200)).pass("test");
    final Channel<String, String> channel2 = JRoutineCore.readOnly(wrapped);
    wrapped.after(100, TimeUnit.MILLISECONDS).close();
    assertThat(channel2.in(timeout.value, timeout.unit).all()).containsExactly("test");
  }

  @Test
  public void testEmpty() {
    final Channel<String, String> wrapped = JRoutineCore.channel().ofType();
    final Channel<String, String> channel = JRoutineCore.readOnly(wrapped);
    assertThat(channel.isEmpty()).isTrue();
    wrapped.pass("test");
    assertThat(channel.isEmpty()).isFalse();
    channel.in(seconds(1)).next();
    assertThat(channel.isEmpty()).isTrue();
    wrapped.pass("test").after(millis(100)).pass("test");
    assertThat(channel.isEmpty()).isFalse();
    assertThat(wrapped.close().in(seconds(10)).getComplete()).isTrue();
    assertThat(channel.isEmpty()).isFalse();
  }

  @Test
  public void testExpiringIterator() {
    final Channel<Object, Object> wrapped = JRoutineCore.channel().ofType();
    final Channel<Object, Object> channel = JRoutineCore.readOnly(wrapped);
    assertThat(channel.eventuallyContinue().hasNext()).isFalse();
    assertThat(channel.expiringIterator().hasNext()).isFalse();
    wrapped.after(millis(100)).pass("test");
    assertThat(channel.eventuallyFail().in(seconds(1)).expiringIterator().next()).isEqualTo("test");
  }

  @Test
  public void testHasNextIteratorTimeout() {
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
  public void testNextIteratorTimeout() {
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of("test1", "test2", "test3", "test4"))
                           .close()
                           .in(seconds(1))
                           .next(2)).containsExactly("test1", "test2");
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of("test1"))
                           .close()
                           .eventuallyContinue()
                           .in(seconds(1))
                           .next(2)).containsExactly("test1");
    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType().pass("test1"))
                  .eventuallyAbort()
                  .in(seconds(1))
                  .next(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType().pass("test1"))
                  .eventuallyAbort(new IllegalStateException())
                  .in(seconds(1))
                  .next(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType().pass("test1"))
                  .eventuallyFail()
                  .in(seconds(1))
                  .next(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testNextOr() {
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().<Object>of("test1"))
                           .in(seconds(1))
                           .nextOrElse(2)).isEqualTo("test1");
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().ofType())
                           .eventuallyContinue()
                           .in(seconds(1))
                           .nextOrElse(2)).isEqualTo(2);
    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType())
                  .eventuallyAbort()
                  .in(millis(100))
                  .nextOrElse("test2");
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType())
                  .eventuallyAbort(new IllegalStateException())
                  .in(millis(100))
                  .nextOrElse("test2");
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType())
                  .eventuallyFail()
                  .in(millis(100))
                  .nextOrElse("test2");
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testNextTimeout() {
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<String, String> channel =
        JRoutineCore.readOnly(JRoutineCore.channel().<String>ofType());
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
    final Channel<?, Integer> channel = JRoutineCore.readOnly(JRoutineCore.channel().of(2))
                                                    .pipe(JRoutineCore.channel().<Integer>ofType())
                                                    .close();
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.in(seconds(1)).all()).containsExactly(2);
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of())
                           .pipe(JRoutineCore.channel().ofType())
                           .close()
                           .in(seconds(1))
                           .all()).isEmpty();
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of(-11, 73))
                           .pipe(JRoutineCore.channel().<Integer>ofType())
                           .close()
                           .in(seconds(1))
                           .all()).containsExactly(-11, 73);
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of(Arrays.asList(3, 12, -7)))
                           .pipe(JRoutineCore.channel().<Integer>ofType())
                           .close()
                           .in(seconds(1))
                           .all()).containsExactly(3, 12, -7);
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of((Object[]) null))
                           .pipe(JRoutineCore.channel().ofType())
                           .close()
                           .all()).isEmpty();
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of((List<Object>) null))
                           .pipe(JRoutineCore.channel().ofType())
                           .close()
                           .all()).isEmpty();
  }

  @Test
  public void testPassTimeout() {
    final Channel<Object, Object> channel1 = JRoutineCore.readOnly(JRoutineCore.channel()
                                                                               .withChannel()
                                                                               .withOutputTimeout(
                                                                                   millis(10))
                                                                               .withOutputTimeoutAction(
                                                                                   TimeoutActionType.CONTINUE)
                                                                               .configuration()
                                                                               .ofType());
    assertThat(channel1.all()).isEmpty();
  }

  @Test
  public void testPassTimeout2() {
    final Channel<Object, Object> channel2 = JRoutineCore.readOnly(JRoutineCore.channel()
                                                                               .withChannel()
                                                                               .withOutputTimeout(
                                                                                   millis(10))
                                                                               .withOutputTimeoutAction(
                                                                                   TimeoutActionType.ABORT)
                                                                               .configuration()
                                                                               .ofType());
    try {
      channel2.all();
      fail();

    } catch (final AbortException ignored) {
    }
  }

  @Test
  public void testSize() {
    final Channel<Object, Object> wrapped = JRoutineCore.channel().ofType();
    final Channel<Object, Object> channel = JRoutineCore.readOnly(wrapped);
    assertThat(channel.inputSize()).isEqualTo(0);
    assertThat(channel.outputSize()).isEqualTo(0);
    wrapped.after(millis(500)).pass("test");
    assertThat(channel.inputSize()).isEqualTo(1);
    assertThat(channel.outputSize()).isEqualTo(1);
    wrapped.afterNoDelay().close();
    assertThat(channel.in(seconds(1)).getComplete()).isTrue();
    assertThat(channel.inputSize()).isEqualTo(1);
    assertThat(channel.outputSize()).isEqualTo(1);
    assertThat(channel.size()).isEqualTo(1);
    assertThat(channel.skipNext(1).outputSize()).isEqualTo(0);

    final Channel<Object, Object> wrapped1 = JRoutineCore.channel().ofType();
    final Channel<Object, Object> channel1 = JRoutineCore.readOnly(wrapped1);
    assertThat(channel1.inputSize()).isEqualTo(0);
    assertThat(channel1.outputSize()).isEqualTo(0);
    wrapped1.after(millis(500)).pass("test");
    assertThat(channel1.inputSize()).isEqualTo(1);
    assertThat(channel1.outputSize()).isEqualTo(1);
    wrapped1.afterNoDelay().close();
    assertThat(channel1.in(seconds(1)).getComplete()).isTrue();
    assertThat(channel1.inputSize()).isEqualTo(1);
    assertThat(channel1.outputSize()).isEqualTo(1);
    assertThat(channel1.size()).isEqualTo(1);
    assertThat(channel1.skipNext(1).outputSize()).isEqualTo(0);
  }

  @Test
  public void testSkip() {
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of("test1", "test2", "test3", "test4"))
                           .in(seconds(1))
                           .skipNext(2)
                           .all()).containsExactly("test3", "test4");
    assertThat(JRoutineCore.readOnly(JRoutineCore.channel().of("test1"))
                           .eventuallyContinue()
                           .in(seconds(1))
                           .skipNext(2)
                           .all()).isEmpty();
    try {
      JRoutineCore.readOnly(JRoutineCore.channel().of("test1"))
                  .eventuallyAbort()
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().of("test1"))
                  .eventuallyAbort(new IllegalStateException())
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().of("test1"))
                  .eventuallyFail()
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testWrite() {
    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType()).pass("test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType()).pass("test", "test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType()).pass(Collections.singleton("test"));
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType())
                  .pass(JRoutineCore.channel().of("test"));
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      JRoutineCore.readOnly(JRoutineCore.channel().ofType()).remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {
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
}
