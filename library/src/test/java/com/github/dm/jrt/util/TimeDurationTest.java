/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.util;

import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.util.TimeDuration.Check;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Time duration unit tests.
 * <p/>
 * Created by davide-maestroni on 10/02/2014.
 */
public class TimeDurationTest {

    private static final long ONE_DAY_NANOS = TimeDuration.days(1).toNanos();

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

            TimeDuration.seconds(1).waitTrue(null, new Check() {

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

                TimeDuration.seconds(1).waitTrue(this, null);
            }

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }

        try {

            TimeDuration.seconds(1).waitTrue(null, null);

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    public void testDayConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(TimeDuration.days(sRandom.nextInt(MAX_DURATION)), true);

        final TimeUnit days = getUnit("DAYS");
        if (days != null) {

            testConversions(TimeDuration.fromUnit(sRandom.nextInt(MAX_DURATION), days), true);
        }
    }

    @Test
    public void testDayError() {

        try {

            TimeDuration.days(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final TimeUnit days = getUnit("DAYS");

            if (days != null) {

                TimeDuration.fromUnit(-1, days);

                fail();
            }

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testDayOverflowError() {

        try {

            TimeDuration.days(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.days(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testEqualConversions() throws InvocationTargetException, IllegalAccessException {

        final TimeDuration duration = TimeDuration.nanos(sRandom.nextInt(MAX_DURATION));
        assertThat(duration).isEqualTo(duration);
        assertThat(duration).isEqualTo(duration.nanosTime());
        assertThat(duration).isNotEqualTo(duration.millisTime());
        assertThat(duration.equals(new Object())).isFalse();
        assertThat(duration.hashCode()).isEqualTo(duration.nanosTime().hashCode());
    }

    @Test
    public void testHourConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(TimeDuration.hours(sRandom.nextInt(MAX_DURATION)), true);

        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {

            testConversions(TimeDuration.fromUnit(sRandom.nextInt(MAX_DURATION), hours), true);
        }
    }

    @Test
    public void testHourError() {

        try {

            TimeDuration.hours(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final TimeUnit hours = getUnit("HOURS");

            if (hours != null) {

                TimeDuration.fromUnit(-1, hours);

                fail();
            }

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testHourOverflowError() {

        try {

            TimeDuration.hours(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.hours(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInfinite() {

        assertThat(TimeDuration.INFINITY.isZero()).isFalse();
        assertThat(TimeDuration.INFINITY.isInfinite()).isTrue();
    }

    @Test
    public void testJoin() throws InterruptedException {

        final Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();

                try {

                    TimeDuration.millis(100).sleep();

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }
            }
        };

        thread.start();

        TimeDuration.seconds(1).join(thread);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testJoinError() {

        try {

            TimeDuration.seconds(1).join(null);

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    public void testMicroConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(TimeDuration.micros(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.MICROSECONDS),
                        true);
    }

    @Test
    public void testMicroError() {

        try {

            TimeDuration.micros(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.fromUnit(-1, TimeUnit.MICROSECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMilliConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(TimeDuration.millis(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.MILLISECONDS),
                        true);
    }

    @Test
    public void testMilliError() {

        try {

            TimeDuration.millis(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.fromUnit(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMinus() {

        final TimeDuration duration = TimeDuration.days(1);
        assertThat(duration.minus(Time.hours(2).minus(Time.minutes(11)))).isNotEqualTo(
                duration.minus(Time.hours(2)).minus(Time.minutes(11)));
        assertThat(duration.minus(Time.hours(2).minus(Time.minutes(11))).toMillis()).isEqualTo(
                duration.toMillis() - Time.hours(2).toMillis() + Time.minutes(11).toMillis());
        assertThat(duration.minus(Time.hours(2)).minus(Time.minutes(11)).toMillis()).isEqualTo(
                duration.toMillis() - Time.hours(2).toMillis() - Time.minutes(11).toMillis());
        assertThat(TimeDuration.minutes(2).minus(TimeDuration.seconds((191)))).isEqualTo(
                TimeDuration.ZERO);
        assertThat(TimeDuration.ZERO.minus(Time.seconds(-2))).isEqualTo(TimeDuration.seconds(2));
    }

    @Test
    public void testMinuteConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(TimeDuration.minutes(sRandom.nextInt(MAX_DURATION)), true);

        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {

            testConversions(TimeDuration.fromUnit(sRandom.nextInt(MAX_DURATION), minutes), true);
        }
    }

    @Test
    public void testMinuteError() {

        try {

            TimeDuration.minutes(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final TimeUnit minutes = getUnit("MINUTES");

            if (minutes != null) {

                TimeDuration.fromUnit(-1, minutes);

                fail();
            }

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMinuteOverflowError() {

        try {

            TimeDuration.minutes(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.minutes(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testNanoConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(TimeDuration.nanos(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.NANOSECONDS),
                        true);
    }

    @Test
    public void testNanoError() {

        try {

            TimeDuration.nanos(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.fromUnit(-1, TimeUnit.NANOSECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testPlus() {

        final TimeDuration duration = TimeDuration.minutes(2);
        assertThat(duration.plus(Time.hours(2).plus(Time.minutes(11)))).isEqualTo(
                duration.plus(Time.hours(2)).plus(Time.minutes(11)));
        assertThat(duration.plus(Time.hours(2)).plus(Time.minutes(11)).toMillis()).isEqualTo(
                duration.toMillis() + Time.hours(2).toMillis() + Time.minutes(11).toMillis());
        assertThat(TimeDuration.minutes(2).plus(Time.seconds(-191))).isEqualTo(TimeDuration.ZERO);
        assertThat(TimeDuration.ZERO.plus(TimeDuration.seconds(3))).isEqualTo(Time.seconds(3));
        assertThat(TimeDuration.ZERO.plus(Time.seconds(-3))).isEqualTo(TimeDuration.ZERO);
    }

    @Test
    public void testSecondConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(TimeDuration.seconds(sRandom.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.fromUnit(sRandom.nextInt(MAX_DURATION), TimeUnit.SECONDS),
                        true);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSecondError() {

        try {

            TimeDuration.seconds(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.seconds(1).to(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            TimeDuration.fromUnit(-1, TimeUnit.SECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testSinceMillis() throws InterruptedException {

        final long past = System.currentTimeMillis() - 180000;
        assertThat(TimeDuration.timeSinceMillis(past)).isEqualTo(TimeDuration.minutes(3));
        assertThat(TimeDuration.timeSinceMillis(System.currentTimeMillis())).isEqualTo(
                TimeDuration.ZERO);
        final long future = System.currentTimeMillis() + 177777;
        assertThat(TimeDuration.timeSinceMillis(future)).isEqualTo(TimeDuration.ZERO);
        TimeDuration.seconds(1).sleepAtLeast();
        assertThat(TimeDuration.timeSinceMillis(past).toSeconds()).isGreaterThanOrEqualTo(181);
    }

    @Test
    public void testSinceNanos() throws InterruptedException {

        final long past = System.nanoTime() - 180000000000l;
        assertThat(TimeDuration.timeSinceNanos(past).millisTime()).isEqualTo(
                TimeDuration.minutes(3));
        assertThat(TimeDuration.timeSinceNanos(System.nanoTime()).millisTime()).isEqualTo(
                TimeDuration.ZERO);
        final long future = System.nanoTime() + 177777777777l;
        assertThat(TimeDuration.timeSinceNanos(future)).isEqualTo(TimeDuration.ZERO);
        TimeDuration.seconds(1).sleepAtLeast();
        assertThat(TimeDuration.timeSinceNanos(past).toSeconds()).isGreaterThanOrEqualTo(181);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSleepError() {

        try {

            TimeDuration.INFINITY.sleepSinceMillis(System.currentTimeMillis());

            fail();

        } catch (final IllegalStateException ignored) {

        } catch (final InterruptedException ignored) {

        }

        try {

            TimeDuration.INFINITY.sleepSinceNanos(System.nanoTime());

            fail();

        } catch (final IllegalStateException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    public void testSleepMilli() throws InterruptedException {

        long startTime = System.currentTimeMillis();

        TimeDuration.millis(100).sleep();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();

        TimeDuration.millis(100).sleepAtLeast();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis() - 1000;
        assertThat(TimeDuration.millis(100).sleepSinceMillis(startTime)).isFalse();

        startTime = System.currentTimeMillis();
        assertThat(TimeDuration.millis(100).sleepSinceMillis(startTime)).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis() - 1000;
        assertThat(TimeDuration.ZERO.sleepSinceMillis(startTime)).isFalse();

        startTime = System.currentTimeMillis();
        assertThat(TimeDuration.ZERO.sleepSinceMillis(startTime)).isFalse();
    }

    @Test
    public void testSleepNano() throws InterruptedException {

        long startTime = System.nanoTime();

        TimeDuration.nanos(11573573).sleep();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);

        startTime = System.nanoTime();

        TimeDuration.nanos(11573573).sleepAtLeast();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);

        startTime = System.nanoTime() - 100000000;
        assertThat(TimeDuration.nanos(11573573).sleepSinceNanos(startTime)).isFalse();

        startTime = System.nanoTime();
        assertThat(TimeDuration.nanos(11573573).sleepSinceNanos(startTime)).isTrue();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);

        startTime = System.nanoTime() - 100000000;
        assertThat(TimeDuration.ZERO.sleepSinceNanos(startTime)).isFalse();

        startTime = System.nanoTime();
        assertThat(TimeDuration.ZERO.sleepSinceNanos(startTime)).isFalse();
    }

    @Test
    public void testSleepZero() throws InterruptedException {

        long startTime = System.currentTimeMillis();

        TimeDuration.ZERO.sleep();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.currentTimeMillis();

        TimeDuration.ZERO.sleepAtLeast();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testUnitError() {

        try {

            TimeDuration.fromUnit(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testUntilMillis() throws InterruptedException {

        final long future = System.currentTimeMillis() + 180000;
        assertThat(TimeDuration.timeUntilMillis(future)).isEqualTo(TimeDuration.minutes(3));
        assertThat(TimeDuration.timeUntilMillis(System.currentTimeMillis())).isEqualTo(
                TimeDuration.ZERO);
        final long past = System.currentTimeMillis() - 177777;
        assertThat(TimeDuration.timeUntilMillis(past)).isEqualTo(TimeDuration.ZERO);
        TimeDuration.seconds(1).sleepAtLeast();
        assertThat(TimeDuration.timeUntilMillis(future).toSeconds()).isLessThanOrEqualTo(179);
    }

    @Test
    public void testUntilNanos() throws InterruptedException {

        final long future = System.nanoTime() + 180000999999l;
        assertThat(TimeDuration.timeUntilNanos(future).millisTime()).isEqualTo(
                TimeDuration.minutes(3));
        assertThat(TimeDuration.timeUntilNanos(System.nanoTime()).millisTime()).isEqualTo(
                TimeDuration.ZERO);
        final long past = System.nanoTime() - 177777777777l;
        assertThat(TimeDuration.timeUntilNanos(past)).isEqualTo(TimeDuration.ZERO);
        TimeDuration.seconds(1).sleepAtLeast();
        assertThat(TimeDuration.timeUntilNanos(future).toSeconds()).isLessThanOrEqualTo(179);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testWaitError() {

        try {

            TimeDuration.seconds(1).wait(null);

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

            TimeDuration.millis(100).wait(this);
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();

                try {

                    synchronized (this) {

                        TimeDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();

        synchronized (thread) {

            TimeDuration.INFINITY.wait(thread);
        }

        startTime = System.currentTimeMillis() - 1000;
        synchronized (this) {

            assertThat(TimeDuration.millis(100).waitSinceMillis(this, startTime)).isFalse();
        }

        startTime = System.currentTimeMillis();
        synchronized (this) {

            assertThat(TimeDuration.millis(100).waitSinceMillis(this, startTime)).isTrue();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        thread = new Thread() {

            @Override
            public void run() {

                super.run();

                try {

                    synchronized (this) {

                        TimeDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();

        synchronized (thread) {

            TimeDuration.INFINITY.waitSinceMillis(thread, System.currentTimeMillis());
        }
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitNano() throws InterruptedException {

        long startTime = System.nanoTime() - 100000000;
        synchronized (this) {

            assertThat(TimeDuration.nanos(10000573).waitSinceNanos(this, startTime)).isFalse();
        }

        startTime = System.nanoTime();
        synchronized (this) {

            assertThat(TimeDuration.nanos(10000573).waitSinceNanos(this, startTime)).isTrue();
        }
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(10000573);

        final Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();

                try {

                    synchronized (this) {

                        TimeDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();

        synchronized (thread) {

            TimeDuration.INFINITY.waitSinceNanos(thread, System.nanoTime());
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testWaitSinceError() {

        try {

            TimeDuration.seconds(1).waitSinceMillis(null, System.currentTimeMillis());

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }

        try {

            TimeDuration.seconds(1).waitSinceNanos(null, System.nanoTime());

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitTrueMilli() throws InterruptedException {

        final Check alwaysFalse = new Check() {

            public boolean isTrue() {

                return false;
            }
        };
        final Check alwaysTrue = new Check() {

            public boolean isTrue() {

                return true;
            }
        };

        long startTime = System.currentTimeMillis();

        synchronized (this) {

            assertThat(TimeDuration.millis(100).waitTrue(this, alwaysFalse)).isFalse();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();

        synchronized (this) {

            assertThat(TimeDuration.millis(100).waitTrue(this, alwaysTrue)).isTrue();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        final Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();

                try {

                    synchronized (this) {

                        TimeDuration.millis(500).sleepAtLeast();
                        notifyAll();
                    }

                } catch (final InterruptedException e) {

                    throw new InvocationInterruptedException(e);
                }
            }
        };
        thread.start();

        synchronized (thread) {

            assertThat(TimeDuration.INFINITY.waitTrue(thread, new Check() {

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

        final Check alwaysFalse = new Check() {

            public boolean isTrue() {

                return false;
            }
        };
        final Check alwaysTrue = new Check() {

            public boolean isTrue() {

                return true;
            }
        };

        long startTime = System.nanoTime();

        synchronized (this) {

            assertThat(TimeDuration.nanos(10573).waitTrue(this, alwaysFalse)).isFalse();
        }
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.nanoTime();

        synchronized (this) {

            assertThat(TimeDuration.nanos(10573).waitTrue(this, alwaysTrue)).isTrue();
        }
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(0);
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitTrueZero() throws InterruptedException {

        final Check alwaysFalse = new Check() {

            public boolean isTrue() {

                return false;
            }
        };
        final Check alwaysTrue = new Check() {

            public boolean isTrue() {

                return true;
            }
        };

        long startTime = System.currentTimeMillis();

        synchronized (this) {

            assertThat(TimeDuration.ZERO.waitTrue(this, alwaysFalse)).isFalse();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.currentTimeMillis();

        synchronized (this) {

            assertThat(TimeDuration.ZERO.waitTrue(this, alwaysTrue)).isTrue();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);
    }

    @Test
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitZero() throws InterruptedException {

        long startTime = System.currentTimeMillis();

        synchronized (this) {

            TimeDuration.ZERO.wait(this);
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.currentTimeMillis() - 1000;
        synchronized (this) {

            assertThat(TimeDuration.ZERO.waitSinceMillis(this, startTime)).isFalse();
        }

        startTime = System.currentTimeMillis();
        synchronized (this) {

            assertThat(TimeDuration.ZERO.waitSinceMillis(this, startTime)).isFalse();
        }

        startTime = System.nanoTime() - 100000000;
        synchronized (this) {

            assertThat(TimeDuration.ZERO.waitSinceNanos(this, startTime)).isFalse();
        }

        startTime = System.nanoTime();
        synchronized (this) {

            assertThat(TimeDuration.ZERO.waitSinceNanos(this, startTime)).isFalse();
        }
    }

    @Test
    public void testZeroDay() {

        assertThat(TimeDuration.days(0).isZero()).isTrue();

        final TimeUnit days = getUnit("DAYS");
        if (days != null) {

            assertThat(TimeDuration.fromUnit(0, days).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroHour() {

        assertThat(TimeDuration.hours(0).isZero()).isTrue();

        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {

            assertThat(TimeDuration.fromUnit(0, hours).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroMicro() {

        assertThat(TimeDuration.micros(0).isZero()).isTrue();
        assertThat(TimeDuration.fromUnit(0, TimeUnit.MICROSECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroMilli() {

        assertThat(TimeDuration.millis(0).isZero()).isTrue();
        assertThat(TimeDuration.fromUnit(0, TimeUnit.MILLISECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroMinute() {

        assertThat(TimeDuration.minutes(0).isZero()).isTrue();

        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {

            assertThat(TimeDuration.fromUnit(0, minutes).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroNano() {

        assertThat(TimeDuration.nanos(0).isZero()).isTrue();
        assertThat(TimeDuration.fromUnit(0, TimeUnit.NANOSECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroSecond() {

        assertThat(TimeDuration.seconds(0).isZero()).isTrue();
        assertThat(TimeDuration.fromUnit(0, TimeUnit.SECONDS).isZero()).isTrue();
    }

    private void testConversions(final TimeDuration time, final boolean isFirst) throws
            InvocationTargetException, IllegalAccessException {

        final long value = time.time;
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
        assertThat(time).isEqualTo(TimeDuration.fromUnit(time.time, time.unit));
        assertThat(time.nanosTime()).isEqualTo(
                TimeDuration.fromUnit(time.nanosTime().to(time.unit), time.unit));
        assertThat(time.microsTime()).isEqualTo(
                TimeDuration.fromUnit(time.microsTime().to(time.unit), time.unit));
        assertThat(time.millisTime()).isEqualTo(
                TimeDuration.fromUnit(time.millisTime().to(time.unit), time.unit));
        assertThat(time.secondsTime()).isEqualTo(
                TimeDuration.fromUnit(time.secondsTime().to(time.unit), time.unit));
        assertThat(time.minutesTime()).isEqualTo(
                TimeDuration.fromUnit(time.minutesTime().to(time.unit), time.unit));
        assertThat(time.hoursTime()).isEqualTo(
                TimeDuration.fromUnit(time.hoursTime().to(time.unit), time.unit));
        assertThat(time.daysTime()).isEqualTo(
                TimeDuration.fromUnit(time.daysTime().to(time.unit), time.unit));

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
