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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling time in different time units.
 * <p>
 * Created by davide-maestroni on 09/09/2014.
 */
@SuppressWarnings("WeakerAccess")
public class TimeMeasure implements Comparable<TimeMeasure> {

  /**
   * The number of hours in a day.
   */
  public static final long HOURS_IN_DAY = 24;

  /**
   * The number of minutes in an hour.
   */
  public static final long MINUTES_IN_HOUR = 60;

  /**
   * The number of minutes in a day.
   */
  public static final long MINUTES_IN_DAY = MINUTES_IN_HOUR * HOURS_IN_DAY;

  /**
   * The number of seconds in a minute.
   */
  public static final long SECONDS_IN_MINUTE = 60;

  /**
   * The number of seconds in an hour.
   */
  public static final long SECONDS_IN_HOUR = SECONDS_IN_MINUTE * MINUTES_IN_HOUR;

  /**
   * The number of seconds in a day.
   */
  public static final long SECONDS_IN_DAY = SECONDS_IN_HOUR * HOURS_IN_DAY;

  /**
   * The maximum number of days expressible in number of seconds.
   */
  protected static final long MAX_DAYS = Long.MAX_VALUE / SECONDS_IN_DAY;

  /**
   * The maximum number of hours expressible in number of seconds.
   */
  protected static final long MAX_HOURS = Long.MAX_VALUE / SECONDS_IN_HOUR;

  /**
   * The maximum number of minutes expressible in number of seconds.
   */
  protected static final long MAX_MINUTES = Long.MAX_VALUE / SECONDS_IN_MINUTE;

  private static final int CACHE_TIME_HIGH = 63;

  private static final int CACHE_TIME_LOW = -64;

  private static final int CACHE_TIME_SIZE = CACHE_TIME_HIGH - CACHE_TIME_LOW + 1;

  private static final int CACHE_UNIT_HIGH = TimeUnit.values().length - 1;

  private static final int CACHE_UNIT_LOW = TimeUnit.SECONDS.ordinal();

  private static final int CACHE_UNIT_SIZE = CACHE_UNIT_HIGH - CACHE_UNIT_LOW + 1;

  private static final TimeMeasure[][] sCaches = new TimeMeasure[CACHE_UNIT_SIZE][CACHE_TIME_SIZE];

  private static final TimeMeasure[] sZeroes = new TimeMeasure[TimeUnit.values().length];

  /**
   * The time unit.
   */
  public final TimeUnit unit;

  /**
   * The time value.
   */
  public final long value;

  /**
   * Constructor.
   *
   * @param value the time value.
   * @param unit  the time unit.
   */
  protected TimeMeasure(final long value, @NotNull final TimeUnit unit) {
    this.value = value;
    this.unit = ConstantConditions.notNull("time unit", unit);
  }

  /**
   * Creates and returns an object representing the current system time in milliseconds.
   *
   * @return the time instance.
   * @see System#currentTimeMillis()
   */
  @NotNull
  public static TimeMeasure current() {
    return millis(System.currentTimeMillis());
  }

  /**
   * Creates and returns an object representing the current high precision system time in
   * nanoseconds.
   *
   * @return the time instance.
   * @see System#nanoTime()
   */
  @NotNull
  public static TimeMeasure currentNano() {
    return nanos(System.nanoTime());
  }

