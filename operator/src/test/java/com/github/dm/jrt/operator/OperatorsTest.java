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
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.function.Action;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.operator.Operators.append;
import static com.github.dm.jrt.operator.Operators.appendAccept;
import static com.github.dm.jrt.operator.Operators.appendGet;
import static com.github.dm.jrt.operator.Operators.collect;
import static com.github.dm.jrt.operator.Operators.collectInto;
import static com.github.dm.jrt.operator.Operators.filter;
import static com.github.dm.jrt.operator.Operators.orElse;
import static com.github.dm.jrt.operator.Operators.orElseAccept;
import static com.github.dm.jrt.operator.Operators.orElseGet;
import static com.github.dm.jrt.operator.Operators.orElseThrow;
import static com.github.dm.jrt.operator.Operators.peekComplete;
import static com.github.dm.jrt.operator.Operators.peekError;
import static com.github.dm.jrt.operator.Operators.peekOutput;
import static com.github.dm.jrt.operator.Operators.prepend;
import static com.github.dm.jrt.operator.Operators.prependAccept;
import static com.github.dm.jrt.operator.Operators.prependGet;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.Operators.replace;
import static com.github.dm.jrt.operator.Operators.replaceAccept;
import static com.github.dm.jrt.operator.Operators.replaceGet;
import static com.github.dm.jrt.operator.Operators.replaceSame;
import static com.github.dm.jrt.operator.Operators.replaceSameAccept;
import static com.github.dm.jrt.operator.Operators.replaceSameGet;
import static com.github.dm.jrt.operator.Operators.then;
import static com.github.dm.jrt.operator.Operators.thenAccept;
import static com.github.dm.jrt.operator.Operators.thenGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Operators unit tests.
 * <p>
 * Created by davide-maestroni on 06/14/2016.
 */
public class OperatorsTest {

