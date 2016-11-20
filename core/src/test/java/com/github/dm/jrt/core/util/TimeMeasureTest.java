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
public class TimeMeasureTest {

  private static final long ONE_DAY_NANOS = TimeMeasure.days(1).toNanos();

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

    assertThat(TimeMeasure.millis(0).compareTo(TimeMeasure.seconds(0))).isEqualTo(0);
    assertThat(TimeMeasure.millis(0).compareTo(TimeMeasure.seconds(-1))).isEqualTo(1);
    assertThat(TimeMeasure.millis(-1).compareTo(TimeMeasure.seconds(0))).isEqualTo(-1);
    assertThat(TimeMeasure.days(0).compareTo(TimeMeasure.seconds(0))).isEqualTo(0);
    assertThat(TimeMeasure.days(0).compareTo(TimeMeasure.seconds(-1))).isEqualTo(1);
    assertThat(TimeMeasure.days(-1).compareTo(TimeMeasure.seconds(0))).isEqualTo(-1);
    assertThat(TimeMeasure.millis(0).isGreaterThan(TimeMeasure.seconds(0))).isFalse();
    assertThat(TimeMeasure.millis(0).isGreaterOrEquals(TimeMeasure.seconds(0))).isTrue();
    assertThat(TimeMeasure.millis(0).isLessThan(TimeMeasure.seconds(0))).isFalse();
    assertThat(TimeMeasure.millis(0).isLessOrEquals(TimeMeasure.seconds(0))).isTrue();
    assertThat(TimeMeasure.millis(0).isGreaterThan(TimeMeasure.seconds(-1))).isTrue();
    assertThat(TimeMeasure.millis(0).isGreaterOrEquals(TimeMeasure.seconds(-1))).isTrue();
    assertThat(TimeMeasure.millis(0).isLessThan(TimeMeasure.seconds(-1))).isFalse();
    assertThat(TimeMeasure.millis(0).isLessOrEquals(TimeMeasure.seconds(-1))).isFalse();
    assertThat(TimeMeasure.millis(-1).isGreaterThan(TimeMeasure.seconds(0))).isFalse();
    assertThat(TimeMeasure.millis(-1).isGreaterOrEquals(TimeMeasure.seconds(0))).isFalse();
    assertThat(TimeMeasure.millis(-1).isLessThan(TimeMeasure.seconds(0))).isTrue();
    assertThat(TimeMeasure.millis(-1).isLessOrEquals(TimeMeasure.seconds(0))).isTrue();
  }

  @Test
  public void testConstants() {

    assertThat(TimeMeasure.SECONDS_IN_MINUTE).isEqualTo(60);
    assertThat(TimeMeasure.SECONDS_IN_HOUR).isEqualTo(60 * 60);
    assertThat(TimeMeasure.SECONDS_IN_DAY).isEqualTo(60 * 60 * 24);
    assertThat(TimeMeasure.MINUTES_IN_HOUR).isEqualTo(60);
    assertThat(TimeMeasure.MINUTES_IN_DAY).isEqualTo(60 * 24);
    assertThat(TimeMeasure.HOURS_IN_DAY).isEqualTo(24);
  }

  @Test
  public void testCurrentTime() {

    final long systemTimeMs = System.currentTimeMillis();
    assertThat(TimeMeasure.current().toMillis()).isBetween(systemTimeMs - 50, systemTimeMs + 50);
    final long systemTimeNs = System.nanoTime();
    assertThat(TimeMeasure.currentNano().toNanos()).isBetween(systemTimeNs - 50000000,
        systemTimeNs + 50000000);
  }

  @Test
  public void testDayConversions() throws InvocationTargetException, IllegalAccessException {

    testConversions(TimeMeasure.days(clip(sRandom.nextInt())), true);
    final TimeUnit days = getUnit("DAYS");
    if (days != null) {
      testConversions(TimeMeasure.fromUnit(clip(sRandom.nextInt()), days), true);
    }
  }

  @Test
  public void testDayOverflowError() {

    try {
      TimeMeasure.days(Long.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.days(Long.MIN_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.days(Double.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.days(-Double.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testEqualConversions() throws InvocationTargetException, IllegalAccessException {

    final TimeMeasure time = TimeMeasure.nanos(clip(sRandom.nextInt()));
    assertThat(time).isEqualTo(time);
    assertThat(time).isEqualTo(time.nanosTime());
    assertThat(time).isNotEqualTo(time.millisTime());
    assertThat(time.equals(new Object())).isFalse();
    assertThat(time.hashCode()).isEqualTo(time.nanosTime().hashCode());
  }

  @Test
  public void testFloatTime() {

    assertThat(TimeMeasure.days(1.6)).isEqualTo(TimeMeasure.hours(38));
    assertThat(TimeMeasure.hours(1.6)).isEqualTo(TimeMeasure.minutes(96));
    assertThat(TimeMeasure.minutes(1.6)).isEqualTo(TimeMeasure.seconds(96));
    assertThat(TimeMeasure.seconds(1.6)).isEqualTo(TimeMeasure.millis(1600));
    assertThat(TimeMeasure.millis(1.6)).isEqualTo(TimeMeasure.micros(1600));
    assertThat(TimeMeasure.micros(1.6)).isEqualTo(TimeMeasure.nanos(1600));
    assertThat(TimeMeasure.nanos(1.6)).isEqualTo(TimeMeasure.nanos(2));
    assertThat(TimeMeasure.days(-1.6)).isEqualTo(TimeMeasure.hours(-38));
    assertThat(TimeMeasure.hours(-1.6)).isEqualTo(TimeMeasure.minutes(-96));
    assertThat(TimeMeasure.minutes(-1.6)).isEqualTo(TimeMeasure.seconds(-96));
    assertThat(TimeMeasure.seconds(-1.6)).isEqualTo(TimeMeasure.millis(-1600));
    assertThat(TimeMeasure.millis(-1.6)).isEqualTo(TimeMeasure.micros(-1600));
    assertThat(TimeMeasure.micros(-1.6)).isEqualTo(TimeMeasure.nanos(-1600));
    assertThat(TimeMeasure.nanos(-1.6)).isEqualTo(TimeMeasure.nanos(-2));

    assertThat(TimeMeasure.days(3.0)).isEqualTo(TimeMeasure.days(3));
    assertThat(TimeMeasure.hours(3.0)).isEqualTo(TimeMeasure.hours(3));
    assertThat(TimeMeasure.minutes(3.0)).isEqualTo(TimeMeasure.minutes(3));
    assertThat(TimeMeasure.seconds(3.0)).isEqualTo(TimeMeasure.seconds(3));
    assertThat(TimeMeasure.millis(3.0)).isEqualTo(TimeMeasure.millis(3));
    assertThat(TimeMeasure.micros(3.0)).isEqualTo(TimeMeasure.micros(3));
    assertThat(TimeMeasure.nanos(3.0)).isEqualTo(TimeMeasure.nanos(3));
    assertThat(TimeMeasure.days(-3.0)).isEqualTo(TimeMeasure.days(-3));
    assertThat(TimeMeasure.hours(-3.0)).isEqualTo(TimeMeasure.hours(-3));
    assertThat(TimeMeasure.minutes(-3.0)).isEqualTo(TimeMeasure.minutes(-3));
    assertThat(TimeMeasure.seconds(-3.0)).isEqualTo(TimeMeasure.seconds(-3));
    assertThat(TimeMeasure.millis(-3.0)).isEqualTo(TimeMeasure.millis(-3));
    assertThat(TimeMeasure.micros(-3.0)).isEqualTo(TimeMeasure.micros(-3));
    assertThat(TimeMeasure.nanos(-3.0)).isEqualTo(TimeMeasure.nanos(-3));
  }

  @Test
  public void testHourConversions() throws InvocationTargetException, IllegalAccessException {

    testConversions(TimeMeasure.hours(clip(sRandom.nextInt())), true);
    final TimeUnit hours = getUnit("HOURS");
    if (hours != null) {
      testConversions(TimeMeasure.fromUnit(clip(sRandom.nextInt()), hours), true);
    }
  }

  @Test
  public void testHourOverflowError() {

    try {
      TimeMeasure.hours(Long.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.hours(Long.MIN_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.hours(Double.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.hours(-Double.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testMicroConversions() throws InvocationTargetException, IllegalAccessException {

    testConversions(TimeMeasure.micros(clip(sRandom.nextInt())), true);
    testConversions(TimeMeasure.fromUnit(clip(sRandom.nextInt()), TimeUnit.MICROSECONDS), true);
  }

  @Test
  public void testMilliConversions() throws InvocationTargetException, IllegalAccessException {

    testConversions(TimeMeasure.millis(clip(sRandom.nextInt())), true);
    testConversions(TimeMeasure.fromUnit(clip(sRandom.nextInt()), TimeUnit.MILLISECONDS), true);
  }

  @Test
  public void testMinus() {

    final TimeMeasure current = TimeMeasure.current();
    assertThat(current.minus(TimeMeasure.hours(2).minus(TimeMeasure.minutes(11)))).isNotEqualTo(
        current.minus(TimeMeasure.hours(2)).minus(TimeMeasure.minutes(11)));
    assertThat(
        current.minus(TimeMeasure.hours(2).minus(TimeMeasure.minutes(11))).toMillis()).isEqualTo(
        current.toMillis() - TimeMeasure.hours(2).toMillis() + TimeMeasure.minutes(11).toMillis());
    assertThat(
        current.minus(TimeMeasure.hours(2)).minus(TimeMeasure.minutes(11)).toMillis()).isEqualTo(
        current.toMillis() - TimeMeasure.hours(2).toMillis() - TimeMeasure.minutes(11).toMillis());
    assertThat(TimeMeasure.minutes(-2).minus(TimeMeasure.seconds(31))).isEqualTo(
        TimeMeasure.millis(-151000));
    assertThat(TimeMeasure.seconds(1).minus(TimeMeasure.millis(700))).isEqualTo(
        TimeMeasure.millis(300));
  }

  @Test
  public void testMinuteConversions() throws InvocationTargetException, IllegalAccessException {

    testConversions(TimeMeasure.minutes(clip(sRandom.nextInt())), true);
    final TimeUnit minutes = getUnit("MINUTES");
    if (minutes != null) {
      testConversions(TimeMeasure.fromUnit(clip(sRandom.nextInt()), minutes), true);
    }
  }

  @Test
  public void testMinuteOverflowError() {

    try {
      TimeMeasure.minutes(Long.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.minutes(Long.MIN_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.minutes(Double.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      TimeMeasure.minutes(-Double.MAX_VALUE);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testNanoConversions() throws InvocationTargetException, IllegalAccessException {

    testConversions(TimeMeasure.nanos(clip(sRandom.nextInt())), true);
    testConversions(TimeMeasure.fromUnit(clip(sRandom.nextInt()), TimeUnit.NANOSECONDS), true);
  }

  @Test
  public void testPlus() {

    final TimeMeasure current = TimeMeasure.current();
    assertThat(current.plus(TimeMeasure.hours(2).plus(TimeMeasure.minutes(11)))).isEqualTo(
        current.plus(TimeMeasure.hours(2)).plus(TimeMeasure.minutes(11)));
    assertThat(
        current.plus(TimeMeasure.hours(2)).plus(TimeMeasure.minutes(11)).toMillis()).isEqualTo(
        current.toMillis() + TimeMeasure.hours(2).toMillis() + TimeMeasure.minutes(11).toMillis());
    assertThat(TimeMeasure.minutes(-2).plus(TimeMeasure.seconds(31))).isEqualTo(
        TimeMeasure.millis(-89000));
    assertThat(TimeMeasure.seconds(1).plus(TimeMeasure.millis(-700))).isEqualTo(
        TimeMeasure.millis(300));
  }

  @Test
  public void testSecondConversions() throws InvocationTargetException, IllegalAccessException {

    testConversions(TimeMeasure.seconds(clip(sRandom.nextInt())), true);
    testConversions(TimeMeasure.fromUnit(clip(sRandom.nextInt()), TimeUnit.SECONDS), true);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testSecondError() {

    try {
      TimeMeasure.seconds(1).to(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testUnitError() {

    try {
      TimeMeasure.fromUnit(0, null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testZeroDay() {

    assertThat(TimeMeasure.days(0).isZero()).isTrue();
    final TimeUnit days = getUnit("DAYS");
    if (days != null) {
      assertThat(TimeMeasure.fromUnit(0, days).isZero()).isTrue();
    }
  }

  @Test
  public void testZeroHour() {

    assertThat(TimeMeasure.hours(0).isZero()).isTrue();
    final TimeUnit hours = getUnit("HOURS");
    if (hours != null) {
      assertThat(TimeMeasure.fromUnit(0, hours).isZero()).isTrue();
    }
  }

  @Test
  public void testZeroMicro() {

    assertThat(TimeMeasure.micros(0).isZero()).isTrue();
    assertThat(TimeMeasure.fromUnit(0, TimeUnit.MICROSECONDS).isZero()).isTrue();
  }

  @Test
  public void testZeroMilli() {

    assertThat(TimeMeasure.millis(0).isZero()).isTrue();
    assertThat(TimeMeasure.fromUnit(0, TimeUnit.MILLISECONDS).isZero()).isTrue();
  }

  @Test
  public void testZeroMinute() {

    assertThat(TimeMeasure.minutes(0).isZero()).isTrue();
    final TimeUnit minutes = getUnit("MINUTES");
    if (minutes != null) {
      assertThat(TimeMeasure.fromUnit(0, minutes).isZero()).isTrue();
    }
  }

  @Test
  public void testZeroNano() {

    assertThat(TimeMeasure.nanos(0).isZero()).isTrue();
    assertThat(TimeMeasure.fromUnit(0, TimeUnit.NANOSECONDS).isZero()).isTrue();
  }

  @Test
  public void testZeroSecond() {

    assertThat(TimeMeasure.seconds(0).isZero()).isTrue();
    assertThat(TimeMeasure.fromUnit(0, TimeUnit.SECONDS).isZero()).isTrue();
  }

  private void testConversions(final TimeMeasure time, final boolean isFirst) throws
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

    assertThat(time.to(TimeUnit.NANOSECONDS)).isEqualTo(TimeUnit.NANOSECONDS.convert(value, unit));
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
    assertThat(time).isEqualTo(TimeMeasure.fromUnit(time.value, time.unit));
    assertThat(time.nanosTime()).isEqualTo(
        TimeMeasure.fromUnit(time.nanosTime().to(time.unit), time.unit));
    assertThat(time.microsTime()).isEqualTo(
        TimeMeasure.fromUnit(time.microsTime().to(time.unit), time.unit));
    assertThat(time.millisTime()).isEqualTo(
        TimeMeasure.fromUnit(time.millisTime().to(time.unit), time.unit));
    assertThat(time.secondsTime()).isEqualTo(
        TimeMeasure.fromUnit(time.secondsTime().to(time.unit), time.unit));
    assertThat(time.minutesTime()).isEqualTo(
        TimeMeasure.fromUnit(time.minutesTime().to(time.unit), time.unit));
    assertThat(time.hoursTime()).isEqualTo(
        TimeMeasure.fromUnit(time.hoursTime().to(time.unit), time.unit));
    assertThat(time.daysTime()).isEqualTo(
        TimeMeasure.fromUnit(time.daysTime().to(time.unit), time.unit));
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