  /**
   * Creates and returns an object representing the specified number of days.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param days the number of days.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure days(final double days) {
    if ((days > MAX_DAYS) || (days < -MAX_DAYS)) {
      throw new IllegalArgumentException("time value overflow: " + days + " days");
    }

    final double seconds = days * SECONDS_IN_DAY;
    return fromUnit(seconds - (seconds % SECONDS_IN_HOUR), TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of days.
   *
   * @param days the number of days.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure days(final long days) {
    if ((days > MAX_DAYS) || (days < -MAX_DAYS)) {
      throw new IllegalArgumentException("time value overflow: " + days + " days");
    }

    return fromUnit(days * SECONDS_IN_DAY, TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified time value in the specified time unit.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param time the time value.
   * @param unit the time unit.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure fromUnit(final double time, @NotNull final TimeUnit unit) {
    final int ordinal = unit.ordinal();
    if ((ordinal == 0) || (Math.rint(time) == time)) {
      return fromUnit(Math.round(time), unit);
    }

    final TimeUnit toUnit = TimeUnit.values()[ordinal - 1];
    return fromUnit(Math.round(toUnit.convert(1, unit) * time), toUnit);
  }

  /**
   * Creates and returns an object representing the specified time value in the specified time unit.
   *
   * @param time the time value.
   * @param unit the time unit.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure fromUnit(final long time, @NotNull final TimeUnit unit) {
    final int ordinal = unit.ordinal();
    if (time == 0) {
      return sZeroes[ordinal];
    }

    if ((ordinal >= CACHE_UNIT_LOW) && (ordinal <= CACHE_UNIT_HIGH) && (time >= CACHE_TIME_LOW) &&
        (time <= CACHE_TIME_HIGH)) {
      return sCaches[ordinal - CACHE_UNIT_LOW][(int) time - CACHE_TIME_LOW];
    }

    return new TimeMeasure(time, ConstantConditions.notNull("time unit", unit));
  }

  /**
   * Creates and returns an object representing the specified number of hours.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param hours the number of hours
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure hours(final double hours) {
    if ((hours > MAX_HOURS) || (hours < -MAX_HOURS)) {
      throw new IllegalArgumentException("time value overflow: " + hours + " hours");
    }

    final double seconds = hours * SECONDS_IN_HOUR;
    return fromUnit(seconds - (seconds % SECONDS_IN_MINUTE), TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of hours.
   *
   * @param hours the number of hours
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure hours(final long hours) {
    if ((hours > MAX_HOURS) || (hours < -MAX_HOURS)) {
      throw new IllegalArgumentException("time value overflow: " + hours + " hours");
    }

    return fromUnit(hours * SECONDS_IN_HOUR, TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of microseconds.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param micros the number of microseconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure micros(final double micros) {
    return fromUnit(micros, TimeUnit.MICROSECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of microseconds.
   *
   * @param micros the number of microseconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure micros(final long micros) {
    return fromUnit(micros, TimeUnit.MICROSECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of milliseconds.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param millis the number of milliseconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure millis(final double millis) {
    return fromUnit(millis, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of milliseconds.
   *
   * @param millis the number of milliseconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure millis(final long millis) {
    return fromUnit(millis, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of minutes.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param minutes the number of minutes.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure minutes(final double minutes) {
    if ((minutes > MAX_MINUTES) || (minutes < -MAX_MINUTES)) {
      throw new IllegalArgumentException("time value overflow: " + minutes + " minutes");
    }

    final double seconds = minutes * SECONDS_IN_MINUTE;
    return fromUnit(seconds - (seconds % 1), TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of minutes.
   *
   * @param minutes the number of minutes.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure minutes(final long minutes) {
    if ((minutes > MAX_MINUTES) || (minutes < -MAX_MINUTES)) {
      throw new IllegalArgumentException("time value overflow: " + minutes + " minutes");
    }

    return fromUnit(minutes * SECONDS_IN_MINUTE, TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of nanoseconds.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param nanos the number of nanoseconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure nanos(final double nanos) {
    return fromUnit(nanos, TimeUnit.NANOSECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of nanoseconds.
   *
   * @param nanos the number of nanoseconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure nanos(final long nanos) {
    return fromUnit(nanos, TimeUnit.NANOSECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of seconds.
   * <br>
   * The returned time will have at maximum the same precision as the next more granular time unit.
   *
   * @param seconds the number of seconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure seconds(final double seconds) {
    return fromUnit(seconds, TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of seconds.
   *
   * @param seconds the number of seconds.
   * @return the time instance.
   */
  @NotNull
  public static TimeMeasure seconds(final long seconds) {
    return fromUnit(seconds, TimeUnit.SECONDS);
  }

  private static int compareLong(final long l1, final long l2) {
    return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
  }

  public int compareTo(@NotNull final TimeMeasure o) {
    final int unitCompare = unit.compareTo(o.unit);
    if (unitCompare == 0) {
      return compareLong(value, o.value);
    }

    if (unitCompare > 0) {
      final int coarseComparison = compareLong(value, o.to(unit));
      return (coarseComparison != 0) ? coarseComparison : compareLong(to(o.unit), o.value);
    }

    final int coarseComparison = compareLong(to(o.unit), o.value);
    return (coarseComparison != 0) ? coarseComparison : compareLong(value, o.to(unit));
  }

  /**
   * Converts this time in days.
   *
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure daysTime() {
    return days(toDays());
  }

  @Override
  public int hashCode() {
    final long value = toNanos();
    return (int) (value ^ (value >>> 32));
  }

  @Override
  public boolean equals(final Object o) {
    return (this == o) || (o instanceof TimeMeasure) && (compareTo((TimeMeasure) o) == 0);
  }

  @Override
  public String toString() {
    return value + " " + unit.toString();
  }

  /**
   * Converts this time in hours.
   *
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure hoursTime() {
    return hours(toHours());
  }

  /**
   * Checks if this time is greater than or equal to the specified one.
   *
   * @param other the time to compare.
   * @return whether this time is greater or equal.
   */
  public boolean isGreaterOrEquals(@NotNull final TimeMeasure other) {
    return compareTo(other) >= 0;
  }