    @Test
    public void testAllMatch() {
        assertThat(JRoutineCore.with(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly(false);
    }

    @Test
    public void testAnyMatch() {
        assertThat(JRoutineCore.with(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly(true);
    }

    @Test
    public void testAppend() {
        assertThat(JRoutineCore.with(append("test2"))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test1", "test2");
        assertThat(JRoutineCore.with(append("test2", "test3"))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineCore.with(append(Arrays.asList("test2", "test3")))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineCore.with(append(JRoutineCore.io().of("test2", "test3")))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testAppend2() {
        assertThat(JRoutineCore.with(appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).syncCall("test1").all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineCore.with(appendAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).syncCall("test1").all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineCore.with(appendGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).syncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineCore.with(appendAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).syncCall("test1").all()).containsExactly("test1", "TEST2", "TEST2", "TEST2");
        assertThat(JRoutineCore.with(appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineCore.with(appendAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineCore.with(appendGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineCore.with(appendAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineCore.with(appendGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineCore.with(appendAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2");
        assertThat(JRoutineCore.with(appendGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
        assertThat(JRoutineCore.with(appendAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("test1", "TEST2",
                "TEST2", "TEST2");
    }

    @Test
    public void testAverage() {
        assertThat(
                JRoutineCore.with(Operators.average()).asyncCall().close().after(seconds(3)).next())
                .isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(BigInteger.valueOf(2));
        assertThat(JRoutineCore.with(Operators.average())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(
                new BigDecimal(2.5).setScale(15, RoundingMode.HALF_EVEN));
    }

    @Test
    public void testAverageBig() {
        assertThat(JRoutineCore.with(Operators.<Integer>averageBig())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(new BigDecimal(2.5));
        assertThat(JRoutineCore.with(Operators.<Float>averageBig())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(new BigDecimal(2.5));
    }

    @Test
    public void testAverageByte() {
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 0);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
    }

    @Test
    public void testAverageDouble() {
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0d);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
    }

    @Test
    public void testAverageFloat() {
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
    }

    @Test
    public void testAverageInteger() {
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall((byte) 1, 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2);
    }

    @Test
    public void testAverageLong() {
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall((byte) 1, 2L, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
    }

    @Test
    public void testAverageShort() {
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo((short) 0);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall((byte) 1, (short) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCastTo() {
        assertThat(JRoutineCore.with(Operators.castTo(Number.class))
                               .asyncCall(1, 2.5)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2.5);
        assertThat(Operators.castTo(String.class)).isEqualTo(Operators.castTo(String.class));
        assertThat(Operators.castTo(tokenOf(String.class))).isEqualTo(
                Operators.castTo(String.class));
        try {
            Operators.castTo((Class<?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testCollect() {
        assertThat(JRoutineCore.with(collect(new BiConsumer<StringBuilder, StringBuilder>() {

            public void accept(final StringBuilder builder, final StringBuilder builder2) {
                builder.append(builder2);
            }
        }))
                               .asyncCall(new StringBuilder("test1"), new StringBuilder("test2"),
                                       new StringBuilder("test3"))
                               .after(seconds(3))
                               .next()
                               .toString()).isEqualTo("test1test2test3");
        assertThat(JRoutineCore.with(collect(new BiConsumer<StringBuilder, StringBuilder>() {

            public void accept(final StringBuilder builder, final StringBuilder builder2) {
                builder.append(builder2);
            }
        }))
                               .syncCall(new StringBuilder("test1"), new StringBuilder("test2"),
                                       new StringBuilder("test3"))
                               .after(seconds(3))
                               .next()
                               .toString()).isEqualTo("test1test2test3");
    }

    @Test
    public void testCollectCollection() {
        assertThat(JRoutineCore.with(collectInto(new Supplier<List<String>>() {

            public List<String> get() {
                return new ArrayList<String>();
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).next()).containsExactly("test1",
                "test2", "test3");
        assertThat(JRoutineCore.with(collectInto(new Supplier<List<String>>() {

            public List<String> get() {
                return new ArrayList<String>();
            }
        })).syncCall("test1", "test2", "test3").after(seconds(3)).next()).containsExactly("test1",
                "test2", "test3");
        assertThat(JRoutineCore.with(collectInto(new Supplier<List<String>>() {

            public List<String> get() {
                return new ArrayList<String>();
            }
        })).asyncCall().close().after(seconds(3)).next()).isEmpty();
        assertThat(JRoutineCore.with(collectInto(new Supplier<List<String>>() {

            public List<String> get() {
                return new ArrayList<String>();
            }
        })).syncCall().close().after(seconds(3)).next()).isEmpty();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCollectCollectionNullPointerError() {
        try {
            JRoutineCore.with(collectInto(null));
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
        assertThat(JRoutineCore.with(collect(new Supplier<StringBuilder>() {

            public StringBuilder get() {
                return new StringBuilder();
            }
        }, new BiConsumer<StringBuilder, String>() {

            public void accept(final StringBuilder b, final String s) {
                b.append(s);
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).next().toString()).isEqualTo(
                "test1test2test3");
        assertThat(JRoutineCore.with(collect(new Supplier<StringBuilder>() {

            public StringBuilder get() {
                return new StringBuilder();
            }
        }, new BiConsumer<StringBuilder, String>() {

            public void accept(final StringBuilder b, final String s) {
                b.append(s);
            }
        })).syncCall("test1", "test2", "test3").after(seconds(3)).next().toString()).isEqualTo(
                "test1test2test3");
        assertThat(JRoutineCore.with(collect(new Supplier<StringBuilder>() {

            public StringBuilder get() {
                return new StringBuilder();
            }
        }, new BiConsumer<StringBuilder, String>() {

            public void accept(final StringBuilder b, final String s) {
                b.append(s);
            }
        })).asyncCall().close().after(seconds(3)).next().toString()).isEqualTo("");
        assertThat(JRoutineCore.with(collect(new Supplier<List<Object>>() {

            public List<Object> get() {
                return new ArrayList<Object>();
            }
        }, new BiConsumer<List<Object>, Object>() {

            public void accept(final List<Object> l, final Object o) {
                l.add(o);
            }
        })).asyncCall().close().after(seconds(3)).next()).isEmpty();
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
            new Operators();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testCount() {
        assertThat(JRoutineCore.with(Operators.count())
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .next()).isEqualTo(10);
        assertThat(JRoutineCore.with(Operators.count())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
    }

    @Test
    public void testEqualTo() {
        assertThat(JRoutineCore.with(Operators.isEqualTo("test"))
                               .asyncCall("test", "test1", "test")
                               .after(seconds(3))
                               .all()).containsExactly("test", "test");
        assertThat(JRoutineCore.with(Operators.isEqualTo(0))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isEqualTo("test")).isEqualTo(Operators.isEqualTo("test"));
        assertThat(Operators.isEqualTo(null)).isEqualTo(Operators.isEqualTo(null));
    }

    @Test
    public void testFilter() {
        assertThat(JRoutineCore.with(filter(Functions.isNotNull()))
                               .asyncCall(null, "test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(filter(Functions.isNotNull()))
                               .parallelCall(null, "test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(filter(Functions.isNotNull()))
                               .syncCall(null, "test")
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(filter(Functions.isNotNull()))
                               .sequentialCall(null, "test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupBy() {
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(3))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(13))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testGroupByEquals() {
        final InvocationFactory<Object, List<Object>> factory = Operators.groupBy(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Operators.groupBy(3));
        assertThat(factory).isEqualTo(Operators.groupBy(2));
        assertThat(factory.hashCode()).isEqualTo(Operators.groupBy(2).hashCode());
    }

    @Test
    public void testGroupByError() {
        try {
            Operators.groupBy(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            Operators.groupBy(0);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupByPlaceholder() {
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(3, 0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, 0, 0));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(13, -1))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(3, -31))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, -31, -31));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(13, 71))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 71, 71, 71));
    }

    @Test
    public void testGroupByPlaceholderEquals() {
        final Object placeholder = -11;
        final InvocationFactory<Object, List<Object>> factory = Operators.groupBy(2, placeholder);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Operators.groupBy(3, -11));
        assertThat(factory).isEqualTo(Operators.groupBy(2, -11));
        assertThat(factory.hashCode()).isEqualTo(Operators.groupBy(2, -11).hashCode());
    }

    @Test
    public void testGroupByPlaceholderError() {
        try {
            Operators.groupBy(-1, 77);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            Operators.groupBy(0, null);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testIdentity() {
        assertThat(JRoutineCore.with(Operators.identity())
                               .asyncCall(1, "test")
                               .after(seconds(3))
                               .all()).containsExactly(1, "test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInstanceOf() {
        assertThat(JRoutineCore.with(Operators.isInstanceOf(String.class))
                               .asyncCall(3, "test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(Operators.isInstanceOf(Number.class))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isInstanceOf(String.class)).isEqualTo(
                Operators.isInstanceOf(String.class));
        try {
            Operators.isInstanceOf(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testLimit() {
        assertThat(JRoutineCore.with(Operators.limit(5))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(JRoutineCore.with(Operators.limit(0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.limit(15))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testLimitEquals() {
        final InvocationFactory<Object, Object> factory = Operators.limit(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Operators.limit(3));
        assertThat(factory).isEqualTo(Operators.limit(2));
        assertThat(factory.hashCode()).isEqualTo(Operators.limit(2).hashCode());
    }

    @Test
    public void testLimitError() {
        try {
            Operators.limit(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testLimitLast() {
        assertThat(JRoutineCore.with(Operators.limitLast(5))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(JRoutineCore.with(Operators.limitLast(0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.limitLast(15))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testLimitLastEquals() {
        final InvocationFactory<Object, Object> factory = Operators.limitLast(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Operators.limitLast(3));
        assertThat(factory).isEqualTo(Operators.limitLast(2));
        assertThat(factory.hashCode()).isEqualTo(Operators.limitLast(2).hashCode());
    }

    @Test
    public void testLimitLastError() {
        try {
            Operators.limitLast(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMax() {
        assertThat(JRoutineCore.with(Operators.<String>max())
                               .asyncCall("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("test");
        assertThat(JRoutineCore.with(Operators.maxBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("Z TEST");
        assertThat(JRoutineCore.with(Operators.max())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.maxBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.maxBy(Collections.reverseOrder())).isEqualTo(
                Operators.maxBy(Collections.reverseOrder()));
        try {
            Operators.maxBy(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMin() {
        assertThat(JRoutineCore.with(Operators.<String>min())
                               .asyncCall("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("Z TEST");
        assertThat(JRoutineCore.with(Operators.minBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("test");
        assertThat(JRoutineCore.with(Operators.min())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.minBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.minBy(Collections.reverseOrder())).isEqualTo(
                Operators.minBy(Collections.reverseOrder()));
        try {
            Operators.minBy(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testNone() {
        assertThat(JRoutineCore.with(Operators.none())
                               .asyncCall("test1", null, 3)
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.none())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
    }

    @Test
    public void testNoneMatch() {
        assertThat(JRoutineCore.with(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly(false);
    }

    @Test
    public void testNotAllMatch() {
        assertThat(JRoutineCore.with(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly(true);
    }

    @Test
    public void testNotEqualTo() {
        assertThat(JRoutineCore.with(Operators.isNotEqualTo("test"))
                               .asyncCall("test", "test1", "test")
                               .after(seconds(3))
                               .all()).containsExactly("test1");
        assertThat(JRoutineCore.with(Operators.isNotEqualTo(0))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isNotEqualTo("test")).isEqualTo(Operators.isNotEqualTo("test"));
        assertThat(Operators.isNotEqualTo(null)).isEqualTo(Operators.isNotEqualTo(null));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNotInstanceOf() {
        assertThat(JRoutineCore.with(Operators.isNotInstanceOf(String.class))
                               .asyncCall(3, "test")
                               .after(seconds(3))
                               .all()).containsExactly(3);
        assertThat(JRoutineCore.with(Operators.isNotInstanceOf(Number.class))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isNotInstanceOf(String.class)).isEqualTo(
                Operators.isNotInstanceOf(String.class));
        try {
            Operators.isNotInstanceOf(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testNotSameAs() {
        final Object ref = new Object();
        assertThat(JRoutineCore.with(Operators.isNotSameAs(ref))
                               .asyncCall("test", "test1", ref)
                               .after(seconds(3))
                               .all()).containsExactly("test", "test1");
        assertThat(JRoutineCore.with(Operators.isNotSameAs(0))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isNotSameAs(ref)).isNotSameAs(Operators.isEqualTo(ref));
        assertThat(Operators.isNotSameAs(null)).isNotSameAs(Operators.isEqualTo(null));
    }

    @Test
    public void testNull() {
        assertThat(JRoutineCore.with(Operators.isNotNull())
                               .asyncCall(3, null, "test", null)
                               .after(seconds(3))
                               .all()).containsExactly(3, "test");
        assertThat(JRoutineCore.with(Operators.isNull())
                               .asyncCall(3, null, "test", null)
                               .after(seconds(3))
                               .all()).containsExactly(null, null);
    }

    @Test
    public void testOrElse() {
        assertThat(JRoutineCore.with(orElse("est"))
                               .asyncCall("test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(
                JRoutineCore.with(orElse("est1", "est2")).asyncCall("test").after(seconds(3)).all())
                .containsExactly("test");
        assertThat(JRoutineCore.with(orElse(Arrays.asList("est1", "est2")))
                               .asyncCall("test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElseAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {
                result.pass("est");
            }
        })).asyncCall("test").after(seconds(3)).all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElseAccept(2, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {
                result.pass("est");
            }
        })).asyncCall().close().after(seconds(3)).all()).containsExactly("est", "est");
        assertThat(JRoutineCore.with(orElseGet(new Supplier<String>() {

            public String get() {
                return "est";
            }
        })).asyncCall("test").after(seconds(3)).all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElseGet(2, new Supplier<String>() {

            public String get() {
                return "est";
            }
        })).asyncCall().close().after(seconds(3)).all()).containsExactly("est", "est");
        assertThat(JRoutineCore.with(orElse("est"))
                               .syncCall("test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElse("est1", "est2"))
                               .syncCall("test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElse(Arrays.asList("est1", "est2")))
                               .syncCall("test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElseAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {
                result.pass("est");
            }
        })).syncCall("test").after(seconds(3)).all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElseAccept(2, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {
                result.pass("est");
            }
        })).syncCall().close().after(seconds(3)).all()).containsExactly("est", "est");
        assertThat(JRoutineCore.with(orElseGet(new Supplier<String>() {

            public String get() {
                return "est";
            }
        })).syncCall("test").after(seconds(3)).all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElseGet(2, new Supplier<String>() {

            public String get() {
                return "est";
            }
        })).syncCall().close().after(seconds(3)).all()).containsExactly("est", "est");
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
        assertThat(JRoutineCore.with(orElseThrow(new IllegalStateException()))
                               .asyncCall("test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(orElseThrow(new IllegalStateException()))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .getError()
                               .getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testPeekComplete() {
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        assertThat(JRoutineCore.with(peekComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly("test1",
                "test2", "test3");
        assertThat(isComplete.get()).isTrue();
        isComplete.set(false);
        final Channel<Object, Object> channel = JRoutineCore.with(peekComplete(new Action() {

            public void perform() {
                isComplete.set(true);
            }
        })).asyncCall();
        channel.abort(new NoSuchElementException());
        assertThat(channel.after(seconds(3)).getError()).isExactlyInstanceOf(AbortException.class);
        assertThat(isComplete.get()).isFalse();
    }

    @Test
    public void testPeekError() {
        final AtomicBoolean isError = new AtomicBoolean(false);
        final Channel<String, String> channel =
                JRoutineCore.with(Operators.<String>peekError(new Consumer<RoutineException>() {

                    public void accept(final RoutineException e) {
                        isError.set(true);
                    }
                })).asyncCall();
        assertThat(channel.abort()).isTrue();
        assertThat(channel.after(seconds(3)).getError()).isExactlyInstanceOf(AbortException.class);
        assertThat(isError.get()).isTrue();
        isError.set(false);
        assertThat(JRoutineCore.with(peekError(new Consumer<RoutineException>() {

            public void accept(final RoutineException e) {
                isError.set(true);
            }
        })).asyncCall("test").after(seconds(3)).all()).containsExactly("test");
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
        assertThat(JRoutineCore.with(peekOutput(new Consumer<String>() {

            public void accept(final String s) {
                data.add(s);
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly("test1",
                "test2", "test3");
        assertThat(data).containsExactly("test1", "test2", "test3");
    }

    @Test
    public void testPrepend() {
        assertThat(JRoutineCore.with(prepend("test2"))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2", "test1");
        assertThat(JRoutineCore.with(prepend("test2", "test3"))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2", "test3", "test1");
        assertThat(JRoutineCore.with(prepend(Arrays.asList("test2", "test3")))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2", "test3", "test1");
        assertThat(JRoutineCore.with(prepend(JRoutineCore.io().of("test2", "test3")))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2", "test3", "test1");
    }

    @Test
    public void testPrepend2() {
        assertThat(JRoutineCore.with(prependGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).syncCall("test1").all()).containsExactly("TEST2", "test1");
        assertThat(JRoutineCore.with(prependAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).syncCall("test1").all()).containsExactly("TEST2", "test1");
        assertThat(JRoutineCore.with(prependGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).syncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2",
                "test1");
        assertThat(JRoutineCore.with(prependAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).syncCall("test1").all()).containsExactly("TEST2", "TEST2", "TEST2", "test1");
        assertThat(JRoutineCore.with(prependGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "test1");
        assertThat(JRoutineCore.with(prependAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "test1");
        assertThat(JRoutineCore.with(prependGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2",
                "test1");
        assertThat(JRoutineCore.with(prependAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2",
                "test1");
        assertThat(JRoutineCore.with(prependGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "test1");
        assertThat(JRoutineCore.with(prependAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "test1");
        assertThat(JRoutineCore.with(prependGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2", "test1");
        assertThat(JRoutineCore.with(prependAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2", "test1");
    }

    @Test
    public void testReduce() {
        assertThat(JRoutineCore.with(reduce(new BiFunction<String, String, String>() {

            public String apply(final String s, final String s2) {
                return s + s2;
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(JRoutineCore.with(reduce(new BiFunction<String, String, String>() {

            public String apply(final String s, final String s2) {
                return s + s2;
            }
        })).syncCall("test1", "test2", "test3").all()).containsExactly("test1test2test3");
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
        assertThat(JRoutineCore.with(reduce(new Supplier<StringBuilder>() {

            public StringBuilder get() {
                return new StringBuilder();
            }
        }, new BiFunction<StringBuilder, String, StringBuilder>() {

            public StringBuilder apply(final StringBuilder b, final String s) {
                return b.append(s);
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).next().toString()).isEqualTo(
                "test1test2test3");
        assertThat(JRoutineCore.with(reduce(new Supplier<StringBuilder>() {

            public StringBuilder get() {
                return new StringBuilder();
            }
        }, new BiFunction<StringBuilder, String, StringBuilder>() {

            public StringBuilder apply(final StringBuilder b, final String s) {
                return b.append(s);
            }
        })).syncCall("test1", "test2", "test3").next().toString()).isEqualTo("test1test2test3");
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
        assertThat(JRoutineCore.with(replace("test2", "test"))
                               .asyncCall("test1", "test2", "test3")
                               .after(seconds(3))
                               .all()).containsExactly("test1", "test", "test3");
        assertThat(JRoutineCore.with(replaceAccept("test2", new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> result) {
                result.pass("test3", "test1");
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly("test1",
                "test3", "test1", "test3");
        assertThat(JRoutineCore.with(replaceGet("test2", new Supplier<String>() {

            public String get() {
                return "test";
            }
        })).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly("test1",
                "test", "test3");
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
        assertThat(JRoutineCore.with(replaceSame(target, obj2))
                               .asyncCall(obj1, target, obj3)
                               .after(seconds(3))
                               .all()).containsExactly(obj1, obj2, obj3);
        assertThat(JRoutineCore.with(replaceSameAccept(target, new Consumer<Channel<Object, ?>>() {

            public void accept(final Channel<Object, ?> result) {
                result.pass(obj3, obj1);
            }
        })).asyncCall(obj1, target, obj3).after(seconds(3)).all()).containsExactly(obj1, obj3, obj1,
                obj3);
        assertThat(JRoutineCore.with(replaceSameGet(target, new Supplier<Object>() {

            public Object get() {
                return obj2;
            }
        })).asyncCall(obj1, target, obj3).after(seconds(3)).all()).containsExactly(obj1, obj2,
                obj3);
    }

    @Test
    public void testSameAs() {
        final Object ref = new Object();
        assertThat(JRoutineCore.with(Operators.isSameAs(ref))
                               .asyncCall("test", "test1", ref)
                               .after(seconds(3))
                               .all()).containsExactly(ref);
        assertThat(JRoutineCore.with(Operators.isSameAs(0))
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isSameAs(ref)).isEqualTo(Operators.isSameAs(ref));
        assertThat(Operators.isSameAs(null)).isEqualTo(Operators.isSameAs(null));
    }

    @Test
    public void testSkip() {
        assertThat(JRoutineCore.with(Operators.skip(5))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(JRoutineCore.with(Operators.skip(15))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.skip(0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testSkipEquals() {
        final InvocationFactory<Object, Object> factory = Operators.skip(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Operators.skip(3));
        assertThat(factory).isEqualTo(Operators.skip(2));
        assertThat(factory.hashCode()).isEqualTo(Operators.skip(2).hashCode());
    }

    @Test
    public void testSkipError() {
        try {
            Operators.skip(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testSkipLast() {
        assertThat(JRoutineCore.with(Operators.skipLast(5))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(JRoutineCore.with(Operators.skipLast(15))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.skipLast(0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testSkipLastEquals() {
        final InvocationFactory<Object, Object> factory = Operators.skipLast(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Operators.skipLast(3));
        assertThat(factory).isEqualTo(Operators.skipLast(2));
        assertThat(factory.hashCode()).isEqualTo(Operators.skipLast(2).hashCode());
    }

    @Test
    public void testSkipLastError() {
        try {
            Operators.skipLast(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testSort() {
        assertThat(JRoutineCore.with(Operators.<Integer>sort())
                               .asyncCall(2, 5, 4, 3, 1)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(JRoutineCore.with(Operators.<String>sort())
                               .asyncCall("a", "C", "b")
                               .after(seconds(3))
                               .all()).containsExactly("C", "a", "b");
        assertThat(JRoutineCore.with(Operators.sortBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall("a", "C", "b")
                               .after(seconds(3))
                               .all()).containsExactly("a", "b", "C");
    }

    @Test
    public void testSum() {
        assertThat(JRoutineCore.with(Operators.sum())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.sum())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10);
    }

    @Test
    public void testSumBig() {
        assertThat(JRoutineCore.with(Operators.sumBig())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(new BigDecimal(10));
    }

    @Test
    public void testSumByte() {
        assertThat(
                JRoutineCore.with(Operators.sumByte()).asyncCall().close().after(seconds(3)).next())
                .isEqualTo((byte) 0);
        assertThat(JRoutineCore.with(Operators.sumByte())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 10);
    }

    @Test
    public void testSumDouble() {
        assertThat(JRoutineCore.with(Operators.sumDouble())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0d);
        assertThat(JRoutineCore.with(Operators.sumDouble())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10d);
    }

    @Test
    public void testSumFloat() {
        assertThat(JRoutineCore.with(Operators.sumFloat())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0f);
        assertThat(JRoutineCore.with(Operators.sumFloat())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10f);
    }

    @Test
    public void testSumInteger() {
        assertThat(JRoutineCore.with(Operators.sumInteger())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.sumInteger())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10);
    }

    @Test
    public void testSumLong() {
        assertThat(
                JRoutineCore.with(Operators.sumLong()).asyncCall().close().after(seconds(3)).next())
                .isEqualTo(0L);
        assertThat(JRoutineCore.with(Operators.sumLong())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10L);
    }

    @Test
    public void testSumShort() {
        assertThat(JRoutineCore.with(Operators.sumShort())
                               .asyncCall()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo((short) 0);
        assertThat(JRoutineCore.with(Operators.sumShort())
                               .asyncCall(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 10);
    }

    @Test
    public void testThen() {
        assertThat(JRoutineCore.with(then("test2"))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2");
        assertThat(JRoutineCore.with(then("test2", "test3"))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2", "test3");
        assertThat(JRoutineCore.with(then(Arrays.asList("test2", "test3")))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2", "test3");
        assertThat(JRoutineCore.with(then(JRoutineCore.io().of("test2", "test3")))
                               .asyncCall("test1")
                               .after(seconds(3))
                               .all()).containsExactly("test2", "test3");
    }

    @Test
    public void testThen2() {
        assertThat(JRoutineCore.with(thenGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).syncCall("test1").all()).containsExactly("TEST2");
        assertThat(JRoutineCore.with(thenAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).syncCall("test1").all()).containsExactly("TEST2");
        assertThat(JRoutineCore.with(thenGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).syncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineCore.with(thenAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).syncCall("test1").all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineCore.with(thenGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineCore.with(thenAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineCore.with(thenGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineCore.with(thenAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).asyncCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(JRoutineCore.with(thenGet(new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineCore.with(thenAccept(new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2");
        assertThat(JRoutineCore.with(thenGet(3, new Supplier<String>() {

            public String get() {
                return "TEST2";
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2");
        assertThat(JRoutineCore.with(thenAccept(3, new Consumer<Channel<String, ?>>() {

            public void accept(final Channel<String, ?> resultChannel) {
                resultChannel.pass("TEST2");
            }
        })).parallelCall("test1").after(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                "TEST2");
    }

    @Test
    public void testToList() {
        assertThat(JRoutineCore.with(Operators.toList())
                               .asyncCall("test", "test")
                               .after(seconds(3))
                               .next()).isEqualTo(Arrays.asList("test", "test"));
        assertThat(JRoutineCore.with(Operators.toList())
                               .asyncCall("test1", "test2")
                               .after(seconds(3))
                               .next()).isEqualTo(Arrays.asList("test1", "test2"));
    }

    @Test
    public void testToMap() {
        assertThat(JRoutineCore.with(Operators.toMap(new Function<String, Integer>() {

            public Integer apply(final String s) {
                return s.hashCode();
            }
        })).asyncCall(Arrays.asList("test", "test")).after(seconds(3)).next()).isEqualTo(
                Collections.singletonMap("test".hashCode(), "test"));
        assertThat(JRoutineCore.with(Operators.toMap(new Function<String, Integer>() {

            public Integer apply(final String s) {
                return s.hashCode();
            }
        })).asyncCall(Arrays.asList("test1", "test2")).after(seconds(3)).next()).isEqualTo(
                new HashMap<Integer, String>() {{
                    put("test1".hashCode(), "test1");
                    put("test2".hashCode(), "test2");
                }});
    }

    @Test
    public void testToSet() {
        assertThat(JRoutineCore.with(Operators.toSet())
                               .asyncCall("test", "test")
                               .after(seconds(3))
                               .next()).isEqualTo(Collections.singleton("test"));
        assertThat(JRoutineCore.with(Operators.toSet())
                               .asyncCall("test1", "test2")
                               .after(seconds(3))
                               .next()).isEqualTo(
                new HashSet<String>(Arrays.asList("test1", "test2")));
    }

    @Test
    public void testUnfold() {
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(3))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .bind(JRoutineCore.with(Operators.<Number>unfold()).parallelCall())
                               .close()
                               .after(seconds(3))
                               .all()).containsOnly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testDistinct() {
        assertThat(JRoutineCore.with(Operators.distinct())
                               .asyncCall("test", "test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(Operators.distinct())
                               .asyncCall("test1", "test2")
                               .after(seconds(3))
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
        List<Object> objects = JRoutineCore.with(Operators.distinctIdentity())
                                           .asyncCall(o, o)
                                           .after(seconds(3))
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
        objects = JRoutineCore.with(Operators.distinctIdentity())
                              .asyncCall(o1, o2)
                              .after(seconds(3))
                              .all();
        assertThat(objects).hasSize(2);
        assertThat(objects.get(0)).isSameAs(o1);
        assertThat(objects.get(1)).isSameAs(o2);
    }
}
