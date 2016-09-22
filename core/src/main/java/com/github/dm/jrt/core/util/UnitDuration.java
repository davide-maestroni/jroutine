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

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling a time duration in different time units.
 * <p>
 * Created by davide-maestroni on 09/09/2014.
 */
public class UnitDuration extends UnitTime {

  private static final long ONE_MILLI_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final UnitDuration sInfinity = seconds(Long.MAX_VALUE);

  private static final UnitDuration sZero = millis(0);

  private static final HashMap<TimeUnit, UnitDuration> sZeroes =
      new HashMap<TimeUnit, UnitDuration>() {{
        final UnitDuration zero = sZero;
        put(zero.unit, zero);
      }};

  /**
   * Constructor.
   *
   * @param duration the time value.
   * @param unit     the time unit.
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  protected UnitDuration(final long duration, @NotNull final TimeUnit unit) {
    super(ConstantConditions.notNegative("time duration", duration), unit);
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
  public static UnitDuration days(final double days) {
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
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration days(final long days) {
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
  public static UnitDuration fromUnit(final double time, @NotNull final TimeUnit unit) {
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
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration fromUnit(final long time, @NotNull final TimeUnit unit) {
    return new UnitDuration(time, unit);
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
  public static UnitDuration hours(final double hours) {
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
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration hours(final long hours) {
    if ((hours > MAX_HOURS) || (hours < -MAX_HOURS)) {
      throw new IllegalArgumentException("time value overflow: " + hours + " hours");
    }

    return fromUnit(hours * SECONDS_IN_HOUR, TimeUnit.SECONDS);
  }

  /**
   * Returns the time duration instance representing the infinity.
   *
   * @return the infinity instance.
   */
  @NotNull
  public static UnitDuration infinity() {
    return sInfinity;
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
  public static UnitDuration micros(final double micros) {
    return fromUnit(micros, TimeUnit.MICROSECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of microseconds.
   *
   * @param micros the number of microseconds.
   * @return the time duration instance.
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration micros(final long micros) {
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
  public static UnitDuration millis(final double millis) {
    return fromUnit(millis, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of milliseconds.
   *
   * @param millis the number of milliseconds.
   * @return the time duration instance.
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration millis(final long millis) {
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
  public static UnitDuration minutes(final double minutes) {
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
   * @return the time duration instance.
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration minutes(final long minutes) {
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
  public static UnitDuration nanos(final double nanos) {
    return fromUnit(nanos, TimeUnit.NANOSECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of nanoseconds.
   *
   * @param nanos the number of nanoseconds.
   * @return the time duration instance.
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration nanos(final long nanos) {
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
  public static UnitDuration seconds(final double seconds) {
    return fromUnit(seconds, TimeUnit.SECONDS);
  }

  /**
   * Creates and returns an object representing the specified number of seconds.
   *
   * @param seconds the number of seconds.
   * @return the time duration instance.
   * @throws java.lang.IllegalArgumentException if the specified duration is negative.
   */
  @NotNull
  public static UnitDuration seconds(final long seconds) {
    return fromUnit(seconds, TimeUnit.SECONDS);
  }

  /**
   * Performs a {@link java.lang.Thread#sleep(long, int)} using the specified duration as timeout,
   * ensuring that the sleep time is respected even if spurious wake-ups happen in the while.
   *
   * @param time the time value.
   * @param unit the time unit.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public static void sleepAtLeast(final long time, @NotNull final TimeUnit unit) throws
      InterruptedException {
    if (time == 0) {
      return;
    }

    if ((unit.compareTo(TimeUnit.MILLISECONDS) >= 0) || ((unit.toNanos(time) % ONE_MILLI_NANOS)
        == 0)) {
      final long startMillis = System.currentTimeMillis();
      while (true) {
        if (!sleepSinceMillis(time, unit, startMillis)) {
          return;
        }
      }
    }

    final long startNanos = System.nanoTime();
    while (true) {
      if (!sleepSinceNanos(time, unit, startNanos)) {
        return;
      }
    }
  }

  /**
   * Performs a {@link java.lang.Thread#sleep(long, int)} as if started from the specified system
   * time in milliseconds, by using the specified time as timeout.
   *
   * @param time      the time value.
   * @param unit      the time unit.
   * @param milliTime the starting system time in milliseconds.
   * @return whether the sleep happened at all.
   * @throws java.lang.IllegalStateException if this duration overflows the maximum sleep time.
   * @throws java.lang.InterruptedException  if the current thread is interrupted.
   * @see System#currentTimeMillis()
   */
  public static boolean sleepSinceMillis(final long time, @NotNull final TimeUnit unit,
      final long milliTime) throws InterruptedException {
    if (time == 0) {
      return false;
    }

    final long millisToSleep = milliTime - System.currentTimeMillis() + unit.toMillis(time);
    if (millisToSleep <= 0) {
      return false;
    }

    TimeUnit.MILLISECONDS.sleep(millisToSleep);
    return true;
  }

  /**
   * Performs a {@link java.lang.Thread#sleep(long, int)} as if started from the specified high
   * precision system time in nanoseconds, by using the specified time as timeout.
   *
   * @param time     the time value.
   * @param unit     the time unit.
   * @param nanoTime the starting system time in nanoseconds.
   * @return whether the sleep happened at all.
   * @throws java.lang.IllegalStateException if this duration overflows the maximum sleep time.
   * @throws java.lang.InterruptedException  if the current thread is interrupted.
   * @see System#nanoTime()
   */
  public static boolean sleepSinceNanos(final long time, @NotNull final TimeUnit unit,
      final long nanoTime) throws InterruptedException {
    if (time == 0) {
      return false;
    }

    final long nanosToSleep = nanoTime - System.nanoTime() + unit.toNanos(time);
    if (nanosToSleep <= 0) {
      return false;
    }

    TimeUnit.NANOSECONDS.sleep(nanosToSleep);
    return true;
  }

  /**
   * Creates and returns an object representing the time range between now and a time in the past.
   * <br>
   * If the specified is in the future, a {@code zero()} duration will be returned.
   *
   * @param milliTime the system time in milliseconds.
   * @return the time duration instance.
   * @see System#currentTimeMillis()
   */
  @NotNull
  public static UnitDuration timeSinceMillis(final long milliTime) {
    return millis(Math.max(0, System.currentTimeMillis() - milliTime));
  }

  /**
   * Creates and returns an object representing the time range between now and a time in the past.
   * <br>
   * If the specified is in the future, a {@code zero()} duration will be returned.
   *
   * @param nanoTime the high precision system time in nanoseconds.
   * @return the time duration instance.
   * @see System#nanoTime()
   */
  @NotNull
  public static UnitDuration timeSinceNanos(final long nanoTime) {
    return nanos(Math.max(0, System.nanoTime() - nanoTime));
  }

  /**
   * Creates and returns an object representing the time range between now and a time in the future.
   * <br>
   * If the specified is in the past, a {@code zero()} duration will be returned.
   *
   * @param milliTime the system time in milliseconds.
   * @return the time duration instance.
   * @see System#currentTimeMillis()
   */
  @NotNull
  public static UnitDuration timeUntilMillis(final long milliTime) {
    return millis(Math.max(0, milliTime - System.currentTimeMillis()));
  }

  /**
   * Creates and returns an object representing the time range between now and a time in the future.
   * <br>
   * If the specified is in the past, a {@code zero()} duration will be returned.
   *
   * @param nanoTime the high precision system time in nanoseconds.
   * @return the time duration instance.
   * @see System#nanoTime()
   */
  @NotNull
  public static UnitDuration timeUntilNanos(final long nanoTime) {
    return nanos(Math.max(0, nanoTime - System.nanoTime()));
  }

  /**
   * Performs an {@link java.lang.Object#wait()} using the specified time.
   * <br>
   * If the specified time is negative, the method will wait indefinitely.
   *
   * @param time   the time value.
   * @param unit   the time unit.
   * @param target the target object.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public static void wait(final long time, @NotNull final TimeUnit unit,
      @NotNull final Object target) throws InterruptedException {
    if (time == 0) {
      return;
    }

    if (time < 0) {
      target.wait();
      return;
    }

    unit.timedWait(target, time);
  }

  /**
   * Performs an {@link java.lang.Object#wait()} as if started from the specified system time in
   * milliseconds, by using the specified time.
   * <br>
   * If the specified time is negative, the method will wait indefinitely.
   *
   * @param time      the time value.
   * @param unit      the time unit.
   * @param target    the target object.
   * @param milliTime the starting system time in milliseconds.
   * @return whether the wait happened at all.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   * @see System#currentTimeMillis()
   */
  public static boolean waitSinceMillis(final long time, @NotNull final TimeUnit unit,
      @NotNull final Object target, final long milliTime) throws InterruptedException {
    if (time == 0) {
      return false;
    }

    if (time < 0) {
      target.wait();
      return true;
    }

    final long millisToWait = milliTime - System.currentTimeMillis() + unit.toMillis(time);
    if (millisToWait <= 0) {
      return false;
    }

    TimeUnit.MILLISECONDS.timedWait(target, millisToWait);
    return true;
  }

  /**
   * Performs an {@link java.lang.Object#wait()} as if started from the specified high precision
   * system time in nanoseconds, by using the specified time.
   * <br>
   * If the specified time is negative, the method will wait indefinitely.
   *
   * @param time     the time value.
   * @param unit     the time unit.
   * @param target   the target object.
   * @param nanoTime the starting system time in nanoseconds.
   * @return whether the wait happened at all.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   * @see System#nanoTime()
   */
  public static boolean waitSinceNanos(final long time, @NotNull final TimeUnit unit,
      @NotNull final Object target, final long nanoTime) throws InterruptedException {
    if (time == 0) {
      return false;
    }

    if (time < 0) {
      target.wait();
      return true;
    }

    final long nanosToWait = nanoTime - System.nanoTime() + unit.toNanos(time);
    if (nanosToWait <= 0) {
      return false;
    }

    TimeUnit.NANOSECONDS.timedWait(target, nanosToWait);
    return true;
  }

  /**
   * Waits for the specified condition to be true by performing an {@link java.lang.Object#wait()}
   * and using the specified time.
   * <br>
   * If the specified time is negative, the method will wait indefinitely.
   *
   * @param time      the time value.
   * @param unit      the time unit.
   * @param target    the target object.
   * @param condition the condition to verify.
   * @return whether the check became true before the timeout elapsed.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public static boolean waitTrue(final long time, @NotNull final TimeUnit unit,
      @NotNull final Object target, @NotNull final Condition condition) throws
      InterruptedException {
    if (time == 0) {
      return condition.isTrue();
    }

    if (time < 0) {
      while (!condition.isTrue()) {
        target.wait();
      }

      return true;
    }

    if ((unit.toNanos(time) % ONE_MILLI_NANOS) == 0) {
      final long startMillis = System.currentTimeMillis();
      while (!condition.isTrue()) {
        if (!waitSinceMillis(time, unit, target, startMillis)) {
          return false;
        }
      }

    } else {
      final long startNanos = System.nanoTime();
      while (!condition.isTrue()) {
        if (!waitSinceNanos(time, unit, target, startNanos)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Returns the time duration instance representing the zero.
   *
   * @return the zero duration.
   */
  @NotNull
  public static UnitDuration zero() {
    return sZero;
  }

  /**
   * Returns the time duration instance representing the zero in the specified unit.
   *
   * @param unit the time unit.
   * @return the zero duration.
   */
  @NotNull
  public static UnitDuration zero(@NotNull final TimeUnit unit) {
    final HashMap<TimeUnit, UnitDuration> zeroes = sZeroes;
    UnitDuration zero = zeroes.get(unit);
    if (zero == null) {
      zero = new UnitDuration(0, unit);
      zeroes.put(unit, zero);
    }

    return zero;
  }

  /**
   * Converts this duration in days.
   *
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration daysTime() {
    return days(toDays());
  }

  /**
   * Converts this duration in hours.
   *
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration hoursTime() {
    return hours(toHours());
  }

  /**
   * Converts this duration in microseconds.
   *
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration microsTime() {
    return micros(toMicros());
  }

  /**
   * Converts this duration in milliseconds.
   *
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration millisTime() {
    return millis(toMillis());
  }

  /**
   * Returns a new instance whose time value is decremented by the specified one.
   * <p>
   * Note that the unit of the returned time will match the one with the highest precision.
   *
   * @param time the time to subtract.
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration minus(@NotNull final UnitTime time) {
    if (unit.compareTo(time.unit) > 0) {
      final long newTime = to(time.unit) - time.value;
      return (newTime >= 0) ? fromUnit(newTime, time.unit)
          : fromUnit(newTime - Long.MIN_VALUE, time.unit);
    }

    final long newTime = this.value - time.to(unit);
    return (newTime >= 0) ? fromUnit(newTime, unit) : fromUnit(newTime - Long.MIN_VALUE, unit);
  }

  /**
   * Converts this duration in minutes.
   *
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration minutesTime() {
    return minutes(toMinutes());
  }

  /**
   * Converts this duration in nanoseconds.
   *
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration nanosTime() {
    return nanos(toNanos());
  }

  /**
   * Returns a new instance whose time value is incremented by the specified one.
   * <p>
   * Note that the unit of the returned time will match the one with the highest precision.
   *
   * @param time the time to add.
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration plus(@NotNull final UnitTime time) {
    if (unit.compareTo(time.unit) > 0) {
      final long newTime = to(time.unit) + time.value;
      return (newTime >= 0) ? fromUnit(newTime, time.unit)
          : fromUnit(newTime - Long.MIN_VALUE, time.unit);
    }

    final long newTime = this.value + time.to(unit);
    return (newTime >= 0) ? fromUnit(newTime, unit) : fromUnit(newTime - Long.MIN_VALUE, unit);
  }

  /**
   * Converts this duration in seconds.
   *
   * @return the time duration instance.
   */
  @NotNull
  @Override
  public UnitDuration secondsTime() {
    return seconds(toSeconds());
  }

  /**
   * Checks if this duration represents the infinity.
   *
   * @return whether this duration is infinite.
   */
  public boolean isInfinite() {
    return (this == sInfinity);
  }

  /**
   * Performs a {@link java.lang.Thread#join()} using this duration as timeout.
   *
   * @param target the target thread.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public void join(@NotNull final Thread target) throws InterruptedException {
    unit.timedJoin(target, value);
  }

  /**
   * Performs a {@link java.lang.Thread#sleep(long, int)} using this duration as timeout.
   *
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public void sleep() throws InterruptedException {
    unit.sleep(value);
  }

  /**
   * Performs a {@link java.lang.Thread#sleep(long, int)} using this duration as timeout, ensuring
   * that the sleep time is respected even if spurious wake-ups happen in the while.
   *
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public void sleepAtLeast() throws InterruptedException {
    sleepAtLeast(value, unit);
  }

  /**
   * Performs a {@link java.lang.Thread#sleep(long, int)} as if started from the specified system
   * time in milliseconds, by using this duration as timeout.
   *
   * @param milliTime the starting system time in milliseconds.
   * @return whether the sleep happened at all.
   * @throws java.lang.IllegalStateException if this duration overflows the maximum sleep time.
   * @throws java.lang.InterruptedException  if the current thread is interrupted.
   * @see System#currentTimeMillis()
   */
  public boolean sleepSinceMillis(final long milliTime) throws InterruptedException {
    return sleepSinceMillis(value, unit, milliTime);
  }

  /**
   * Performs a {@link java.lang.Thread#sleep(long, int)} as if started from the specified high
   * precision system time in nanoseconds, by using this duration as timeout.
   *
   * @param nanoTime the starting system time in nanoseconds.
   * @return whether the sleep happened at all.
   * @throws java.lang.IllegalStateException if this duration overflows the maximum sleep time.
   * @throws java.lang.InterruptedException  if the current thread is interrupted.
   * @see System#nanoTime()
   */
  public boolean sleepSinceNanos(final long nanoTime) throws InterruptedException {
    return sleepSinceNanos(value, unit, nanoTime);
  }

  /**
   * Performs an {@link java.lang.Object#wait()} using this duration as timeout.
   *
   * @param target the target object.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public void wait(@NotNull final Object target) throws InterruptedException {
    wait(isInfinite() ? -1 : value, unit, target);
  }

  /**
   * Performs an {@link java.lang.Object#wait()} as if started from the specified system time in
   * milliseconds, by using this duration as timeout.
   *
   * @param target    the target object.
   * @param milliTime the starting system time in milliseconds.
   * @return whether the wait happened at all.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   * @see System#currentTimeMillis()
   */
  public boolean waitSinceMillis(@NotNull final Object target, final long milliTime) throws
      InterruptedException {
    return waitSinceMillis(isInfinite() ? -1 : value, unit, target, milliTime);
  }

  /**
   * Performs an {@link java.lang.Object#wait()} as if started from the specified high precision
   * system time in nanoseconds, by using this duration as timeout.
   *
   * @param target   the target object.
   * @param nanoTime the starting system time in nanoseconds.
   * @return whether the wait happened at all.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   * @see System#nanoTime()
   */
  public boolean waitSinceNanos(@NotNull final Object target, final long nanoTime) throws
      InterruptedException {
    return waitSinceNanos(isInfinite() ? -1 : value, unit, target, nanoTime);
  }

  /**
   * Waits for the specified condition to be true by performing an {@link java.lang.Object#wait()}
   * and using this duration as timeout.
   *
   * @param target    the target object.
   * @param condition the condition to verify.
   * @return whether the check became true before the timeout elapsed.
   * @throws java.lang.InterruptedException if the current thread is interrupted.
   */
  public boolean waitTrue(@NotNull final Object target, @NotNull final Condition condition) throws
      InterruptedException {
    return waitTrue(isInfinite() ? -1 : value, unit, target, condition);
  }

  /**
   * Interface defining a condition to check.
   */
  public interface Condition {

    /**
     * Checks if true.
     *
     * @return whether the condition is verified.
     */
    boolean isTrue();
  }
}
