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

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Time unit tests.
 * <p>
 * Created by davide-maestroni on 10/02/2014.
 */
public class UnitTimeTest {

    private static final long ONE_DAY_NANOS = UnitTime.days(1).toNanos();

    private static final long MAX_TIME = Long.MAX_VALUE / ONE_DAY_NANOS;

    private static final long MIN_TIME = Long.MIN_VALUE / ONE_DAY_NANOS;

    private static final Random sRandom = new Random();

    private static long clip(final int i) {

        return Math.min(Math.max(MIN_TIME, i), MAX_TIME);
    }

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
    public void testCompare() {

        assertThat(UnitTime.millis(0).compareTo(UnitTime.seconds(0))).isEqualTo(0);
        assertThat(UnitTime.millis(0).compareTo(UnitTime.seconds(-1))).isEqualTo(1);
        assertThat(UnitTime.millis(-1).compareTo(UnitTime.seconds(0))).isEqualTo(-1);
        assertThat(UnitTime.days(0).compareTo(UnitTime.seconds(0))).isEqualTo(0);
        assertThat(UnitTime.days(0).compareTo(UnitTime.seconds(-1))).isEqualTo(1);
        assertThat(UnitTime.days(-1).compareTo(UnitTime.seconds(0))).isEqualTo(-1);
        assertThat(UnitTime.millis(0).isGreaterThan(UnitTime.seconds(0))).isFalse();
        assertThat(UnitTime.millis(0).isGreaterOrEquals(UnitTime.seconds(0))).isTrue();
        assertThat(UnitTime.millis(0).isLessThan(UnitTime.seconds(0))).isFalse();
        assertThat(UnitTime.millis(0).isLessOrEquals(UnitTime.seconds(0))).isTrue();
        assertThat(UnitTime.millis(0).isGreaterThan(UnitTime.seconds(-1))).isTrue();
        assertThat(UnitTime.millis(0).isGreaterOrEquals(UnitTime.seconds(-1))).isTrue();
        assertThat(UnitTime.millis(0).isLessThan(UnitTime.seconds(-1))).isFalse();
        assertThat(UnitTime.millis(0).isLessOrEquals(UnitTime.seconds(-1))).isFalse();
        assertThat(UnitTime.millis(-1).isGreaterThan(UnitTime.seconds(0))).isFalse();
        assertThat(UnitTime.millis(-1).isGreaterOrEquals(UnitTime.seconds(0))).isFalse();
        assertThat(UnitTime.millis(-1).isLessThan(UnitTime.seconds(0))).isTrue();
        assertThat(UnitTime.millis(-1).isLessOrEquals(UnitTime.seconds(0))).isTrue();
    }

    @Test
    public void testConstants() {

        assertThat(UnitTime.SECONDS_IN_MINUTE).isEqualTo(60);
        assertThat(UnitTime.SECONDS_IN_HOUR).isEqualTo(60 * 60);
        assertThat(UnitTime.SECONDS_IN_DAY).isEqualTo(60 * 60 * 24);
        assertThat(UnitTime.MINUTES_IN_HOUR).isEqualTo(60);
        assertThat(UnitTime.MINUTES_IN_DAY).isEqualTo(60 * 24);
        assertThat(UnitTime.HOURS_IN_DAY).isEqualTo(24);
    }

    @Test
    public void testCurrentTime() {

        final long systemTimeMs = System.currentTimeMillis();
        assertThat(UnitTime.current().toMillis()).isBetween(systemTimeMs - 50, systemTimeMs + 50);

        final long systemTimeNs = System.nanoTime();
        assertThat(UnitTime.currentNano().toNanos()).isBetween(systemTimeNs - 50000000,
                systemTimeNs + 50000000);
    }

