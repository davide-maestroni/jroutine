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
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
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
        })).async("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).async("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).async("test1", "test2").after(seconds(3)).all()).containsExactly(false);
    }

    @Test
    public void testAnyMatch() {
        assertThat(JRoutineCore.with(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).async("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).async("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).async("test1", "test2").after(seconds(3)).all()).containsExactly(true);
    }

    @Test
    public void testAverage() {
        assertThat(JRoutineCore.with(Operators.average())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.average())
                               .async(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.average())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.average())
                               .async((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.average())
                               .async((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.average())
                               .async(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.average())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.average())
                               .async(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(BigInteger.valueOf(2));
        assertThat(JRoutineCore.with(Operators.average())
                               .async(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(
                new BigDecimal(2.5).setScale(15, RoundingMode.HALF_EVEN));
    }

    @Test
    public void testAverageBig() {
        assertThat(JRoutineCore.with(Operators.<Integer>averageBig())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(new BigDecimal(2.5));
        assertThat(JRoutineCore.with(Operators.<Float>averageBig())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(new BigDecimal(2.5));
    }

    @Test
    public void testAverageByte() {
        assertThat(
                JRoutineCore.with(Operators.averageByte()).async().close().after(seconds(3)).next())
                .isEqualTo((byte) 0);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.with(Operators.averageByte())
                               .async(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 2);
    }

    @Test
    public void testAverageDouble() {
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0d);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.with(Operators.averageDouble())
                               .async(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5);
    }

    @Test
    public void testAverageFloat() {
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.with(Operators.averageFloat())
                               .async(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2.5f);
    }

    @Test
    public void testAverageInteger() {
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async((byte) 1, 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.with(Operators.averageInteger())
                               .async(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2);
    }

    @Test
    public void testAverageLong() {
        assertThat(
                JRoutineCore.with(Operators.averageLong()).async().close().after(seconds(3)).next())
                .isEqualTo(0L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async((byte) 1, 2L, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.with(Operators.averageLong())
                               .async(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo(2L);
    }

    @Test
    public void testAverageShort() {
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo((short) 0);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async(1L, 2L, 3L, 4L)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async((short) 1, (short) 2, (short) 3, (short) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async((byte) 1, (short) 2, (byte) 3, (byte) 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async(1.0, 2.0, 3.0, 4.0)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async(1f, 2f, 3f, 4f)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.with(Operators.averageShort())
                               .async(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .after(seconds(3))
                               .next()).isEqualTo((short) 2);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCastTo() {
        assertThat(JRoutineCore.with(Operators.castTo(Number.class))
                               .async(1, 2.5)
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
    public void testCount() {
        assertThat(JRoutineCore.with(Operators.count())
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .next()).isEqualTo(10);
        assertThat(JRoutineCore.with(Operators.count())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
    }

    @Test
    public void testEqualTo() {
        assertThat(JRoutineCore.with(Operators.isEqualTo("test"))
                               .async("test", "test1", "test")
                               .after(seconds(3))
                               .all()).containsExactly("test", "test");
        assertThat(JRoutineCore.with(Operators.isEqualTo(0))
                               .async()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isEqualTo("test")).isEqualTo(Operators.isEqualTo("test"));
        assertThat(Operators.isEqualTo(null)).isEqualTo(Operators.isEqualTo(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupBy() {
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(3))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(13))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
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
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, 0, 0));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(13, -1))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(3, -31))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, -31, -31));
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(13, 71))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
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
        assertThat(JRoutineCore.with(Operators.identity()).async(1, "test").after(seconds(3)).all())
                .containsExactly(1, "test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInstanceOf() {
        assertThat(JRoutineCore.with(Operators.isInstanceOf(String.class))
                               .async(3, "test")
                               .after(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(Operators.isInstanceOf(Number.class))
                               .async()
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
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(JRoutineCore.with(Operators.limit(0))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.limit(15))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
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
    @SuppressWarnings("ConstantConditions")
    public void testMax() {
        assertThat(JRoutineCore.with(Operators.<String>max())
                               .async("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("test");
        assertThat(JRoutineCore.with(Operators.maxBy(String.CASE_INSENSITIVE_ORDER))
                               .async("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("Z TEST");
        assertThat(JRoutineCore.with(Operators.max())
                               .async()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.maxBy(String.CASE_INSENSITIVE_ORDER))
                               .async()
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
                               .async("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("Z TEST");
        assertThat(JRoutineCore.with(Operators.minBy(String.CASE_INSENSITIVE_ORDER))
                               .async("Z TEST", "test")
                               .after(seconds(3))
                               .next()).isEqualTo("test");
        assertThat(JRoutineCore.with(Operators.min())
                               .async()
                               .close()
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.minBy(String.CASE_INSENSITIVE_ORDER))
                               .async()
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
        assertThat(
                JRoutineCore.with(Operators.none()).async("test1", null, 3).after(seconds(3)).all())
                .isEmpty();
        assertThat(JRoutineCore.with(Operators.none())
                               .async()
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
        })).async("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).async("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).async("test1", "test2").after(seconds(3)).all()).containsExactly(false);
    }

    @Test
    public void testNotAllMatch() {
        assertThat(JRoutineCore.with(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).async("test", "test").after(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.with(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).async("test", "test").after(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.with(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).async("test1", "test2").after(seconds(3)).all()).containsExactly(true);
    }

    @Test
    public void testNotEqualTo() {
        assertThat(JRoutineCore.with(Operators.isNotEqualTo("test"))
                               .async("test", "test1", "test")
                               .after(seconds(3))
                               .all()).containsExactly("test1");
        assertThat(JRoutineCore.with(Operators.isNotEqualTo(0))
                               .async()
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
                               .async(3, "test")
                               .after(seconds(3))
                               .all()).containsExactly(3);
        assertThat(JRoutineCore.with(Operators.isNotInstanceOf(Number.class))
                               .async()
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
                               .async("test", "test1", ref)
                               .after(seconds(3))
                               .all()).containsExactly("test", "test1");
        assertThat(
                JRoutineCore.with(Operators.isNotSameAs(0)).async().close().after(seconds(3)).all())
                .isEmpty();
        assertThat(Operators.isNotSameAs(ref)).isNotSameAs(Operators.isEqualTo(ref));
        assertThat(Operators.isNotSameAs(null)).isNotSameAs(Operators.isEqualTo(null));
    }

    @Test
    public void testNull() {
        assertThat(JRoutineCore.with(Operators.isNotNull())
                               .async(3, null, "test", null)
                               .after(seconds(3))
                               .all()).containsExactly(3, "test");
        assertThat(JRoutineCore.with(Operators.isNull())
                               .async(3, null, "test", null)
                               .after(seconds(3))
                               .all()).containsExactly(null, null);
    }

    @Test
    public void testSameAs() {
        final Object ref = new Object();
        assertThat(JRoutineCore.with(Operators.isSameAs(ref))
                               .async("test", "test1", ref)
                               .after(seconds(3))
                               .all()).containsExactly(ref);
        assertThat(JRoutineCore.with(Operators.isSameAs(0)).async().close().after(seconds(3)).all())
                .isEmpty();
        assertThat(Operators.isSameAs(ref)).isEqualTo(Operators.isSameAs(ref));
        assertThat(Operators.isSameAs(null)).isEqualTo(Operators.isSameAs(null));
    }

    @Test
    public void testSkip() {
        assertThat(JRoutineCore.with(Operators.skip(5))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(JRoutineCore.with(Operators.skip(15))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .after(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.with(Operators.skip(0))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
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
    public void testSort() {
        assertThat(JRoutineCore.with(Operators.<Integer>sort())
                               .async(2, 5, 4, 3, 1)
                               .after(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(JRoutineCore.with(Operators.<String>sort())
                               .async("a", "C", "b")
                               .after(seconds(3))
                               .all()).containsExactly("C", "a", "b");
        assertThat(JRoutineCore.with(Operators.sortBy(String.CASE_INSENSITIVE_ORDER))
                               .async("a", "C", "b")
                               .after(seconds(3))
                               .all()).containsExactly("a", "b", "C");
    }

    @Test
    public void testSum() {
        assertThat(JRoutineCore.with(Operators.sum())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.sum())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10);
    }

    @Test
    public void testSumBig() {
        assertThat(JRoutineCore.with(Operators.sumBig()).async(1, 2, 3, 4).after(seconds(3)).next())
                .isEqualTo(new BigDecimal(10));
    }

    @Test
    public void testSumByte() {
        assertThat(JRoutineCore.with(Operators.sumByte())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 0);
        assertThat(JRoutineCore.with(Operators.sumByte())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((byte) 10);
    }

    @Test
    public void testSumDouble() {
        assertThat(JRoutineCore.with(Operators.sumDouble())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0d);
        assertThat(
                JRoutineCore.with(Operators.sumDouble()).async(1, 2, 3, 4).after(seconds(3)).next())
                .isEqualTo(10d);
    }

    @Test
    public void testSumFloat() {
        assertThat(JRoutineCore.with(Operators.sumFloat()).async().close().after(seconds(3)).next())
                .isEqualTo(0f);
        assertThat(JRoutineCore.with(Operators.sumFloat())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10f);
    }

    @Test
    public void testSumInteger() {
        assertThat(JRoutineCore.with(Operators.sumInteger())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.with(Operators.sumInteger())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10);
    }

    @Test
    public void testSumLong() {
        assertThat(JRoutineCore.with(Operators.sumLong())
                               .async()
                               .close()
                               .after(seconds(3))
                               .next()).isEqualTo(0L);
        assertThat(JRoutineCore.with(Operators.sumLong())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo(10L);
    }

    @Test
    public void testSumShort() {
        assertThat(JRoutineCore.with(Operators.sumShort()).async().close().after(seconds(3)).next())
                .isEqualTo((short) 0);
        assertThat(JRoutineCore.with(Operators.sumShort())
                               .async(1, 2, 3, 4)
                               .after(seconds(3))
                               .next()).isEqualTo((short) 10);
    }

    @Test
    public void testToList() {
        assertThat(JRoutineCore.with(Operators.toList())
                               .async("test", "test")
                               .after(seconds(3))
                               .next()).isEqualTo(Arrays.asList("test", "test"));
        assertThat(JRoutineCore.with(Operators.toList())
                               .async("test1", "test2")
                               .after(seconds(3))
                               .next()).isEqualTo(Arrays.asList("test1", "test2"));
    }

    @Test
    public void testToMap() {
        assertThat(JRoutineCore.with(Operators.toMap(new Function<String, Integer>() {

            public Integer apply(final String s) {
                return s.hashCode();
            }
        })).async("test", "test").after(seconds(3)).next()).isEqualTo(
                Collections.singletonMap("test".hashCode(), "test"));
        assertThat(JRoutineCore.with(Operators.toMap(new Function<String, Integer>() {

            public Integer apply(final String s) {
                return s.hashCode();
            }
        })).async("test1", "test2").after(seconds(3)).next()).isEqualTo(
                new HashMap<Integer, String>() {{
                    put("test1".hashCode(), "test1");
                    put("test2".hashCode(), "test2");
                }});
    }

    @Test
    public void testToSet() {
        assertThat(
                JRoutineCore.with(Operators.toSet()).async("test", "test").after(seconds(3)).next())
                .isEqualTo(Collections.singleton("test"));
        assertThat(JRoutineCore.with(Operators.toSet())
                               .async("test1", "test2")
                               .after(seconds(3))
                               .next()).isEqualTo(
                new HashSet<String>(Arrays.asList("test1", "test2")));
    }

    @Test
    public void testUnfold() {
        assertThat(JRoutineCore.with(Operators.<Number>groupBy(3))
                               .async(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .bind(JRoutineCore.with(Operators.<Number>unfold()).parallel())
                               .close()
                               .after(seconds(3))
                               .all()).containsOnly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testUnique() {
        assertThat(
                JRoutineCore.with(Operators.unique()).async("test", "test").after(seconds(3)).all())
                .containsExactly("test");
        assertThat(JRoutineCore.with(Operators.unique())
                               .async("test1", "test2")
                               .after(seconds(3))
                               .all()).containsExactly("test1", "test2");
    }
}
