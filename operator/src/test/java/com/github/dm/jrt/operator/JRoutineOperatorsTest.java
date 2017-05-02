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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.common.BackoffBuilder;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.function.util.Supplier;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOfParallel;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.util.FunctionDecorator.identity;
import static com.github.dm.jrt.function.util.PredicateDecorator.isEqualTo;
import static com.github.dm.jrt.function.util.PredicateDecorator.isInstanceOf;
import static com.github.dm.jrt.function.util.PredicateDecorator.isNotNull;
import static com.github.dm.jrt.function.util.PredicateDecorator.isNull;
import static com.github.dm.jrt.function.util.PredicateDecorator.isSameAs;
import static com.github.dm.jrt.function.util.PredicateDecorator.negative;
import static com.github.dm.jrt.operator.JRoutineOperators.append;
import static com.github.dm.jrt.operator.JRoutineOperators.appendAccept;
import static com.github.dm.jrt.operator.JRoutineOperators.appendGet;
import static com.github.dm.jrt.operator.JRoutineOperators.collect;
import static com.github.dm.jrt.operator.JRoutineOperators.collectInto;
import static com.github.dm.jrt.operator.JRoutineOperators.failIf;
import static com.github.dm.jrt.operator.JRoutineOperators.failOnComplete;
import static com.github.dm.jrt.operator.JRoutineOperators.filter;
import static com.github.dm.jrt.operator.JRoutineOperators.orElse;
import static com.github.dm.jrt.operator.JRoutineOperators.orElseAccept;
import static com.github.dm.jrt.operator.JRoutineOperators.orElseGet;
import static com.github.dm.jrt.operator.JRoutineOperators.orElseThrow;
import static com.github.dm.jrt.operator.JRoutineOperators.peekComplete;
import static com.github.dm.jrt.operator.JRoutineOperators.peekError;
import static com.github.dm.jrt.operator.JRoutineOperators.peekOutput;
import static com.github.dm.jrt.operator.JRoutineOperators.prepend;
import static com.github.dm.jrt.operator.JRoutineOperators.prependAccept;
import static com.github.dm.jrt.operator.JRoutineOperators.prependGet;
import static com.github.dm.jrt.operator.JRoutineOperators.reduce;
import static com.github.dm.jrt.operator.JRoutineOperators.replaceIf;
import static com.github.dm.jrt.operator.JRoutineOperators.replacementAcceptIf;
import static com.github.dm.jrt.operator.JRoutineOperators.replacementApplyIf;
import static com.github.dm.jrt.operator.JRoutineOperators.then;
import static com.github.dm.jrt.operator.JRoutineOperators.thenAccept;
import static com.github.dm.jrt.operator.JRoutineOperators.thenGet;
import static com.github.dm.jrt.operator.JRoutineOperators.unary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Operators unit tests.
 * <p>
 * Created by davide-maestroni on 06/14/2016.
 */
public class JRoutineOperatorsTest {