    @Test
    public void testDayConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitTime.days(clip(sRandom.nextInt())), true);

        final TimeUnit days = getUnit("DAYS");
        if (days != null) {

            testConversions(UnitTime.fromUnit(clip(sRandom.nextInt()), days), true);
        }

    }

    @Test
    public void testDayOverflowError() {

        try {

            UnitTime.days(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            UnitTime.days(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testEqualConversions() throws InvocationTargetException, IllegalAccessException {

        final UnitTime time = UnitTime.nanos(clip(sRandom.nextInt()));
        assertThat(time).isEqualTo(time);
        assertThat(time).isEqualTo(time.nanosTime());
        assertThat(time).isNotEqualTo(time.millisTime());
        assertThat(time.equals(new Object())).isFalse();
        assertThat(time.hashCode()).isEqualTo(time.nanosTime().hashCode());
    }

    @Test
    public void testHourConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitTime.hours(clip(sRandom.nextInt())), true);

        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {

            testConversions(UnitTime.fromUnit(clip(sRandom.nextInt()), hours), true);
        }
    }

    @Test
    public void testHourOverflowError() {

        try {

            UnitTime.hours(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            UnitTime.hours(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMicroConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitTime.micros(clip(sRandom.nextInt())), true);
        testConversions(UnitTime.fromUnit(clip(sRandom.nextInt()), TimeUnit.MICROSECONDS), true);
    }

    @Test
    public void testMilliConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitTime.millis(clip(sRandom.nextInt())), true);
        testConversions(UnitTime.fromUnit(clip(sRandom.nextInt()), TimeUnit.MILLISECONDS), true);
    }

    @Test
    public void testMinus() {

        final UnitTime current = UnitTime.current();
        assertThat(current.minus(UnitTime.hours(2).minus(UnitTime.minutes(11)))).isNotEqualTo(
                current.minus(UnitTime.hours(2)).minus(UnitTime.minutes(11)));
        assertThat(
                current.minus(UnitTime.hours(2).minus(UnitTime.minutes(11))).toMillis()).isEqualTo(
                current.toMillis() - UnitTime.hours(2).toMillis() + UnitTime.minutes(11)
                                                                            .toMillis());
        assertThat(
                current.minus(UnitTime.hours(2)).minus(UnitTime.minutes(11)).toMillis()).isEqualTo(
                current.toMillis() - UnitTime.hours(2).toMillis() - UnitTime.minutes(11)
                                                                            .toMillis());
        assertThat(UnitTime.minutes(-2).minus(UnitTime.seconds(31))).isEqualTo(
                UnitTime.millis(-151000));
        assertThat(UnitTime.seconds(1).minus(UnitTime.millis(700))).isEqualTo(UnitTime.millis(300));
    }

    @Test
    public void testMinuteConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitTime.minutes(clip(sRandom.nextInt())), true);

        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {

            testConversions(UnitTime.fromUnit(clip(sRandom.nextInt()), minutes), true);
        }
    }

    @Test
    public void testMinuteOverflowError() {

        try {

            UnitTime.minutes(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            UnitTime.minutes(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testNanoConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitTime.nanos(clip(sRandom.nextInt())), true);
        testConversions(UnitTime.fromUnit(clip(sRandom.nextInt()), TimeUnit.NANOSECONDS), true);
    }

    @Test
    public void testPlus() {

        final UnitTime current = UnitTime.current();
        assertThat(current.plus(UnitTime.hours(2).plus(UnitTime.minutes(11)))).isEqualTo(
                current.plus(UnitTime.hours(2)).plus(UnitTime.minutes(11)));
        assertThat(current.plus(UnitTime.hours(2)).plus(UnitTime.minutes(11)).toMillis()).isEqualTo(
                current.toMillis() + UnitTime.hours(2).toMillis() + UnitTime.minutes(11)
                                                                            .toMillis());
        assertThat(UnitTime.minutes(-2).plus(UnitTime.seconds(31))).isEqualTo(
                UnitTime.millis(-89000));
        assertThat(UnitTime.seconds(1).plus(UnitTime.millis(-700))).isEqualTo(UnitTime.millis(300));
    }

    @Test
    public void testSecondConversions() throws InvocationTargetException, IllegalAccessException {

        testConversions(UnitTime.seconds(clip(sRandom.nextInt())), true);
        testConversions(UnitTime.fromUnit(clip(sRandom.nextInt()), TimeUnit.SECONDS), true);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSecondError() {

        try {

            UnitTime.seconds(1).to(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testUnitError() {

        try {

            UnitTime.fromUnit(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testZeroDay() {

        assertThat(UnitTime.days(0).isZero()).isTrue();

        final TimeUnit days = getUnit("DAYS");
        if (days != null) {

            assertThat(UnitTime.fromUnit(0, days).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroHour() {

        assertThat(UnitTime.hours(0).isZero()).isTrue();

        final TimeUnit hours = getUnit("HOURS");
        if (hours != null) {

            assertThat(UnitTime.fromUnit(0, hours).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroMicro() {

        assertThat(UnitTime.micros(0).isZero()).isTrue();
        assertThat(UnitTime.fromUnit(0, TimeUnit.MICROSECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroMilli() {

        assertThat(UnitTime.millis(0).isZero()).isTrue();
        assertThat(UnitTime.fromUnit(0, TimeUnit.MILLISECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroMinute() {

        assertThat(UnitTime.minutes(0).isZero()).isTrue();

        final TimeUnit minutes = getUnit("MINUTES");
        if (minutes != null) {

            assertThat(UnitTime.fromUnit(0, minutes).isZero()).isTrue();
        }
    }

    @Test
    public void testZeroNano() {

        assertThat(UnitTime.nanos(0).isZero()).isTrue();
        assertThat(UnitTime.fromUnit(0, TimeUnit.NANOSECONDS).isZero()).isTrue();
    }

    @Test
    public void testZeroSecond() {

        assertThat(UnitTime.seconds(0).isZero()).isTrue();
        assertThat(UnitTime.fromUnit(0, TimeUnit.SECONDS).isZero()).isTrue();
    }

    private void testConversions(final UnitTime time, final boolean isFirst) throws
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
        assertThat(time).isEqualTo(UnitTime.fromUnit(time.value, time.unit));
        assertThat(time.nanosTime()).isEqualTo(
                UnitTime.fromUnit(time.nanosTime().to(time.unit), time.unit));
        assertThat(time.microsTime()).isEqualTo(
                UnitTime.fromUnit(time.microsTime().to(time.unit), time.unit));
        assertThat(time.millisTime()).isEqualTo(
                UnitTime.fromUnit(time.millisTime().to(time.unit), time.unit));
        assertThat(time.secondsTime()).isEqualTo(
                UnitTime.fromUnit(time.secondsTime().to(time.unit), time.unit));
        assertThat(time.minutesTime()).isEqualTo(
                UnitTime.fromUnit(time.minutesTime().to(time.unit), time.unit));
        assertThat(time.hoursTime()).isEqualTo(
                UnitTime.fromUnit(time.hoursTime().to(time.unit), time.unit));
        assertThat(time.daysTime()).isEqualTo(
                UnitTime.fromUnit(time.daysTime().to(time.unit), time.unit));

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