  /**
   * Checks if this time is greater than the specified one.
   *
   * @param other the time to compare.
   * @return whether this time is greater.
   */
  public boolean isGreaterThan(@NotNull final TimeMeasure other) {
    return compareTo(other) > 0;
  }

  /**
   * Checks if this time is less than or equal to the specified one.
   *
   * @param other the time to compare.
   * @return whether this time is less or equal.
   */
  public boolean isLessOrEquals(@NotNull final TimeMeasure other) {
    return compareTo(other) <= 0;
  }

  /**
   * Checks if this time is less than the specified one.
   *
   * @param other the time to compare.
   * @return whether this time is less.
   */
  public boolean isLessThan(@NotNull final TimeMeasure other) {
    return compareTo(other) < 0;
  }

  /**
   * Checks if this time is noTime.
   *
   * @return whether this time value is noTime.
   */
  public boolean isZero() {
    return (value == 0);
  }

  /**
   * Converts this time in microseconds.
   *
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure microsTime() {
    return micros(toMicros());
  }

  /**
   * Converts this time in milliseconds.
   *
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure millisTime() {
    return millis(toMillis());
  }

  /**
   * Returns a new instance whose time value is decremented by the specified one.
   * <p>
   * Note that the unit of the returned time will match the one with the highest precision.
   *
   * @param time the time to subtract.
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure minus(@NotNull final TimeMeasure time) {
    if (unit.compareTo(time.unit) > 0) {
      return fromUnit(to(time.unit) - time.value, time.unit);
    }

    return fromUnit(this.value - time.to(unit), unit);
  }

  /**
   * Converts this time in minutes.
   *
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure minutesTime() {
    return minutes(toMinutes());
  }

  /**
   * Converts this time in nanoseconds.
   *
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure nanosTime() {
    return nanos(toNanos());
  }

  /**
   * Returns a new instance whose time value is incremented by the specified one.
   * <p>
   * Note that the unit of the returned time will match the one with the highest precision.
   *
   * @param time the time to add.
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure plus(@NotNull final TimeMeasure time) {
    if (unit.compareTo(time.unit) > 0) {
      return fromUnit(to(time.unit) + time.value, time.unit);
    }

    return fromUnit(this.value + time.to(unit), unit);
  }

  /**
   * Converts this time in seconds.
   *
   * @return the time instance.
   */
  @NotNull
  public TimeMeasure secondsTime() {
    return seconds(toSeconds());
  }

  /**
   * Converts this time in the specified unit.
   *
   * @param timeUnit the time unit to convert to.
   * @return the time value in the specified unit.
   */
  public long to(@NotNull final TimeUnit timeUnit) {
    return timeUnit.convert(value, unit);
  }

  /**
   * Converts this time in number of days.
   *
   * @return the number of days.
   */
  public long toDays() {
    return unit.toSeconds(value) / SECONDS_IN_DAY;
  }

  /**
   * Converts this time in number of hours.
   *
   * @return the number of hours.
   */
  public long toHours() {
    return unit.toSeconds(value) / SECONDS_IN_HOUR;
  }

  /**
   * Converts this time in number of microseconds.
   *
   * @return the number of microseconds.
   */
  public long toMicros() {
    return unit.toMicros(value);
  }

  /**
   * Converts this time in number of milliseconds.
   *
   * @return the number of milliseconds.
   */
  public long toMillis() {
    return unit.toMillis(value);
  }

  /**
   * Converts this time in number of minutes.
   *
   * @return the number of minutes.
   */
  public long toMinutes() {
    return unit.toSeconds(value) / SECONDS_IN_MINUTE;
  }

  /**
   * Converts this time in number of nanoseconds.
   *
   * @return the number of nanoseconds.
   */
  public long toNanos() {
    return unit.toNanos(value);
  }

  /**
   * Converts this time in number of seconds.
   *
   * @return the number of seconds.
   */
  public long toSeconds() {
    return unit.toSeconds(value);
  }

  static {
    final TimeMeasure[] zeroes = sZeroes;
    final TimeUnit[] timeUnits = TimeUnit.values();
    for (int i = 0; i < timeUnits.length; ++i) {
      zeroes[i] = new TimeMeasure(0, timeUnits[i]);
    }

    final TimeMeasure[][] caches = sCaches;
    for (int i = 0; i < CACHE_UNIT_SIZE; ++i) {
      final TimeUnit timeUnit = timeUnits[CACHE_UNIT_LOW + i];
      final TimeMeasure[] cache = caches[i];
      for (int j = 0; j < CACHE_TIME_SIZE; ++j) {
        cache[j] = new TimeMeasure(CACHE_TIME_LOW + j, timeUnit);
      }
    }
  }
}
