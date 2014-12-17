/**
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
package com.bmd.jrt.time;

import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.time.TimeDuration.Check;

import junit.framework.TestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Time duration unit tests.
 * <p/>
 * Created by davide on 10/2/14.
 */
public class TimeDurationTest extends TestCase {

    private static final long ONE_DAY_NANOS = TimeDuration.days(1).toNanos();

    private static final int MAX_DURATION =
            (int) Math.min(Integer.MAX_VALUE, Long.MAX_VALUE / ONE_DAY_NANOS);

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

    public void testConversions() throws InvocationTargetException, IllegalAccessException {

        final Random random = new Random();

        testConversions(TimeDuration.nanos(random.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.micros(random.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.millis(random.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.seconds(random.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.minutes(random.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.hours(random.nextInt(MAX_DURATION)), true);
        testConversions(TimeDuration.days(random.nextInt(MAX_DURATION)), true);

        testConversions(TimeDuration.fromUnit(random.nextInt(MAX_DURATION), TimeUnit.NANOSECONDS),
                        true);
        testConversions(TimeDuration.fromUnit(random.nextInt(MAX_DURATION), TimeUnit.MICROSECONDS),
                        true);
        testConversions(TimeDuration.fromUnit(random.nextInt(MAX_DURATION), TimeUnit.MILLISECONDS),
                        true);
        testConversions(TimeDuration.fromUnit(random.nextInt(MAX_DURATION), TimeUnit.SECONDS),
                        true);

        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {

            testConversions(TimeDuration.fromUnit(random.nextInt(MAX_DURATION), minutes), true);
        }

        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {

            testConversions(TimeDuration.fromUnit(random.nextInt(MAX_DURATION), hours), true);
        }

        final TimeUnit days = getUnit("DAYS");
        if (days != null) {

            testConversions(TimeDuration.fromUnit(random.nextInt(MAX_DURATION), days), true);
        }

        final TimeDuration duration = TimeDuration.nanos(random.nextInt(MAX_DURATION));
        assertThat(duration).isEqualTo(duration);
        assertThat(duration).isEqualTo(duration.nanosTime());
        assertThat(duration).isNotEqualTo(duration.millisTime());
        assertThat(duration.equals(new Object())).isFalse();
        assertThat(duration.hashCode()).isEqualTo(duration.nanosTime().hashCode());
    }

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            TimeDuration.fromUnit(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            TimeDuration.seconds(1).to(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            TimeDuration.nanos(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.micros(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.millis(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.seconds(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.minutes(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

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

        try {

            TimeDuration.hours(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

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

        try {

            TimeDuration.days(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

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

        try {

            TimeDuration.fromUnit(-1, TimeUnit.NANOSECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.fromUnit(-1, TimeUnit.MICROSECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.fromUnit(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            TimeDuration.fromUnit(-1, TimeUnit.SECONDS);

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

        try {

            final TimeUnit hours = getUnit("HOURS");

            if (hours != null) {

                TimeDuration.fromUnit(-1, hours);

                fail();
            }

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

        try {

            TimeDuration.seconds(1).join(null);

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }

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

        try {

            TimeDuration.seconds(1).wait(null);

            fail();

        } catch (final NullPointerException ignored) {

        } catch (final InterruptedException ignored) {

        }

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

        try {

            TimeDuration.seconds(1).waitTrue(null, new Check() {

                @Override
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

    public void testInfinite() {

        assertThat(TimeDuration.INFINITY.isZero()).isFalse();
        assertThat(TimeDuration.INFINITY.isInfinite()).isTrue();
    }

    public void testJoin() throws InterruptedException {

        final Thread thread = new Thread() {

            @Override
            public void run() {

                super.run();

                try {

                    TimeDuration.millis(100).sleep();

                } catch (final InterruptedException e) {

                    RoutineInterruptedException.interrupt(e);
                }
            }
        };

        thread.start();

        TimeDuration.seconds(1).join(thread);
    }

    public void testSleep() throws InterruptedException {

        long startTime = System.currentTimeMillis();

        TimeDuration.millis(100).sleep();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.nanoTime();

        TimeDuration.nanos(11573573).sleep();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);

        startTime = System.currentTimeMillis();

        TimeDuration.millis(100).sleepAtLeast();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.nanoTime();

        TimeDuration.nanos(11573573).sleepAtLeast();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);

        startTime = System.currentTimeMillis() - 1000;
        assertThat(TimeDuration.millis(100).sleepSinceMillis(startTime)).isFalse();

        startTime = System.currentTimeMillis();
        assertThat(TimeDuration.millis(100).sleepSinceMillis(startTime)).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.nanoTime() - 100000000;
        assertThat(TimeDuration.nanos(11573573).sleepSinceNanos(startTime)).isFalse();

        startTime = System.nanoTime();
        assertThat(TimeDuration.nanos(11573573).sleepSinceNanos(startTime)).isTrue();
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(11573573);

        startTime = System.currentTimeMillis();

        TimeDuration.ZERO.sleep();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.currentTimeMillis();

        TimeDuration.ZERO.sleepAtLeast();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.currentTimeMillis() - 1000;
        assertThat(TimeDuration.ZERO.sleepSinceMillis(startTime)).isFalse();

        startTime = System.currentTimeMillis();
        assertThat(TimeDuration.ZERO.sleepSinceMillis(startTime)).isFalse();

        startTime = System.nanoTime() - 100000000;
        assertThat(TimeDuration.ZERO.sleepSinceNanos(startTime)).isFalse();

        startTime = System.nanoTime();
        assertThat(TimeDuration.ZERO.sleepSinceNanos(startTime)).isFalse();
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWait() throws InterruptedException {

        long startTime = System.currentTimeMillis();

        synchronized (this) {

            TimeDuration.millis(100).wait(this);
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();

        synchronized (this) {

            TimeDuration.ZERO.wait(this);
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

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

                    RoutineInterruptedException.interrupt(e);
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

        startTime = System.nanoTime() - 100000000;
        synchronized (this) {

            assertThat(TimeDuration.nanos(10000573).waitSinceNanos(this, startTime)).isFalse();
        }

        startTime = System.nanoTime();
        synchronized (this) {

            assertThat(TimeDuration.nanos(10000573).waitSinceNanos(this, startTime)).isTrue();
        }
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(10000573);

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

                    RoutineInterruptedException.interrupt(e);
                }
            }
        };
        thread.start();

        synchronized (thread) {

            TimeDuration.INFINITY.waitSinceMillis(thread, System.currentTimeMillis());
        }

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

                    RoutineInterruptedException.interrupt(e);
                }
            }
        };
        thread.start();

        synchronized (thread) {

            TimeDuration.INFINITY.waitSinceNanos(thread, System.nanoTime());
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void testWaitTrue() throws InterruptedException {

        final Check alwaysFalse = new Check() {

            @Override
            public boolean isTrue() {

                return false;
            }
        };
        final Check alwaysTrue = new Check() {

            @Override
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

            assertThat(TimeDuration.ZERO.waitTrue(this, alwaysFalse)).isFalse();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.currentTimeMillis();

        synchronized (this) {

            assertThat(TimeDuration.millis(100).waitTrue(this, alwaysTrue)).isTrue();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.currentTimeMillis();

        synchronized (this) {

            assertThat(TimeDuration.ZERO.waitTrue(this, alwaysTrue)).isTrue();
        }
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(0);

        startTime = System.nanoTime();

        synchronized (this) {

            assertThat(TimeDuration.nanos(10573).waitTrue(this, alwaysFalse)).isFalse();
        }
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.nanoTime();

        synchronized (this) {

            assertThat(TimeDuration.nanos(10573).waitTrue(this, alwaysTrue)).isTrue();
        }
        assertThat(System.nanoTime() - startTime).isGreaterThanOrEqualTo(0);

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

                    RoutineInterruptedException.interrupt(e);
                }
            }
        };
        thread.start();

        synchronized (thread) {

            assertThat(TimeDuration.INFINITY.waitTrue(thread, new Check() {

                private boolean mToggle = true;

                @Override
                public boolean isTrue() {

                    return (mToggle = !mToggle);
                }
            })).isTrue();
        }
    }

    public void testZero() {

        assertThat(TimeDuration.nanos(0).isZero()).isTrue();
        assertThat(TimeDuration.micros(0).isZero()).isTrue();
        assertThat(TimeDuration.millis(0).isZero()).isTrue();
        assertThat(TimeDuration.seconds(0).isZero()).isTrue();
        assertThat(TimeDuration.minutes(0).isZero()).isTrue();
        assertThat(TimeDuration.hours(0).isZero()).isTrue();
        assertThat(TimeDuration.days(0).isZero()).isTrue();

        assertThat(TimeDuration.fromUnit(0, TimeUnit.NANOSECONDS).isZero()).isTrue();
        assertThat(TimeDuration.fromUnit(0, TimeUnit.MICROSECONDS).isZero()).isTrue();
        assertThat(TimeDuration.fromUnit(0, TimeUnit.MILLISECONDS).isZero()).isTrue();
        assertThat(TimeDuration.fromUnit(0, TimeUnit.SECONDS).isZero()).isTrue();

        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {

            assertThat(TimeDuration.fromUnit(0, minutes).isZero()).isTrue();
        }

        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {

            assertThat(TimeDuration.fromUnit(0, hours).isZero()).isTrue();
        }

        final TimeUnit days = getUnit("DAYS");
        if (days != null) {

            assertThat(TimeDuration.fromUnit(0, days).isZero()).isTrue();
        }
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
