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
        assertThat(JRoutineCore.on(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.on(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.on(Operators.allMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsExactly(false);
    }

    @Test
    public void testAnyMatch() {
        assertThat(JRoutineCore.on(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.on(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.on(Operators.anyMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsExactly(true);
    }

    @Test
    public void testAverage() {
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(BigInteger.valueOf(2));
        assertThat(JRoutineCore.on(Operators.average())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(
                new BigDecimal(2.5).setScale(15, RoundingMode.HALF_EVEN));
    }

    @Test
    public void testAverageBig() {
        assertThat(JRoutineCore.on(Operators.<Integer>averageBig())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(new BigDecimal(2.5));
        assertThat(JRoutineCore.on(Operators.<Float>averageBig())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(new BigDecimal(2.5));
    }

    @Test
    public void testAverageByte() {
        assertThat(JRoutineCore.on(Operators.averageByte()).asyncCall().afterMax(seconds(3)).next())
                .isEqualTo((byte) 0);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
        assertThat(JRoutineCore.on(Operators.averageByte())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 2);
    }

    @Test
    public void testAverageDouble() {
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0d);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
        assertThat(JRoutineCore.on(Operators.averageDouble())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5);
    }

    @Test
    public void testAverageFloat() {
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall((byte) 1, (byte) 2, (byte) 3, (byte) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
        assertThat(JRoutineCore.on(Operators.averageFloat())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2.5f);
    }

    @Test
    public void testAverageInteger() {
        assertThat(
                JRoutineCore.on(Operators.averageInteger()).asyncCall().afterMax(seconds(3)).next())
                .isEqualTo(0);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall((byte) 1, 2, (byte) 3, (byte) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
        assertThat(JRoutineCore.on(Operators.averageInteger())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2);
    }

    @Test
    public void testAverageLong() {
        assertThat(JRoutineCore.on(Operators.averageLong()).asyncCall().afterMax(seconds(3)).next())
                .isEqualTo(0L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall((byte) 1, 2L, (byte) 3, (byte) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
        assertThat(JRoutineCore.on(Operators.averageLong())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo(2L);
    }

    @Test
    public void testAverageShort() {
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 0);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall(1L, 2L, 3L, 4L)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall((short) 1, (short) 2, (short) 3, (short) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall((byte) 1, (short) 2, (byte) 3, (byte) 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall(1.0, 2.0, 3.0, 4.0)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall(1f, 2f, 3f, 4f)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall(BigInteger.valueOf(1), BigInteger.valueOf(2),
                                       BigInteger.valueOf(3), BigInteger.valueOf(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
        assertThat(JRoutineCore.on(Operators.averageShort())
                               .asyncCall(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                                       new BigDecimal(4))
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 2);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCastTo() {
        assertThat(JRoutineCore.on(Operators.castTo(Number.class))
                               .asyncCall(1, 2.5)
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.count())
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(10);
        assertThat(JRoutineCore.on(Operators.count())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0);
    }

    @Test
    public void testEqualTo() {
        assertThat(JRoutineCore.on(Operators.isEqualTo("test"))
                               .asyncCall("test", "test1", "test")
                               .afterMax(seconds(3))
                               .all()).containsExactly("test", "test");
        assertThat(JRoutineCore.on(Operators.isEqualTo(0))
                               .asyncCall()
                               .afterMax(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isEqualTo("test")).isEqualTo(Operators.isEqualTo("test"));
        assertThat(Operators.isEqualTo(null)).isEqualTo(Operators.isEqualTo(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupBy() {
        assertThat(JRoutineCore.on(Operators.<Number>groupBy(3))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(JRoutineCore.on(Operators.<Number>groupBy(13))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.<Number>groupBy(3, 0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, 0, 0));
        assertThat(JRoutineCore.on(Operators.<Number>groupBy(13, -1))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(JRoutineCore.on(Operators.<Number>groupBy(3, -31))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, -31, -31));
        assertThat(JRoutineCore.on(Operators.<Number>groupBy(13, 71))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.identity())
                               .asyncCall(1, "test")
                               .afterMax(seconds(3))
                               .all()).containsExactly(1, "test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInstanceOf() {
        assertThat(JRoutineCore.on(Operators.isInstanceOf(String.class))
                               .asyncCall(3, "test")
                               .afterMax(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.on(Operators.isInstanceOf(Number.class))
                               .asyncCall()
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.limit(5))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(JRoutineCore.on(Operators.limit(0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.on(Operators.limit(15))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.<String>max())
                               .asyncCall("Z TEST", "test")
                               .afterMax(seconds(3))
                               .next()).isEqualTo("test");
        assertThat(JRoutineCore.on(Operators.maxBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall("Z TEST", "test")
                               .afterMax(seconds(3))
                               .next()).isEqualTo("Z TEST");
        assertThat(
                JRoutineCore.on(Operators.max()).asyncCall().afterMax(seconds(3)).all()).isEmpty();
        assertThat(JRoutineCore.on(Operators.maxBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall()
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.<String>min())
                               .asyncCall("Z TEST", "test")
                               .afterMax(seconds(3))
                               .next()).isEqualTo("Z TEST");
        assertThat(JRoutineCore.on(Operators.minBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall("Z TEST", "test")
                               .afterMax(seconds(3))
                               .next()).isEqualTo("test");
        assertThat(
                JRoutineCore.on(Operators.min()).asyncCall().afterMax(seconds(3)).all()).isEmpty();
        assertThat(JRoutineCore.on(Operators.minBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall()
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.none())
                               .asyncCall("test1", null, 3)
                               .afterMax(seconds(3))
                               .all()).isEmpty();
        assertThat(
                JRoutineCore.on(Operators.none()).asyncCall().afterMax(seconds(3)).all()).isEmpty();
    }

    @Test
    public void testNoneMatch() {
        assertThat(JRoutineCore.on(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.on(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.on(Operators.noneMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsExactly(false);
    }

    @Test
    public void testNotAllMatch() {
        assertThat(JRoutineCore.on(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(true);
        assertThat(JRoutineCore.on(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test".equals(s);
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).all()).containsExactly(false);
        assertThat(JRoutineCore.on(Operators.notAllMatch(new Predicate<String>() {

            public boolean test(final String s) {
                return "test1".equals(s);
            }
        })).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsExactly(true);
    }

    @Test
    public void testNotEqualTo() {
        assertThat(JRoutineCore.on(Operators.isNotEqualTo("test"))
                               .asyncCall("test", "test1", "test")
                               .afterMax(seconds(3))
                               .all()).containsExactly("test1");
        assertThat(JRoutineCore.on(Operators.isNotEqualTo(0))
                               .asyncCall()
                               .afterMax(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isNotEqualTo("test")).isEqualTo(Operators.isNotEqualTo("test"));
        assertThat(Operators.isNotEqualTo(null)).isEqualTo(Operators.isNotEqualTo(null));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNotInstanceOf() {
        assertThat(JRoutineCore.on(Operators.isNotInstanceOf(String.class))
                               .asyncCall(3, "test")
                               .afterMax(seconds(3))
                               .all()).containsExactly(3);
        assertThat(JRoutineCore.on(Operators.isNotInstanceOf(Number.class))
                               .asyncCall()
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.isNotSameAs(ref))
                               .asyncCall("test", "test1", ref)
                               .afterMax(seconds(3))
                               .all()).containsExactly("test", "test1");
        assertThat(JRoutineCore.on(Operators.isNotSameAs(0)).asyncCall().afterMax(seconds(3)).all())
                .isEmpty();
        assertThat(Operators.isNotSameAs(ref)).isNotSameAs(Operators.isEqualTo(ref));
        assertThat(Operators.isNotSameAs(null)).isNotSameAs(Operators.isEqualTo(null));
    }

    @Test
    public void testNull() {
        assertThat(JRoutineCore.on(Operators.isNotNull())
                               .asyncCall(3, null, "test", null)
                               .afterMax(seconds(3))
                               .all()).containsExactly(3, "test");
        assertThat(JRoutineCore.on(Operators.isNull())
                               .asyncCall(3, null, "test", null)
                               .afterMax(seconds(3))
                               .all()).containsExactly(null, null);
    }

    @Test
    public void testSameAs() {
        final Object ref = new Object();
        assertThat(JRoutineCore.on(Operators.isSameAs(ref))
                               .asyncCall("test", "test1", ref)
                               .afterMax(seconds(3))
                               .all()).containsExactly(ref);
        assertThat(JRoutineCore.on(Operators.isSameAs(0))
                               .asyncCall()
                               .afterMax(seconds(3))
                               .all()).isEmpty();
        assertThat(Operators.isSameAs(ref)).isEqualTo(Operators.isSameAs(ref));
        assertThat(Operators.isSameAs(null)).isEqualTo(Operators.isSameAs(null));
    }

    @Test
    public void testSkip() {
        assertThat(JRoutineCore.on(Operators.skip(5))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(JRoutineCore.on(Operators.skip(15))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
                               .all()).isEmpty();
        assertThat(JRoutineCore.on(Operators.skip(0))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .afterMax(seconds(3))
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
        assertThat(JRoutineCore.on(Operators.<Integer>sort())
                               .asyncCall(2, 5, 4, 3, 1)
                               .afterMax(seconds(3))
                               .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(JRoutineCore.on(Operators.<String>sort())
                               .asyncCall("a", "C", "b")
                               .afterMax(seconds(3))
                               .all()).containsExactly("C", "a", "b");
        assertThat(JRoutineCore.on(Operators.sortBy(String.CASE_INSENSITIVE_ORDER))
                               .asyncCall("a", "C", "b")
                               .afterMax(seconds(3))
                               .all()).containsExactly("a", "b", "C");
    }

    @Test
    public void testSum() {
        assertThat(
                JRoutineCore.on(Operators.sum()).asyncCall().afterMax(seconds(3)).next()).isEqualTo(
                0);
        assertThat(JRoutineCore.on(Operators.sum())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(10);
    }

    @Test
    public void testSumBig() {
        assertThat(JRoutineCore.on(Operators.sumBig())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(new BigDecimal(10));
    }

    @Test
    public void testSumByte() {
        assertThat(JRoutineCore.on(Operators.sumByte())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 0);
        assertThat(JRoutineCore.on(Operators.sumByte())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((byte) 10);
    }

    @Test
    public void testSumDouble() {
        assertThat(JRoutineCore.on(Operators.sumDouble())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0d);
        assertThat(JRoutineCore.on(Operators.sumDouble())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(10d);
    }

    @Test
    public void testSumFloat() {
        assertThat(JRoutineCore.on(Operators.sumFloat())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0f);
        assertThat(JRoutineCore.on(Operators.sumFloat())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(10f);
    }

    @Test
    public void testSumInteger() {
        assertThat(JRoutineCore.on(Operators.sumInteger())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0);
        assertThat(JRoutineCore.on(Operators.sumInteger())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(10);
    }

    @Test
    public void testSumLong() {
        assertThat(JRoutineCore.on(Operators.sumLong())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo(0L);
        assertThat(JRoutineCore.on(Operators.sumLong())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo(10L);
    }

    @Test
    public void testSumShort() {
        assertThat(JRoutineCore.on(Operators.sumShort())
                               .asyncCall()
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 0);
        assertThat(JRoutineCore.on(Operators.sumShort())
                               .asyncCall(1, 2, 3, 4)
                               .afterMax(seconds(3))
                               .next()).isEqualTo((short) 10);
    }

    @Test
    public void testToList() {
        assertThat(JRoutineCore.on(Operators.toList())
                               .asyncCall("test", "test")
                               .afterMax(seconds(3))
                               .next()).isEqualTo(Arrays.asList("test", "test"));
        assertThat(JRoutineCore.on(Operators.toList())
                               .asyncCall("test1", "test2")
                               .afterMax(seconds(3))
                               .next()).isEqualTo(Arrays.asList("test1", "test2"));
    }

    @Test
    public void testToMap() {
        assertThat(JRoutineCore.on(Operators.toMap(new Function<String, Integer>() {

            public Integer apply(final String s) {
                return s.hashCode();
            }
        })).asyncCall("test", "test").afterMax(seconds(3)).next()).isEqualTo(
                Collections.singletonMap("test".hashCode(), "test"));
        assertThat(JRoutineCore.on(Operators.toMap(new Function<String, Integer>() {

            public Integer apply(final String s) {
                return s.hashCode();
            }
        })).asyncCall("test1", "test2").afterMax(seconds(3)).next()).isEqualTo(
                new HashMap<Integer, String>() {{
                    put("test1".hashCode(), "test1");
                    put("test2".hashCode(), "test2");
                }});
    }

    @Test
    public void testToSet() {
        assertThat(JRoutineCore.on(Operators.toSet())
                               .asyncCall("test", "test")
                               .afterMax(seconds(3))
                               .next()).isEqualTo(Collections.singleton("test"));
        assertThat(JRoutineCore.on(Operators.toSet())
                               .asyncCall("test1", "test2")
                               .afterMax(seconds(3))
                               .next()).isEqualTo(
                new HashSet<String>(Arrays.asList("test1", "test2")));
    }

    @Test
    public void testUnfold() {
        assertThat(JRoutineCore.on(Operators.<Number>groupBy(3))
                               .asyncCall(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                               .bind(JRoutineCore.on(Operators.<Number>unfold()).parallelInvoke())
                               .result()
                               .afterMax(seconds(3))
                               .all()).containsOnly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testUnique() {
        assertThat(JRoutineCore.on(Operators.unique())
                               .asyncCall("test", "test")
                               .afterMax(seconds(3))
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.on(Operators.unique())
                               .asyncCall("test1", "test2")
                               .afterMax(seconds(3))
                               .all()).containsExactly("test1", "test2");
    }
}