  @Test
  public void testAllMatch() {
    assertThat(JRoutineCore.routine().of(JRoutineOperators.allMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(false);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.allMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(true);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.allMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test1", "test2").close().in(seconds(3)).all()).containsExactly(false);
  }

  @Test
  public void testAnyMatch() {
    assertThat(JRoutineCore.routine().of(JRoutineOperators.anyMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(false);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.anyMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(true);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.anyMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test1", "test2").close().in(seconds(3)).all()).containsExactly(true);
  }

  @Test
  public void testAppend() {
    assertThat(JRoutineCore.routine()
                           .of(append("test2"))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test2");
    assertThat(JRoutineCore.routine()
                           .of(append("test2", "test3"))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineCore.routine()
                           .of(append(Arrays.asList("test2", "test3")))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineCore.routine()
                           .of(append(JRoutineCore.channel().of("test2", "test3")))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test2", "test3");
  }

  @Test
  public void testAppend2() {
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(appendGet(new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().all()).containsExactly("test1", "TEST2");
    assertThat(
        JRoutineCore.routineOn(syncExecutor()).of(appendAccept(new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> resultChannel) {
            resultChannel.pass("TEST2");
          }
        })).invoke().pass("test1").close().all()).containsExactly("test1", "TEST2");
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(appendGet(3, new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("test1", "TEST2",
        "TEST2", "TEST2");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(appendAccept(3, new Consumer<Channel<String, ?>>() {

                             public void accept(final Channel<String, ?> resultChannel) {
                               resultChannel.pass("TEST2");
                             }
                           }))
                           .invoke()
                           .pass("test1")
                           .close()
                           .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
    assertThat(JRoutineCore.routine().of(appendGet(new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("test1", "TEST2");
    assertThat(JRoutineCore.routine().of(appendAccept(new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> resultChannel) {
        resultChannel.pass("TEST2");
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("test1", "TEST2");
    assertThat(JRoutineCore.routine().of(appendGet(3, new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("test1", "TEST2",
        "TEST2", "TEST2");
    assertThat(JRoutineCore.routine().of(appendAccept(3, new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> resultChannel) {
        resultChannel.pass("TEST2");
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("test1", "TEST2",
        "TEST2", "TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(
                               JRoutineCore.routine().of(appendGet(new Supplier<String>() {

                                 public String get() {
                                   return "TEST2";
                                 }
                               }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(JRoutineCore.routine()
                                                             .of(appendAccept(
                                                                 new Consumer<Channel<String, ?>>
                                                                     () {

                                                                   public void accept(
                                                                       final Channel<String, ?>
                                                                           resultChannel) {
                                                                     resultChannel.pass("TEST2");
                                                                   }
                                                                 }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(
                               JRoutineCore.routine().of(appendGet(3, new Supplier<String>() {

                                 public String get() {
                                   return "TEST2";
                                 }
                               }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(JRoutineCore.routine()
                                                             .of(appendAccept(3,
                                                                 new Consumer<Channel<String, ?>>
                                                                     () {

                                                                   public void accept(
                                                                       final Channel<String, ?>
                                                                           resultChannel) {
                                                                     resultChannel.pass("TEST2");
                                                                   }
                                                                 }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
  }

  @Test
  public void testAverage() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass((short) 1, (short) 2, (short) 3, (short) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass(1.0, 2.0, 3.0, 4.0)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass(BigInteger.valueOf(1), BigInteger.valueOf(2),
                               BigInteger.valueOf(3), BigInteger.valueOf(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(BigInteger.valueOf(2));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average())
                           .invoke()
                           .pass(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                               new BigDecimal(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(new BigDecimal(2.5));
  }

  @Test
  public void testAverageBig() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(BigDecimal.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(new BigDecimal(2.5));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(BigDecimal.class))
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(new BigDecimal(2.5));
  }

  @Test
  public void testAverageByte() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass((short) 1, (short) 2, (short) 3, (short) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass(1.0, 2.0, 3.0, 4.0)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass(BigInteger.valueOf(1), BigInteger.valueOf(2),
                               BigInteger.valueOf(3), BigInteger.valueOf(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class))
                           .invoke()
                           .pass(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                               new BigDecimal(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class, Byte.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class, Byte.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 2);
  }

  @Test
  public void testAverageDouble() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0d);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass((short) 1, (short) 2, (short) 3, (short) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass(1.0, 2.0, 3.0, 4.0)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass(BigInteger.valueOf(1), BigInteger.valueOf(2),
                               BigInteger.valueOf(3), BigInteger.valueOf(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class))
                           .invoke()
                           .pass(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                               new BigDecimal(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class, Double.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0d);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class, Double.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5);
  }

  @Test
  public void testAverageFloat() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass((short) 1, (short) 2, (short) 3, (short) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass(1.0, 2.0, 3.0, 4.0)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass(BigInteger.valueOf(1), BigInteger.valueOf(2),
                               BigInteger.valueOf(3), BigInteger.valueOf(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class))
                           .invoke()
                           .pass(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                               new BigDecimal(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class, Float.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Double.class, Float.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2.5f);
  }

  @Test
  public void testAverageInteger() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass((short) 1, (short) 2, (short) 3, (short) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass((byte) 1, 2, (byte) 3, (byte) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass(1.0, 2.0, 3.0, 4.0)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass(BigInteger.valueOf(1), BigInteger.valueOf(2),
                               BigInteger.valueOf(3), BigInteger.valueOf(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Integer.class))
                           .invoke()
                           .pass(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                               new BigDecimal(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Float.class, Integer.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Byte.class, Integer.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2);
  }

  @Test
  public void testAverageLong() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass((short) 1, (short) 2, (short) 3, (short) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass((byte) 1, 2L, (byte) 3, (byte) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass(1.0, 2.0, 3.0, 4.0)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass(BigInteger.valueOf(1), BigInteger.valueOf(2),
                               BigInteger.valueOf(3), BigInteger.valueOf(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Long.class))
                           .invoke()
                           .pass(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                               new BigDecimal(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(BigDecimal.class, Long.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(BigInteger.class, Long.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(2L);
  }

  @Test
  public void testAverageShort() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass((short) 1, (short) 2, (short) 3, (short) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass((byte) 1, (short) 2, (byte) 3, (byte) 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass(1.0, 2.0, 3.0, 4.0)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass(1f, 2f, 3f, 4f)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass(BigInteger.valueOf(1), BigInteger.valueOf(2),
                               BigInteger.valueOf(3), BigInteger.valueOf(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class))
                           .invoke()
                           .pass(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                               new BigDecimal(4))
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class, Short.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.average(Short.class, Short.class))
                           .invoke()
                           .pass(1L, 2L, 3L, 4L)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 2);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCastTo() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.castTo(Number.class))
                           .invoke()
                           .pass(1, 2.5)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2.5);
    assertThat(JRoutineOperators.castTo(String.class)).isEqualTo(JRoutineOperators.castTo(String.class));
    assertThat(JRoutineOperators.castTo(tokenOf(String.class))).isEqualTo(JRoutineOperators.castTo(String.class));
    try {
      JRoutineOperators.castTo((Class<?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testCollect() {
    assertThat(JRoutineCore.routine()
                           .of(collect(new BiConsumer<StringBuilder, StringBuilder>() {

                             public void accept(final StringBuilder builder,
                                 final StringBuilder builder2) {
                               builder.append(builder2);
                             }
                           }))
                           .invoke()
                           .pass(new StringBuilder("test1"), new StringBuilder("test2"),
                               new StringBuilder("test3"))
                           .close()
                           .in(seconds(3))
                           .next()
                           .toString()).isEqualTo("test1test2test3");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(collect(new BiConsumer<StringBuilder, StringBuilder>() {

                             public void accept(final StringBuilder builder,
                                 final StringBuilder builder2) {
                               builder.append(builder2);
                             }
                           }))
                           .invoke()
                           .pass(new StringBuilder("test1"), new StringBuilder("test2"),
                               new StringBuilder("test3"))
                           .close()
                           .in(seconds(3))
                           .next()
                           .toString()).isEqualTo("test1test2test3");
  }

  @Test
  public void testCollectCollection() {
    assertThat(JRoutineCore.routine().of(collectInto(new Supplier<List<String>>() {

      public List<String> get() {
        return new ArrayList<String>();
      }
    })).invoke().pass("test1", "test2", "test3").close().in(seconds(3)).next()).containsExactly(
        "test1", "test2", "test3");
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(collectInto(new Supplier<List<String>>() {

      public List<String> get() {
        return new ArrayList<String>();
      }
    })).invoke().pass("test1", "test2", "test3").close().in(seconds(3)).next()).containsExactly(
        "test1", "test2", "test3");
    assertThat(JRoutineCore.routine().of(collectInto(new Supplier<List<String>>() {

      public List<String> get() {
        return new ArrayList<String>();
      }
    })).invoke().close().in(seconds(3)).next()).isEmpty();
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(collectInto(new Supplier<List<String>>() {

      public List<String> get() {
        return new ArrayList<String>();
      }
    })).invoke().close().in(seconds(3)).next()).isEmpty();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCollectCollectionNullPointerError() {
    try {
      JRoutineCore.routine().of(collectInto(null));
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCollectNullPointerError() {
    try {
      collect(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testCollectSeed() {
    assertThat(JRoutineCore.routine()
                           .of(collect(new Supplier<StringBuilder>() {

                             public StringBuilder get() {
                               return new StringBuilder();
                             }
                           }, new BiConsumer<StringBuilder, String>() {

                             public void accept(final StringBuilder b, final String s) {
                               b.append(s);
                             }
                           }))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .in(seconds(3))
                           .next()
                           .toString()).isEqualTo("test1test2test3");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(collect(new Supplier<StringBuilder>() {

                             public StringBuilder get() {
                               return new StringBuilder();
                             }
                           }, new BiConsumer<StringBuilder, String>() {

                             public void accept(final StringBuilder b, final String s) {
                               b.append(s);
                             }
                           }))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .in(seconds(3))
                           .next()
                           .toString()).isEqualTo("test1test2test3");
    assertThat(JRoutineCore.routine().of(collect(new Supplier<StringBuilder>() {

      public StringBuilder get() {
        return new StringBuilder();
      }
    }, new BiConsumer<StringBuilder, String>() {

      public void accept(final StringBuilder b, final String s) {
        b.append(s);
      }
    })).invoke().close().in(seconds(3)).next().toString()).isEqualTo("");
    assertThat(JRoutineCore.routine().of(collect(new Supplier<List<Object>>() {

      public List<Object> get() {
        return new ArrayList<Object>();
      }
    }, new BiConsumer<List<Object>, Object>() {

      public void accept(final List<Object> l, final Object o) {
        l.add(o);
      }
    })).invoke().close().in(seconds(3)).next()).isEmpty();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCollectSeedNullPointerError() {
    try {
      collect(null, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testConstructor() {
    boolean failed = false;
    try {
      new JRoutineOperators();
      failed = true;

    } catch (final Throwable ignored) {
    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testCount() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.count())
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(10);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.count())
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0);
  }

  @Test
  public void testDistinct() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.distinct())
                           .invoke()
                           .pass("test", "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.distinct())
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test2");
  }

  @Test
  public void testDistinctIdentity() {
    final Object o = new Object() {

      @Override
      @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
      public boolean equals(final Object obj) {
        return false;
      }
    };
    List<Object> objects = JRoutineCore.routine()
                                       .of(JRoutineOperators.distinctIdentity())
                                       .invoke()
                                       .pass(o, o)
                                       .close()
                                       .in(seconds(3))
                                       .all();
    assertThat(objects).hasSize(1);
    assertThat(objects.get(0)).isSameAs(o);
    final Object o1 = new Object() {

      @Override
      @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
      public boolean equals(final Object obj) {
        return true;
      }
    };
    final Object o2 = new Object() {

      @Override
      @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
      public boolean equals(final Object obj) {
        return true;
      }
    };
    objects = JRoutineCore.routine()
                          .of(JRoutineOperators.distinctIdentity())
                          .invoke()
                          .pass(o1, o2)
                          .close()
                          .in(seconds(3))
                          .all();
    assertThat(objects).hasSize(2);
    assertThat(objects.get(0)).isSameAs(o1);
    assertThat(objects.get(1)).isSameAs(o2);
  }

  @Test
  public void testEqualTo() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isEqualTo("test")))
                           .invoke()
                           .pass("test", "test1", "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test", "test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isEqualTo(0)))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.filter(isEqualTo("test"))).isEqualTo(JRoutineOperators.filter(isEqualTo("test")));
    assertThat(JRoutineOperators.filter(isEqualTo(null))).isEqualTo(JRoutineOperators.filter(isEqualTo(null)));
  }

  @Test
  public void testFailOnComplete() {
    assertThat(JRoutineCore.routine()
                           .of(failOnComplete())
                           .invoke()
                           .pass(null, "test")
                           .close()
                           .in(seconds(3))
                           .getError()).isExactlyInstanceOf(AbortException.class);
    assertThat(JRoutineCore.routine().of(failOnComplete(new Supplier<Throwable>() {

      public Throwable get() {
        return new OutputTimeoutException("test");
      }
    })).invoke().pass(null, "test").close().in(seconds(3)).getError()).isExactlyInstanceOf(
        OutputTimeoutException.class);
    assertThat(JRoutineCore.routine()
                           .of(failOnComplete())
                           .invoke()
                           .pass("test1", "test2")
                           .in(seconds(3))
                           .next(2)).containsExactly("test1", "test2");
  }

  @Test
  public void testFailOnInput() {
    assertThat(JRoutineCore.routine()
                           .of(failIf(isNotNull()))
                           .invoke()
                           .pass(null, "test")
                           .close()
                           .in(seconds(3))
                           .getError()).isExactlyInstanceOf(AbortException.class);
    assertThat(JRoutineCore.routine().of(failIf(isNull(), new Supplier<Throwable>() {

      public Throwable get() {
        return new OutputTimeoutException("test");
      }
    })).invoke().pass(null, "test").close().in(seconds(3)).getError()).isExactlyInstanceOf(
        OutputTimeoutException.class);
    assertThat(JRoutineCore.routine()
                           .of(failIf(isNull()))
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test2");
  }

  @Test
  public void testFilter() {
    assertThat(JRoutineCore.routine()
                           .of(filter(isNotNull()))
                           .invoke()
                           .pass(null, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(JRoutineCore.routine().of(filter(isNotNull()))))
                           .invoke()
                           .pass(null, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(filter(isNotNull()))
                           .invoke()
                           .pass(null, "test")
                           .close()
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(factoryOfParallel(
                               JRoutineCore.routineOn(syncExecutor()).of(filter(isNotNull()))))
                           .invoke()
                           .pass(null, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGroupBy() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Number>groupBy(3))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
        Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
        Collections.<Number>singletonList(10));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Number>groupBy(13))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(
        Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
  }

  @Test
  public void testGroupByEquals() {
    final InvocationFactory<Object, List<Object>> factory = JRoutineOperators.groupBy(2);
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo("test");
    assertThat(factory).isNotEqualTo(JRoutineOperators.groupBy(3));
    assertThat(factory).isEqualTo(JRoutineOperators.groupBy(2));
    assertThat(factory.hashCode()).isEqualTo(JRoutineOperators.groupBy(2).hashCode());
  }

  @Test
  public void testGroupByError() {
    try {
      JRoutineOperators.groupBy(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      JRoutineOperators.groupBy(0);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGroupByKey() {
    assertThat(JRoutineCore.routine().of(JRoutineOperators.groupBy(new Function<Integer, Object>() {

      public Object apply(final Integer i) {
        return i % 2;
      }
    })).invoke().pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).close().in(seconds(3)).all()).containsOnly(
        Arrays.asList(2, 4, 6, 8, 10), Arrays.asList(1, 3, 5, 7, 9));
  }

  @Test
  public void testGroupByKeyEquals() {
    final InvocationFactory<Object, List<Object>> factory = JRoutineOperators.groupBy(identity());
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo("test");
    assertThat(factory).isNotEqualTo(JRoutineOperators.groupBy(new Function<Object, Object>() {

      public Object apply(final Object o) {
        return null;
      }
    }));
    assertThat(factory).isEqualTo(JRoutineOperators.groupBy(identity()));
    assertThat(factory.hashCode()).isEqualTo(JRoutineOperators.groupBy(identity()).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testGroupByKeyError() {
    try {
      JRoutineOperators.groupBy(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGroupByPlaceholder() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Number>groupBy(3, 0))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
        Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
        Arrays.<Number>asList(10, 0, 0));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Number>groupBy(13, -1))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(
        Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Number>groupBy(3, -31))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
        Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
        Arrays.<Number>asList(10, -31, -31));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Number>groupBy(13, 71))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(
        Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 71, 71, 71));
  }

  @Test
  public void testGroupByPlaceholderEquals() {
    final Object placeholder = -11;
    final InvocationFactory<Object, List<Object>> factory = JRoutineOperators.groupBy(2, placeholder);
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo("test");
    assertThat(factory).isNotEqualTo(JRoutineOperators.groupBy(3, -11));
    assertThat(factory).isEqualTo(JRoutineOperators.groupBy(2, -11));
    assertThat(factory.hashCode()).isEqualTo(JRoutineOperators.groupBy(2, -11).hashCode());
  }

  @Test
  public void testGroupByPlaceholderError() {
    try {
      JRoutineOperators.groupBy(-1, 77);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      JRoutineOperators.groupBy(0, null);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testIdentity() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.identity())
                           .invoke()
                           .pass(1, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, "test");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testInstanceOf() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isInstanceOf(String.class)))
                           .invoke()
                           .pass(3, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isInstanceOf(Number.class)))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.filter(isInstanceOf(String.class))).isEqualTo(
        JRoutineOperators.filter(isInstanceOf(String.class)));
    try {
      JRoutineOperators.filter(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testInterval() {
    long startTime = System.currentTimeMillis();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.interval(100, TimeUnit.MILLISECONDS))
                           .invoke()
                           .pass(3, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsOnly(3, "test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.interval(millis(50)))
                           .invoke()
                           .pass(3, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsOnly(3, "test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(50);
    startTime = System.currentTimeMillis();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.interval(
                               BackoffBuilder.afterCount(1).constantDelay(millis(200))))
                           .invoke()
                           .pass(3, "test", null)
                           .close()
                           .in(seconds(3))
                           .all()).containsOnly(3, "test", null);
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(200);
  }

  @Test
  public void testLimit() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.limit(5))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2, 3, 4, 5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.limit(0))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.limit(15))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void testLimitEquals() {
    final InvocationFactory<Object, Object> factory = JRoutineOperators.limit(2);
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo("test");
    assertThat(factory).isNotEqualTo(JRoutineOperators.limit(3));
    assertThat(factory).isEqualTo(JRoutineOperators.limit(2));
    assertThat(factory.hashCode()).isEqualTo(JRoutineOperators.limit(2).hashCode());
  }

  @Test
  public void testLimitError() {
    try {
      JRoutineOperators.limit(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testLimitLast() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.limitLast(5))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(6, 7, 8, 9, 10);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.limitLast(0))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.limitLast(15))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void testLimitLastEquals() {
    final InvocationFactory<Object, Object> factory = JRoutineOperators.limitLast(2);
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo("test");
    assertThat(factory).isNotEqualTo(JRoutineOperators.limitLast(3));
    assertThat(factory).isEqualTo(JRoutineOperators.limitLast(2));
    assertThat(factory.hashCode()).isEqualTo(JRoutineOperators.limitLast(2).hashCode());
  }

  @Test
  public void testLimitLastError() {
    try {
      JRoutineOperators.limitLast(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMax() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<String>max())
                           .invoke()
                           .pass("Z TEST", "test")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo("test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.maxBy(String.CASE_INSENSITIVE_ORDER))
                           .invoke()
                           .pass("Z TEST", "test")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo("Z TEST");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Integer>max())
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.maxBy(String.CASE_INSENSITIVE_ORDER))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.maxBy(Collections.reverseOrder())).isEqualTo(
        JRoutineOperators.maxBy(Collections.reverseOrder()));
    try {
      JRoutineOperators.maxBy(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMin() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<String>min())
                           .invoke()
                           .pass("Z TEST", "test")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo("Z TEST");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.minBy(String.CASE_INSENSITIVE_ORDER))
                           .invoke()
                           .pass("Z TEST", "test")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo("test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Integer>min())
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.minBy(String.CASE_INSENSITIVE_ORDER))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.minBy(Collections.reverseOrder())).isEqualTo(
        JRoutineOperators.minBy(Collections.reverseOrder()));
    try {
      JRoutineOperators.minBy(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testNone() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(negative()))
                           .invoke()
                           .pass("test1", null, 3)
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(negative()))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
  }

  @Test
  public void testNoneMatch() {
    assertThat(JRoutineCore.routine().of(JRoutineOperators.noneMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(true);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.noneMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(false);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.noneMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test1", "test2").close().in(seconds(3)).all()).containsExactly(false);
  }

  @Test
  public void testNotAllMatch() {
    assertThat(JRoutineCore.routine().of(JRoutineOperators.notAllMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(true);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.notAllMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test".equals(s);
      }
    })).invoke().pass("test", "test").close().in(seconds(3)).all()).containsExactly(false);
    assertThat(JRoutineCore.routine().of(JRoutineOperators.notAllMatch(new Predicate<String>() {

      public boolean test(final String s) {
        return "test1".equals(s);
      }
    })).invoke().pass("test1", "test2").close().in(seconds(3)).all()).containsExactly(true);
  }

  @Test
  public void testNotEqualTo() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isEqualTo("test").negate()))
                           .invoke()
                           .pass("test", "test1", "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isEqualTo(0).negate()))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.filter(isEqualTo("test").negate())).isEqualTo(
        JRoutineOperators.filter(isEqualTo("test").negate()));
    assertThat(JRoutineOperators.filter(isEqualTo(null).negate())).isEqualTo(
        JRoutineOperators.filter(isEqualTo(null).negate()));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNotInstanceOf() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isInstanceOf(String.class).negate()))
                           .invoke()
                           .pass(3, "test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(3);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isInstanceOf(Number.class).negate()))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.filter(isInstanceOf(String.class).negate())).isEqualTo(
        JRoutineOperators.filter(isInstanceOf(String.class).negate()));
  }

  @Test
  public void testNotSameAs() {
    final Object ref = new Object();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isSameAs(ref).negate()))
                           .invoke()
                           .pass("test", "test1", ref)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test", "test1");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isSameAs(0).negate()))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.filter(isSameAs(ref).negate())).isNotSameAs(
        JRoutineOperators.filter(isEqualTo(ref)));
    assertThat(JRoutineOperators.filter(isSameAs(null).negate())).isNotSameAs(
        JRoutineOperators.filter(isEqualTo(null)));
  }

  @Test
  public void testNull() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isNotNull()))
                           .invoke()
                           .pass(3, null, "test", null)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(3, "test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isNull()))
                           .invoke()
                           .pass(3, null, "test", null)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(null, null);
  }

  @Test
  public void testOrElse() {
    assertThat(
        JRoutineCore.routine().of(orElse("est")).invoke().pass("test").close().in(seconds(3)).all())
        .containsExactly("test");
    assertThat(JRoutineCore.routine()
                           .of(orElse("est1", "est2"))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routine()
                           .of(orElse(Arrays.asList("est1", "est2")))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routine().of(orElseAccept(new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> result) {
        result.pass("est");
      }
    })).invoke().pass("test").close().in(seconds(3)).all()).containsExactly("test");
    assertThat(JRoutineCore.routine().of(orElseAccept(2, new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> result) {
        result.pass("est");
      }
    })).invoke().close().in(seconds(3)).all()).containsExactly("est", "est");
    assertThat(JRoutineCore.routine().of(orElseGet(new Supplier<String>() {

      public String get() {
        return "est";
      }
    })).invoke().pass("test").close().in(seconds(3)).all()).containsExactly("test");
    assertThat(JRoutineCore.routine().of(orElseGet(2, new Supplier<String>() {

      public String get() {
        return "est";
      }
    })).invoke().close().in(seconds(3)).all()).containsExactly("est", "est");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(orElse("est"))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(orElse("est1", "est2"))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(orElse(Arrays.asList("est1", "est2")))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(
        JRoutineCore.routineOn(syncExecutor()).of(orElseAccept(new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> result) {
            result.pass("est");
          }
        })).invoke().pass("test").close().in(seconds(3)).all()).containsExactly("test");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(orElseAccept(2, new Consumer<Channel<String, ?>>() {

                             public void accept(final Channel<String, ?> result) {
                               result.pass("est");
                             }
                           }))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("est", "est");
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(orElseGet(new Supplier<String>() {

      public String get() {
        return "est";
      }
    })).invoke().pass("test").close().in(seconds(3)).all()).containsExactly("test");
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(orElseGet(2, new Supplier<String>() {

      public String get() {
        return "est";
      }
    })).invoke().close().in(seconds(3)).all()).containsExactly("est", "est");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testOrElseNullPointerError() {
    try {
      orElseAccept(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      orElseAccept(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      orElseGet(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      orElseGet(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testOrElseThrow() {
    assertThat(JRoutineCore.routine()
                           .of(orElseThrow(new IllegalStateException()))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test");
    assertThat(JRoutineCore.routine()
                           .of(orElseThrow(new IllegalStateException()))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .getError()
                           .getCause()).isExactlyInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testPeekComplete() {
    final AtomicBoolean isComplete = new AtomicBoolean(false);
    assertThat(JRoutineCore.routine().of(peekComplete(new Action() {

      public void perform() {
        isComplete.set(true);
      }
    })).invoke().pass("test1", "test2", "test3").close().in(seconds(3)).all()).containsExactly(
        "test1", "test2", "test3");
    assertThat(isComplete.get()).isTrue();
    isComplete.set(false);
    final Channel<Object, Object> channel = JRoutineCore.routine().of(peekComplete(new Action() {

      public void perform() {
        isComplete.set(true);
      }
    })).invoke();
    channel.abort(new NoSuchElementException());
    assertThat(channel.in(seconds(3)).getError()).isExactlyInstanceOf(AbortException.class);
    assertThat(isComplete.get()).isFalse();
  }

  @Test
  public void testPeekError() {
    final AtomicBoolean isError = new AtomicBoolean(false);
    final Channel<String, String> channel =
        JRoutineCore.routine().of(JRoutineOperators.<String>peekError(new Consumer<RoutineException>() {

          public void accept(final RoutineException e) {
            isError.set(true);
          }
        })).invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(3)).getError()).isExactlyInstanceOf(AbortException.class);
    assertThat(isError.get()).isTrue();
    isError.set(false);
    assertThat(JRoutineCore.routine().of(peekError(new Consumer<RoutineException>() {

      public void accept(final RoutineException e) {
        isError.set(true);
      }
    })).invoke().pass("test").close().in(seconds(3)).all()).containsExactly("test");
    assertThat(isError.get()).isFalse();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testPeekNullPointerError() {
    try {
      peekOutput(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      peekComplete(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      peekError(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testPeekOutput() {
    final ArrayList<String> data = new ArrayList<String>();
    assertThat(JRoutineCore.routine().of(peekOutput(new Consumer<String>() {

      public void accept(final String s) {
        data.add(s);
      }
    })).invoke().pass("test1", "test2", "test3").close().in(seconds(3)).all()).containsExactly(
        "test1", "test2", "test3");
    assertThat(data).containsExactly("test1", "test2", "test3");
  }

  @Test
  public void testPrepend() {
    assertThat(JRoutineCore.routine()
                           .of(prepend("test2"))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2", "test1");
    assertThat(JRoutineCore.routine()
                           .of(prepend("test2", "test3"))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2", "test3", "test1");
    assertThat(JRoutineCore.routine()
                           .of(prepend(Arrays.asList("test2", "test3")))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2", "test3", "test1");
    assertThat(JRoutineCore.routine()
                           .of(prepend(JRoutineCore.channel().of("test2", "test3")))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2", "test3", "test1");
  }

  @Test
  public void testPrepend2() {
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(prependGet(new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().all()).containsExactly("TEST2", "test1");
    assertThat(
        JRoutineCore.routineOn(syncExecutor()).of(prependAccept(new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> resultChannel) {
            resultChannel.pass("TEST2");
          }
        })).invoke().pass("test1").close().all()).containsExactly("TEST2", "test1");
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(prependGet(3, new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "TEST2",
        "TEST2", "test1");
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .of(prependAccept(3, new Consumer<Channel<String, ?>>() {

                             public void accept(final Channel<String, ?> resultChannel) {
                               resultChannel.pass("TEST2");
                             }
                           }))
                           .invoke()
                           .pass("test1")
                           .close()
                           .all()).containsExactly("TEST2", "TEST2", "TEST2", "test1");
    assertThat(JRoutineCore.routine().of(prependGet(new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "test1");
    assertThat(JRoutineCore.routine().of(prependAccept(new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> resultChannel) {
        resultChannel.pass("TEST2");
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "test1");
    assertThat(JRoutineCore.routine().of(prependGet(3, new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "TEST2",
        "TEST2", "test1");
    assertThat(JRoutineCore.routine().of(prependAccept(3, new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> resultChannel) {
        resultChannel.pass("TEST2");
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "TEST2",
        "TEST2", "test1");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(
                               JRoutineCore.routine().of(prependGet(new Supplier<String>() {

                                 public String get() {
                                   return "TEST2";
                                 }
                               }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2", "test1");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(JRoutineCore.routine()
                                                             .of(prependAccept(
                                                                 new Consumer<Channel<String, ?>>
                                                                     () {

                                                                   public void accept(
                                                                       final Channel<String, ?>
                                                                           resultChannel) {
                                                                     resultChannel.pass("TEST2");
                                                                   }
                                                                 }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2", "test1");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(
                               JRoutineCore.routine().of(prependGet(3, new Supplier<String>() {

                                 public String get() {
                                   return "TEST2";
                                 }
                               }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2", "TEST2", "TEST2", "test1");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(JRoutineCore.routine()
                                                             .of(prependAccept(3,
                                                                 new Consumer<Channel<String, ?>>
                                                                     () {

                                                                   public void accept(
                                                                       final Channel<String, ?>
                                                                           resultChannel) {
                                                                     resultChannel.pass("TEST2");
                                                                   }
                                                                 }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2", "TEST2", "TEST2", "test1");
  }

  @Test
  public void testReduce() {
    assertThat(JRoutineCore.routine().of(reduce(new BiFunction<String, String, String>() {

      public String apply(final String s, final String s2) {
        return s + s2;
      }
    })).invoke().pass("test1", "test2", "test3").close().in(seconds(3)).all()).containsExactly(
        "test1test2test3");
    assertThat(
        JRoutineCore.routineOn(syncExecutor()).of(reduce(new BiFunction<String, String, String>() {

          public String apply(final String s, final String s2) {
            return s + s2;
          }
        })).invoke().pass("test1", "test2", "test3").close().all()).containsExactly(
        "test1test2test3");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testReduceNullPointerError() {
    try {
      reduce(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testReduceSeed() {
    assertThat(JRoutineCore.routine()
                           .of(reduce(new Supplier<StringBuilder>() {

                             public StringBuilder get() {
                               return new StringBuilder();
                             }
                           }, new BiFunction<StringBuilder, String, StringBuilder>() {

                             public StringBuilder apply(final StringBuilder b, final String s) {
                               return b.append(s);
                             }
                           }))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .in(seconds(3))
                           .next()
                           .toString()).isEqualTo("test1test2test3");
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(reduce(new Supplier<StringBuilder>() {

      public StringBuilder get() {
        return new StringBuilder();
      }
    }, new BiFunction<StringBuilder, String, StringBuilder>() {

      public StringBuilder apply(final StringBuilder b, final String s) {
        return b.append(s);
      }
    })).invoke().pass("test1", "test2", "test3").close().next().toString()).isEqualTo(
        "test1test2test3");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testReduceSeedNullPointerError() {
    try {
      reduce(null, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testReplace() {
    assertThat(JRoutineCore.routine()
                           .of(replaceIf(isEqualTo("test2"), "test"))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test", "test3");
    assertThat(JRoutineCore.routine()
                           .of(replacementAcceptIf(isEqualTo("test2"),
                               new BiConsumer<String, Channel<String, ?>>() {

                                 public void accept(final String s,
                                     final Channel<String, ?> result) {
                                   assertThat(s).isEqualTo("test2");
                                   result.pass("test3", "test1");
                                 }
                               }))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test3", "test1", "test3");
    assertThat(JRoutineCore.routine()
                           .of(replacementApplyIf(isEqualTo("test2"),
                               new Function<String, String>() {

                                 public String apply(final String s) {
                                   assertThat(s).isEqualTo("test2");
                                   return "test";
                                 }
                               }))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test1", "test", "test3");
  }

  @Test
  public void testReplaceSame() {
    final Object target = new Object();
    final Object obj1 = new Object() {

      @Override
      public boolean equals(final Object obj) {
        return (obj == target) || super.equals(obj);
      }
    };
    final Object obj2 = new Object() {

      @Override
      public boolean equals(final Object obj) {
        return (obj == target) || super.equals(obj);
      }
    };
    final Object obj3 = new Object() {

      @Override
      public boolean equals(final Object obj) {
        return (obj == target) || super.equals(obj);
      }
    };
    assertThat(JRoutineCore.routine()
                           .of(replaceIf(isSameAs(target), obj2))
                           .invoke()
                           .pass(obj1, target, obj3)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(obj1, obj2, obj3);
    assertThat(JRoutineCore.routine()
                           .of(replacementAcceptIf(isSameAs(target),
                               new BiConsumer<Object, Channel<Object, ?>>() {

                                 public void accept(final Object o,
                                     final Channel<Object, ?> result) {
                                   assertThat(o).isSameAs(target);
                                   result.pass(obj3, obj1);
                                 }
                               }))
                           .invoke()
                           .pass(obj1, target, obj3)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(obj1, obj3, obj1, obj3);
    assertThat(JRoutineCore.routine()
                           .of(replacementApplyIf(isSameAs(target), new Function<Object, Object>() {

                             public Object apply(final Object o) {
                               assertThat(o).isSameAs(target);
                               return obj2;
                             }
                           }))
                           .invoke()
                           .pass(obj1, target, obj3)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(obj1, obj2, obj3);
  }

  @Test
  public void testSameAs() {
    final Object ref = new Object();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isSameAs(ref)))
                           .invoke()
                           .pass("test", "test1", ref)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(ref);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.filter(isSameAs(0)))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineOperators.filter(isSameAs(ref))).isEqualTo(JRoutineOperators.filter(isSameAs(ref)));
    assertThat(JRoutineOperators.filter(isSameAs(null))).isEqualTo(JRoutineOperators.filter(isSameAs(null)));
  }

  @Test
  public void testSkip() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.skip(5))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(6, 7, 8, 9, 10);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.skip(15))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.skip(0))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void testSkipEquals() {
    final InvocationFactory<Object, Object> factory = JRoutineOperators.skip(2);
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo("test");
    assertThat(factory).isNotEqualTo(JRoutineOperators.skip(3));
    assertThat(factory).isEqualTo(JRoutineOperators.skip(2));
    assertThat(factory.hashCode()).isEqualTo(JRoutineOperators.skip(2).hashCode());
  }

  @Test
  public void testSkipError() {
    try {
      JRoutineOperators.skip(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testSkipLast() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.skipLast(5))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2, 3, 4, 5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.skipLast(15))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).isEmpty();
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.skipLast(0))
                           .invoke()
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void testSkipLastEquals() {
    final InvocationFactory<Object, Object> factory = JRoutineOperators.skipLast(2);
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo("test");
    assertThat(factory).isNotEqualTo(JRoutineOperators.skipLast(3));
    assertThat(factory).isEqualTo(JRoutineOperators.skipLast(2));
    assertThat(factory.hashCode()).isEqualTo(JRoutineOperators.skipLast(2).hashCode());
  }

  @Test
  public void testSkipLastError() {
    try {
      JRoutineOperators.skipLast(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testSort() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Integer>sort())
                           .invoke()
                           .pass(2, 5, 4, 3, 1)
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly(1, 2, 3, 4, 5);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<String>sort())
                           .invoke()
                           .pass("a", "C", "b")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("C", "a", "b");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sortBy(String.CASE_INSENSITIVE_ORDER))
                           .invoke()
                           .pass("a", "C", "b")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("a", "b", "C");
  }

  @Test
  public void testSum() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum())
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum())
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(10);
  }

  @Test
  public void testSumBig() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(BigDecimal.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(new BigDecimal(10));
  }

  @Test
  public void testSumByte() {
    assertThat(
        JRoutineCore.routine().of(JRoutineOperators.sum(Byte.class)).invoke().close().in(seconds(3)).next())
        .isEqualTo((byte) 0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Byte.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((byte) 10);
  }

  @Test
  public void testSumDouble() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Double.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0d);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Double.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(10d);
  }

  @Test
  public void testSumFloat() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Float.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0f);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Float.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(10f);
  }

  @Test
  public void testSumInteger() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Integer.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Integer.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(10);
  }

  @Test
  public void testSumLong() {
    assertThat(
        JRoutineCore.routine().of(JRoutineOperators.sum(Long.class)).invoke().close().in(seconds(3)).next())
        .isEqualTo(0L);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Long.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(10L);
  }

  @Test
  public void testSumShort() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Short.class))
                           .invoke()
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 0);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.sum(Short.class))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo((short) 10);
  }

  @Test
  public void testThen() {
    assertThat(JRoutineCore.routine()
                           .of(then("test2"))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2");
    assertThat(JRoutineCore.routine()
                           .of(then("test2", "test3"))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2", "test3");
    assertThat(JRoutineCore.routine()
                           .of(then(Arrays.asList("test2", "test3")))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2", "test3");
    assertThat(JRoutineCore.routine()
                           .of(then(JRoutineCore.channel().of("test2", "test3")))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("test2", "test3");
  }

  @Test
  public void testThen2() {
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(thenGet(new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().all()).containsExactly("TEST2");
    assertThat(
        JRoutineCore.routineOn(syncExecutor()).of(thenAccept(new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> resultChannel) {
            resultChannel.pass("TEST2");
          }
        })).invoke().pass("test1").close().all()).containsExactly("TEST2");
    assertThat(JRoutineCore.routineOn(syncExecutor()).of(thenGet(3, new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "TEST2",
        "TEST2");
    assertThat(
        JRoutineCore.routineOn(syncExecutor()).of(thenAccept(3, new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> resultChannel) {
            resultChannel.pass("TEST2");
          }
        })).invoke().pass("test1").close().all()).containsExactly("TEST2", "TEST2", "TEST2");
    assertThat(JRoutineCore.routine().of(thenGet(new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2");
    assertThat(JRoutineCore.routine().of(thenAccept(new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> resultChannel) {
        resultChannel.pass("TEST2");
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2");
    assertThat(JRoutineCore.routine().of(thenGet(3, new Supplier<String>() {

      public String get() {
        return "TEST2";
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "TEST2",
        "TEST2");
    assertThat(JRoutineCore.routine().of(thenAccept(3, new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> resultChannel) {
        resultChannel.pass("TEST2");
      }
    })).invoke().pass("test1").close().in(seconds(3)).all()).containsExactly("TEST2", "TEST2",
        "TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(
                               JRoutineCore.routine().of(thenGet(new Supplier<String>() {

                                 public String get() {
                                   return "TEST2";
                                 }
                               }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(JRoutineCore.routine()
                                                             .of(thenAccept(
                                                                 new Consumer<Channel<String, ?>>
                                                                     () {

                                                                   public void accept(
                                                                       final Channel<String, ?>
                                                                           resultChannel) {
                                                                     resultChannel.pass("TEST2");
                                                                   }
                                                                 }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(
                               JRoutineCore.routine().of(thenGet(3, new Supplier<String>() {

                                 public String get() {
                                   return "TEST2";
                                 }
                               }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2", "TEST2", "TEST2");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(JRoutineCore.routine()
                                                             .of(thenAccept(3,
                                                                 new Consumer<Channel<String, ?>>
                                                                     () {

                                                                   public void accept(
                                                                       final Channel<String, ?>
                                                                           resultChannel) {
                                                                     resultChannel.pass("TEST2");
                                                                   }
                                                                 }))))
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(3))
                           .all()).containsExactly("TEST2", "TEST2", "TEST2");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testToArray() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray(String.class))
                           .invoke()
                           .pass("test", "test")
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly("test", "test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray(String.class))
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly("test1", "test2");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray(Number.class))
                           .invoke()
                           .pass(1, 2)
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly(1, 2);
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray())
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly("test1", "test2");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray())
                           .invoke()
                           .pass(1, 2)
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly(1, 2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testToArray2() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray(tokenOf(String.class)))
                           .invoke()
                           .pass("test", "test")
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly("test", "test");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray(tokenOf(String.class)))
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly("test1", "test2");
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toArray(tokenOf(Number.class)))
                           .invoke()
                           .pass(1, 2)
                           .close()
                           .in(seconds(3))
                           .next()).containsExactly(1, 2);
  }

  @Test
  public void testToList() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toList())
                           .invoke()
                           .pass("test", "test")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(Arrays.asList("test", "test"));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toList())
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(Arrays.asList("test1", "test2"));
  }

  @Test
  public void testToMap() {
    assertThat(JRoutineCore.routine().of(JRoutineOperators.toMap(new Function<String, Integer>() {

      public Integer apply(final String s) {
        return s.hashCode();
      }
    })).invoke().pass(Arrays.asList("test", "test")).close().in(seconds(3)).next()).isEqualTo(
        Collections.singletonMap("test".hashCode(), "test"));
    assertThat(JRoutineCore.routine().of(JRoutineOperators.toMap(new Function<String, Integer>() {

      public Integer apply(final String s) {
        return s.hashCode();
      }
    })).invoke().pass(Arrays.asList("test1", "test2")).close().in(seconds(3)).next()).isEqualTo(
        new HashMap<Integer, String>() {{
          put("test1".hashCode(), "test1");
          put("test2".hashCode(), "test2");
        }});
  }

  @Test
  public void testToSet() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toSet())
                           .invoke()
                           .pass("test", "test")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(Collections.singleton("test"));
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.toSet())
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .in(seconds(3))
                           .next()).isEqualTo(new HashSet<String>(Arrays.asList("test1", "test2")));
  }

  @Test
  public void testUnary() {
    assertThat(JRoutineCore.routine().of(unary(new Function<String, String>() {

      public String apply(final String s) {
        return s.toUpperCase();
      }
    })).invoke().pass("test1", "test2").close().in(seconds(3)).all()).containsExactly("TEST1",
        "TEST2");
  }

  @Test
  public void testUnfold() {
    assertThat(JRoutineCore.routine()
                           .of(JRoutineOperators.<Number>groupBy(3))
                           .invoke()
                           .pipe(JRoutineCore.routine()
                                             .of(factoryOfParallel(JRoutineCore.routine()
                                                                               .of(JRoutineOperators
                                                                                   .<Number>unfold())))
                                             .invoke())
                           .pass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                           .close()
                           .in(seconds(3))
                           .all()).containsOnly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }
}
