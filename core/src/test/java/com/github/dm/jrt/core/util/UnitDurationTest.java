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

package com.github.dm.jrt.core.util;

import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.util.UnitDuration.Condition;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.infinity;
import static com.github.dm.jrt.core.util.UnitDuration.zero;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Time duration unit tests.
 * <p>
 * Created by davide-maestroni on 10/02/2014.
 */
public class UnitDurationTest {

    private static final long ONE_DAY_NANOS = UnitDuration.days(1).toNanos();

    private static final int MAX_DURATION =
            (int) Math.min(Integer.MAX_VALUE, Long.MAX_VALUE / ONE_DAY_NANOS);

    private static final Random sRandom = new Random();

    private static Method getMethod(final String name) {

        try {
            return TimeUnit.class.getMethod(name, long.class);

        } catch (final NoSuchMethodException ignored) {

        }

        return null;
    }

    private static TimeUnit getUnit(final String name) {

        try {
            return TimeUnit.valueOf(name);

        } catch (final IllegalArgumentException ignored) {

        }

        return null;
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCheckError() {

        try {
            UnitDuration.seconds(1).waitTrue(null, new Condition() {

                public boolean isTrue() {

                    return false;
                }
            });
            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }

        try {
            synchronized (this) {
                UnitDuration.seconds(1).waitTrue(this, null);
            }

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }

        try {
            UnitDuration.seconds(1).waitTrue(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    public void testDayConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitDuration.days(sRandom.nextInt(MAX_DURATION)), true);
        final TimeUnit days = getUnit("DAYS");
        if (days != null) {
            testConversions(UnitDuration.fromUnit(sRandom.nextInt(MAX_DURATION), days), true);
        }
    }

    @Test
    public void testDayError() {

        try {
            UnitDuration.days(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            final TimeUnit days = getUnit("DAYS");
            if (days != null) {
                UnitDuration.fromUnit(-1, days);
                fail();
            }

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testDayOverflowError() {

        try {
            UnitDuration.days(Long.MAX_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.days(Long.MIN_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.days(Double.MAX_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.days(Double.MIN_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testEqualConversions() throws InvocationTargetException, IllegalAccessException {

        final UnitDuration duration = UnitDuration.nanos(sRandom.nextInt(MAX_DURATION));
        assertThat(duration).isEqualTo(duration);
        assertThat(duration).isEqualTo(duration.nanosTime());
        assertThat(duration).isNotEqualTo(duration.millisTime());
        assertThat(duration.equals(new Object())).isFalse();
        assertThat(duration.hashCode()).isEqualTo(duration.nanosTime().hashCode());
    }

    @Test
    public void testFloatTime() {

        assertThat(UnitDuration.days(1.6)).isEqualTo(UnitDuration.hours(38));
        assertThat(UnitDuration.hours(1.6)).isEqualTo(UnitDuration.minutes(96));
        assertThat(UnitDuration.minutes(1.6)).isEqualTo(UnitDuration.seconds(96));
        assertThat(UnitDuration.seconds(1.6)).isEqualTo(UnitDuration.millis(1600));
        assertThat(UnitDuration.millis(1.6)).isEqualTo(UnitDuration.micros(1600));
        assertThat(UnitDuration.micros(1.6)).isEqualTo(UnitDuration.nanos(1600));
        assertThat(UnitDuration.nanos(1.6)).isEqualTo(UnitDuration.nanos(2));

        assertThat(UnitDuration.days(3.0)).isEqualTo(UnitDuration.days(3));
        assertThat(UnitDuration.hours(3.0)).isEqualTo(UnitDuration.hours(3));
        assertThat(UnitDuration.minutes(3.0)).isEqualTo(UnitDuration.minutes(3));
        assertThat(UnitDuration.seconds(3.0)).isEqualTo(UnitDuration.seconds(3));
        assertThat(UnitDuration.millis(3.0)).isEqualTo(UnitDuration.millis(3));
        assertThat(UnitDuration.micros(3.0)).isEqualTo(UnitDuration.micros(3));
        assertThat(UnitDuration.nanos(3.0)).isEqualTo(UnitDuration.nanos(3));
    }

    @Test
    public void testHourConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitDuration.hours(sRandom.nextInt(MAX_DURATION)), true);
        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {
            testConversions(UnitDuration.fromUnit(sRandom.nextInt(MAX_DURATION), hours), true);
        }
    }

    @Test
    public void testHourError() {

        try {
            UnitDuration.hours(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            final TimeUnit hours = getUnit("HOURS");
            if (hours != null) {
                UnitDuration.fromUnit(-1, hours);
                fail();
            }

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testHourOverflowError() {

        try {
            UnitDuration.hours(Long.MAX_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.hours(Long.MIN_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.hours(Double.MAX_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.hours(Double.MIN_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInfinite() {

        assertThat(infinity().isZero()).isFalse();
        assertThat(infinity().isInfinite()).isTrue();
    }

    @Test
    public void testJoin() throws InterruptedException {

        final Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();
                try {
                    UnitDuration.millis(100).sleep();

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }
            }
        };

        thread.start();
        UnitDuration.seconds(1).join(thread);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testJoinError() {

        try {
            UnitDuration.seconds(1).join(null);
            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    public void testMicroConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitDuration.micros(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(UnitDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.MICROSECONDS),
                true);
    }

    @Test
    public void testMicroError() {

        try {
            UnitDuration.micros(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.fromUnit(-1, TimeUnit.MICROSECONDS);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMilliConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitDuration.millis(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(UnitDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.MILLISECONDS),
                true);
    }

    @Test
    public void testMilliError() {

        try {
            UnitDuration.millis(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.fromUnit(-1, TimeUnit.MILLISECONDS);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMinus() {

        final UnitDuration duration = UnitDuration.days(1);
        assertThat(duration.minus(UnitTime.hours(2).minus(UnitTime.minutes(11)))).isNotEqualTo(
                duration.minus(UnitTime.hours(2)).minus(UnitTime.minutes(11)));
        assertThat(
                duration.minus(UnitTime.hours(2).minus(UnitTime.minutes(11))).toMillis()).isEqualTo(
                duration.toMillis() - UnitTime.hours(2).toMillis() + UnitTime.minutes(11)
                                                                             .toMillis());
        assertThat(
                duration.minus(UnitTime.hours(2)).minus(UnitTime.minutes(11)).toMillis()).isEqualTo(
                duration.toMillis() - UnitTime.hours(2).toMillis() - UnitTime.minutes(11)
                                                                             .toMillis());
        assertThat(UnitDuration.minutes(2).minus(UnitDuration.seconds((191)))).isEqualTo(
                UnitDuration.seconds(Long.MAX_VALUE - 70));
        assertThat(zero().minus(UnitTime.seconds(-2))).isEqualTo(UnitDuration.seconds(2));
        assertThat(UnitDuration.seconds(1).minus(UnitDuration.millis(700))).isEqualTo(
                UnitDuration.millis(300));
    }

    @Test
    public void testMinuteConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitDuration.minutes(sRandom.nextInt(MAX_DURATION)), true);
        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {
            testConversions(UnitDuration.fromUnit(sRandom.nextInt(MAX_DURATION), minutes), true);
        }
    }

    @Test
    public void testMinuteError() {

        try {
            UnitDuration.minutes(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            final TimeUnit minutes = getUnit("MINUTES");
            if (minutes != null) {
                UnitDuration.fromUnit(-1, minutes);
                fail();
            }

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMinuteOverflowError() {

        try {
            UnitDuration.minutes(Long.MAX_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.minutes(Long.MIN_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.minutes(Double.MAX_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.minutes(Double.MIN_VALUE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testNanoConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitDuration.nanos(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(UnitDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.NANOSECONDS),
                true);
    }

    @Test
    public void testNanoError() {

        try {
            UnitDuration.nanos(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.fromUnit(-1, TimeUnit.NANOSECONDS);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testPlus() {

        final UnitDuration duration = UnitDuration.minutes(2);
        assertThat(duration.plus(UnitTime.hours(2).plus(UnitTime.minutes(11)))).isEqualTo(
                duration.plus(UnitTime.hours(2)).plus(UnitTime.minutes(11)));
        assertThat(
                duration.plus(UnitTime.hours(2)).plus(UnitTime.minutes(11)).toMillis()).isEqualTo(
                duration.toMillis() + UnitTime.hours(2).toMillis() + UnitTime.minutes(11)
                                                                             .toMillis());
        assertThat(UnitDuration.minutes(2).plus(UnitTime.seconds(-191))).isEqualTo(
                UnitDuration.seconds(Long.MAX_VALUE - 70));
        assertThat(zero(TimeUnit.SECONDS).plus(UnitDuration.seconds(3))).isEqualTo(
                UnitDuration.seconds(3));
        assertThat(zero(TimeUnit.SECONDS).plus(UnitTime.seconds(-3))).isEqualTo(
                UnitDuration.seconds(Long.MAX_VALUE - 2));
        assertThat(UnitDuration.seconds(1).plus(UnitTime.millis(-700))).isEqualTo(
                UnitDuration.millis(300));
    }

    @Test
    public void testSecondConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitDuration.seconds(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(UnitDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.SECONDS),
                true);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSecondError() {

        try {
            UnitDuration.seconds(-1);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            UnitDuration.seconds(1).to(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            UnitDuration.fromUnit(-1, TimeUnit.SECONDS);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testSinceMillis() throws InterruptedException {

        final long past = System.currentTimeMillis() - 180000;
        assertThat(UnitDuration.timeSinceMillis(past)).isEqualTo(UnitDuration.minutes(3));
        assertThat(UnitDuration.timeSinceMillis(System.currentTimeMillis())).isEqualTo(zero());
        final long future = System.currentTimeMillis() + 177777;
        assertThat(UnitDuration.timeSinceMillis(future)).isEqualTo(zero());
        UnitDuration.millis(1100).sleepAtLeast();
        assertThat(UnitDuration.timeSinceMillis(past).toSeconds()).isGreaterThanOrEqualTo(181);
    }

    @Test
    public void testSinceNanos() throws InterruptedException {

        final long past = System.nanoTime() - 180000000000L;
        assertThat(UnitDuration.timeSinceNanos(past).millisTime()).isEqualTo(
                UnitDuration.minutes(3));
        assertThat(UnitDuration.timeSinceNanos(System.nanoTime()).millisTime()).isEqualTo(zero());
        final long future = System.nanoTime() + 177777777777L;
        assertThat(UnitDuration.timeSinceNanos(future)).isEqualTo(zero());
        UnitDuration.millis(1100).sleepAtLeast();
        assertThat(UnitDuration.timeSinceNanos(past).toSeconds()).isGreaterThanOrEqualTo(181);
    }

    @Test
    public void testSleepMilli() throws InterruptedException {

        long startTime = System.currentTimeMillis();
        UnitDuration.millis(100).sleep();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
        startTime = System.currentTimeMillis();
        UnitDuration.millis(100).sleepAtLeast();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
        startTime = System.currentTimeMillis() - 1000;
        assertThat(UnitDuration.millis(100).sleepSinceMillis(startTime)).isFalse();
        startTime = System.currentTimeMillis();
        assertThat(UnitDuration.millis(100).sleepSinceMillis(startTime)).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
        startTime = System.currentTimeMillis() - 1000;
        assertThat(zero().sleepSinceMillis(startTime)).isFalse();
        startTime = System.currentTimeMillis();
        assertThat(zero().sleepSinceMillis(startTime)).isFalse();
    }

    @Test
    public void testSleepNano() throws InterruptedException {

        long startTime = System.nanoTime();
        UnitDuration.nanos(11573573).sleep();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);
        startTime = System.nanoTime();
        UnitDuration.nanos(11573573).sleepAtLeast();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);
        startTime = System.nanoTime() - 100000000;
        assertThat(UnitDuration.nanos(11573573).sleepSinceNanos(startTime)).isFalse();
        startTime = System.nanoTime();
        assertThat(UnitDuration.nanos(11573573).sleepSinceNanos(startTime)).isTrue();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);
        startTime = System.nanoTime() - 100000000;
        assertThat(zero().sleepSinceNanos(startTime)).isFalse();
        startTime = System.nanoTime();
        assertThat(zero().sleepSinceNanos(startTime)).isFalse();
    }

    @Test
    public void testSleepZero() throws InterruptedException {

        long startTime = System.currentTimeMillis();
        zero().sleep();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
        startTime = System.currentTimeMillis();
        zero().sleepAtLeast();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testUnitError() {

        try {
            UnitDuration.fromUnit(0, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testUntilMillis() throws InterruptedException {

        final long future = System.currentTimeMillis() + 180000;
        assertThat(UnitDuration.timeUntilMillis(future)).isEqualTo(UnitDuration.minutes(3));
        assertThat(UnitDuration.timeUntilMillis(System.currentTimeMillis())).isEqualTo(zero());
        final long past = System.currentTimeMillis() - 177777;
        assertThat(UnitDuration.timeUntilMillis(past)).isEqualTo(zero());
        UnitDuration.seconds(1).sleepAtLeast();
        assertThat(UnitDuration.timeUntilMillis(future).toSeconds()).isLessThanOrEqualTo(179);
    }

    @Test
    public void testUntilNanos() throws InterruptedException {

        final long future = System.nanoTime() + 180000999999L;
        assertThat(UnitDuration.timeUntilNanos(future).millisTime()).isEqualTo(
                UnitDuration.minutes(3));
        assertThat(UnitDuration.timeUntilNanos(System.nanoTime()).millisTime()).isEqualTo(zero());
        final long past = System.nanoTime() - 177777777777L;
        assertThat(UnitDuration.timeUntilNanos(past)).isEqualTo(zero());
        UnitDuration.seconds(1).sleepAtLeast();
        assertThat(UnitDuration.timeUntilNanos(future).toSeconds()).isLessThanOrEqualTo(179);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testWaitError() {

        try {
            UnitDuration.seconds(1).wait(null);
            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitMilli() throws InterruptedException {

        long startTime = System.currentTimeMillis();
        synchronized (this) {
            UnitDuration.millis(100).wait(this);
        }

        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
        Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();
                try {
                    synchronized (this) {
                        UnitDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();
        synchronized (thread) {
            infinity().wait(thread);
        }

        startTime = System.currentTimeMillis() - 1000;
        synchronized (this) {
            assertThat(UnitDuration.millis(100).waitSinceMillis(this, startTime)).isFalse();
        }

        startTime = System.currentTimeMillis();
        synchronized (this) {
            assertThat(UnitDuration.millis(100).waitSinceMillis(this, startTime)).isTrue();
        }

        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
        thread = new Thread() {

            @Override
            public void run() {

                super.run();
                try {
                    synchronized (this) {
                        UnitDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();
        synchronized (thread) {
            infinity().waitSinceMillis(thread, System.currentTimeMillis());
        }
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitNano() throws InterruptedException {

        long startTime = System.nanoTime() - 100000000;
        synchronized (this) {
            assertThat(UnitDuration.nanos(10000573).waitSinceNanos(this, startTime)).isFalse();
        }

        startTime = System.nanoTime();
        synchronized (this) {
            assertThat(UnitDuration.nanos(10000573).waitSinceNanos(this, startTime)).isTrue();
        }

        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(10000573);
        final Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();
                try {
                    synchronized (this) {
                        UnitDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {
                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();
        synchronized (thread) {
            infinity().waitSinceNanos(thread, System.nanoTime());
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testWaitSinceError() {

        try {
            UnitDuration.seconds(1).waitSinceMillis(null, System.currentTimeMillis());
            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }

        try {
            UnitDuration.seconds(1).waitSinceNanos(null, System.nanoTime());
            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitTrueMilli() throws InterruptedException {

        final Condition alwaysFalse = new Condition() {

            public boolean isTrue() {

                return false;
            }
        };
        final Condition alwaysTrue = new Condition() {

            public boolean isTrue() {

                return true;
            }
        };

        long startTime = System.currentTimeMillis();
        synchronized (this) {
            assertThat(UnitDuration.millis(100).waitTrue(this, alwaysFalse)).isFalse();
        }

        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
        startTime = System.currentTimeMillis();
        synchronized (this) {
            assertThat(UnitDuration.millis(100).waitTrue(this, alwaysTrue)).isTrue();
        }

        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
        final Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();
                try {
                    synchronized (this) {
                        UnitDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();
        synchronized (thread) {
            assertThat(infinity().waitTrue(thread, new Condition() {

                private boolean mToggle = true;

                public boolean isTrue() {

                    return (mToggle = !mToggle);
                }
            })).isTrue();
        }
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitTrueNano() throws InterruptedException {

        final Condition alwaysFalse = new Condition() {

            public boolean isTrue() {

                return false;
            }
        };
        final Condition alwaysTrue = new Condition() {

            public boolean isTrue() {

                return true;
            }
        };

        long startTime = System.nanoTime();
        synchronized (this) {
            assertThat(UnitDuration.nanos(10573).waitTrue(this, alwaysFalse)).isFalse();
        }

        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(100);
        startTime = System.nanoTime();
        synchronized (this) {
            assertThat(UnitDuration.nanos(10573).waitTrue(this, alwaysTrue)).isTrue();
        }

        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(0);
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitTrueZero() throws InterruptedException {

        final Condition alwaysFalse = new Condition() {

            public boolean isTrue() {

                return false;
            }
        };
        final Condition alwaysTrue = new Condition() {

            public boolean isTrue() {

                return true;
            }
        };

        long startTime = System.currentTimeMillis();
        synchronized (this) {
            assertThat(zero().waitTrue(this, alwaysFalse)).isFalse();
        }

        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
        startTime = System.currentTimeMillis();
        synchronized (this) {
            assertThat(zero().waitTrue(this, alwaysTrue)).isTrue();
        }

        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitZero() throws InterruptedException {

        long startTime = System.currentTimeMillis();
        synchronized (this) {
            zero().wait(this);
        }

        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
        startTime = System.currentTimeMillis() - 1000;
        synchronized (this) {
            assertThat(zero().waitSinceMillis(this, startTime)).isFalse();
        }

        startTime = System.currentTimeMillis();
        synchronized (this) {
            assertThat(zero().waitSinceMillis(this, startTime)).isFalse();
        }

        startTime = System.nanoTime() - 100000000;
        synchronized (this) {
            assertThat(zero().waitSinceNanos(this, startTime)).isFalse();
        }

        startTime = System.nanoTime();
        synchronized (this) {
            assertThat(zero().waitSinceNanos(this, startTime)).isFalse();
        }
    }

    @Test
    public void testZeroDay() {

        assertThat(UnitDuration.days(0).isZero()).isTrue();
        final TimeUnit days = getUnit("DAYS");
        if (days != null) {
            assertThat(UnitDuration.fromUnit(0, days).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroHour() {

        assertThat(UnitDuration.hours(0).isZero()).isTrue();
        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {
            assertThat(UnitDuration.fromUnit(0, hours).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroMicro() {

        assertThat(UnitDuration.micros(0).isZero()).isTrue();
        assertThat(UnitDuration.fromUnit(0, TimeUnit.MICROSECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroMilli() {

        assertThat(UnitDuration.millis(0).isZero()).isTrue();
        assertThat(UnitDuration.fromUnit(0, TimeUnit.MILLISECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroMinute() {

        assertThat(UnitDuration.minutes(0).isZero()).isTrue();
        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {
            assertThat(UnitDuration.fromUnit(0, minutes).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroNano() {

        assertThat(UnitDuration.nanos(0).isZero()).isTrue();
        assertThat(UnitDuration.fromUnit(0, TimeUnit.NANOSECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroSecond() {

        assertThat(UnitDuration.seconds(0).isZero()).isTrue();
        assertThat(UnitDuration.fromUnit(0, TimeUnit.SECONDS).isZero()).isTrue();
    }

    private void testConversions(final UnitDuration time, final boolean isFirst) throws
            InvocationTargetException, IllegalAccessException {

        final long value = time.value;
        final TimeUnit unit = time.unit;
        assertThat(time.toNanos()).isEqualTo(unit.toNanos(value));
        assertThat(time.toMicros()).isEqualTo(unit.toMicros(value));
        assertThat(time.toMillis()).isEqualTo(unit.toMillis(value));
        assertThat(time.toSeconds()).isEqualTo(unit.toSeconds(value));
        final Method toMinutes = getMethod("toMinutes");
        if (toMinutes != null) {
            assertThat(time.toMinutes()).isEqualTo((Long) toMinutes.invoke(unit, value));
        }

        final Method toHours = getMethod("toHours");
        if (toHours != null) {
            assertThat(time.toHours()).isEqualTo((Long) toHours.invoke(unit, value));
        }

        final Method toDays = getMethod("toDays");
        if (toDays != null) {
            assertThat(time.toDays()).isEqualTo((Long) toDays.invoke(unit, value));
        }

        assertThat(time.to(TimeUnit.NANOSECONDS)).isEqualTo(
                TimeUnit.NANOSECONDS.convert(value, unit));
        assertThat(time.to(TimeUnit.MICROSECONDS)).isEqualTo(
                TimeUnit.MICROSECONDS.convert(value, unit));
        assertThat(time.to(TimeUnit.MILLISECONDS)).isEqualTo(
                TimeUnit.MILLISECONDS.convert(value, unit));
        assertThat(time.to(TimeUnit.SECONDS)).isEqualTo(TimeUnit.SECONDS.convert(value, unit));
        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {
            assertThat(time.to(minutes)).isEqualTo(minutes.convert(value, unit));
        }

        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {
            assertThat(time.to(hours)).isEqualTo(hours.convert(value, unit));
        }

        final TimeUnit days = getUnit("DAYS");
        if (days != null) {
            assertThat(time.to(days)).isEqualTo(days.convert(value, unit));
        }

        assertThat(time).isEqualTo(time);
        assertThat(time).isEqualTo(UnitDuration.fromUnit(time.value, time.unit));
        assertThat(time.nanosTime()).isEqualTo(
                UnitDuration.fromUnit(time.nanosTime().to(time.unit), time.unit));
        assertThat(time.microsTime()).isEqualTo(
                UnitDuration.fromUnit(time.microsTime().to(time.unit), time.unit));
        assertThat(time.millisTime()).isEqualTo(
                UnitDuration.fromUnit(time.millisTime().to(time.unit), time.unit));
        assertThat(time.secondsTime()).isEqualTo(
                UnitDuration.fromUnit(time.secondsTime().to(time.unit), time.unit));
        assertThat(time.minutesTime()).isEqualTo(
                UnitDuration.fromUnit(time.minutesTime().to(time.unit), time.unit));
        assertThat(time.hoursTime()).isEqualTo(
                UnitDuration.fromUnit(time.hoursTime().to(time.unit), time.unit));
        assertThat(time.daysTime()).isEqualTo(
                UnitDuration.fromUnit(time.daysTime().to(time.unit), time.unit));
        if (isFirst) {
            testConversions(time.nanosTime(), false);
            testConversions(time.microsTime(), false);
            testConversions(time.millisTime(), false);
            testConversions(time.secondsTime(), false);
            testConversions(time.minutesTime(), false);
            testConversions(time.hoursTime(), false);
            testConversions(time.daysTime(), false);
        }
    }
}
