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

package com.github.dm.jrt.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling a time duration in different time units.
 * <p/>
 * Created by davide-maestroni on 09/09/2014.
 */
public class TimeDuration extends Time {

    /**
     * Time duration instance representing the infinity.
     */
    public static final TimeDuration INFINITY = seconds(Long.MAX_VALUE);

    /**
     * Time duration instance representing the zero.
     */
    public static final TimeDuration ZERO = seconds(0);

    private static final long MILLI_DAYS_OVERFLOW = 106751991167L;

    private static final long NANO_DAYS_OVERFLOW = 106750L;

    private static final long ONE_MILLI_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    /**
     * Constructor.
     *
     * @param duration the time value.
     * @param unit     the time unit.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    protected TimeDuration(final long duration, @NotNull final TimeUnit unit) {

        super(duration, unit);
        if (duration < 0) {
            throw new IllegalArgumentException("the time duration cannot be negative: " + duration);
        }
    }

    /**
     * Creates and returns an object representing the specified number of days.
     *
     * @param days the number of days.
     * @return the time instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    public static TimeDuration days(final long days) {

        if ((days > MAX_DAYS) || (days < -MAX_DAYS)) {
            throw new IllegalArgumentException("time value overflow: " + days + " days");
        }

        return new TimeDuration(days * SECONDS_IN_DAY, TimeUnit.SECONDS);
    }

    /**
     * Creates and returns an object representing the specified time value in the specified time
     * unit.
     *
     * @param time the time value.
     * @param unit the time unit.
     * @return the time instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static TimeDuration fromUnit(final long time, @NotNull final TimeUnit unit) {

        if (unit == null) {
            throw new NullPointerException("the time unit must not be null");
        }

        return new TimeDuration(time, unit);
    }

    /**
     * Creates and returns an object representing the specified number of hours.
     *
     * @param hours the number of hours
     * @return the time instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    public static TimeDuration hours(final long hours) {

        if ((hours > MAX_HOURS) || (hours < -MAX_HOURS)) {
            throw new IllegalArgumentException("time value overflow: " + hours + " hours");
        }

        return new TimeDuration(hours * SECONDS_IN_HOUR, TimeUnit.SECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of microseconds.
     *
     * @param micros the number of microseconds.
     * @return the time duration instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    public static TimeDuration micros(final long micros) {

        return new TimeDuration(micros, TimeUnit.MICROSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of milliseconds.
     *
     * @param millis the number of milliseconds.
     * @return the time duration instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    public static TimeDuration millis(final long millis) {

        return new TimeDuration(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of minutes.
     *
     * @param minutes the number of minutes.
     * @return the time duration instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    public static TimeDuration minutes(final long minutes) {

        if ((minutes > MAX_MINUTES) || (minutes < -MAX_MINUTES)) {
            throw new IllegalArgumentException("time value overflow: " + minutes + " minutes");
        }

        return new TimeDuration(minutes * SECONDS_IN_MINUTE, TimeUnit.SECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of nanoseconds.
     *
     * @param nanos the number of nanoseconds.
     * @return the time duration instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    public static TimeDuration nanos(final long nanos) {

        return new TimeDuration(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of seconds.
     *
     * @param seconds the number of seconds.
     * @return the time duration instance.
     * @throws java.lang.IllegalArgumentException if the specified duration is negative.
     */
    @NotNull
    public static TimeDuration seconds(final long seconds) {

        return new TimeDuration(seconds, TimeUnit.SECONDS);
    }

    /**
     * Creates and returns an object representing the time range between now and a time in the past.
     * <br/>
     * If the specified is in the future, a {@code ZERO} duration will be returned.
     *
     * @param milliTime the system time in milliseconds.
     * @return the time duration instance.
     * @see java.lang.System#currentTimeMillis()
     */
    @NotNull
    public static TimeDuration timeSinceMillis(final long milliTime) {

        final long millis = System.currentTimeMillis() - milliTime;
        return (millis > 0) ? millis(millis) : ZERO;
    }

    /**
     * Creates and returns an object representing the time range between now and a time in the past.
     * <br/>
     * If the specified is in the future, a {@code ZERO} duration will be returned.
     *
     * @param nanoTime the high precision system time in nanoseconds.
     * @return the time duration instance.
     * @see java.lang.System#nanoTime()
     */
    @NotNull
    public static TimeDuration timeSinceNanos(final long nanoTime) {

        final long nanos = System.nanoTime() - nanoTime;
        return (nanos > 0) ? nanos(nanos) : ZERO;
    }

    /**
     * Creates and returns an object representing the time range between now and a time in the
     * future.<br/>
     * If the specified is in the past, a {@code ZERO} duration will be returned.
     *
     * @param milliTime the system time in milliseconds.
     * @return the time duration instance.
     * @see java.lang.System#currentTimeMillis()
     */
    @NotNull
    public static TimeDuration timeUntilMillis(final long milliTime) {

        final long millis = milliTime - System.currentTimeMillis();
        return (millis > 0) ? millis(millis) : ZERO;
    }

    /**
     * Creates and returns an object representing the time range between now and a time in the
     * future.<br/>
     * If the specified is in the past, a {@code ZERO} duration will be returned.
     *
     * @param nanoTime the high precision system time in nanoseconds.
     * @return the time duration instance.
     * @see java.lang.System#nanoTime()
     */
    @NotNull
    public static TimeDuration timeUntilNanos(final long nanoTime) {

        final long nanos = nanoTime - System.nanoTime();
        return (nanos > 0) ? nanos(nanos) : ZERO;
    }

    /**
     * Converts this duration in days.
     *
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration daysTime() {

        return days(toDays());
    }

    /**
     * Converts this duration in hours.
     *
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration hoursTime() {

        return hours(toHours());
    }

    /**
     * Converts this duration in microseconds.
     *
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration microsTime() {

        return micros(toMicros());
    }

    /**
     * Converts this duration in milliseconds.
     *
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration millisTime() {

        return millis(toMillis());
    }

    /**
     * Returns a new instance whose time value is decremented by the specified one.<br/>
     * Note that the unit of the returned time will match the one with the highest precision.<br/>
     * Note also that, if the resulting time is negative, the value will be clipped to 0.
     *
     * @param time the time to subtract.
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration minus(@NotNull final Time time) {

        if (unit.compareTo(time.unit) > 0) {
            return fromUnit(Math.max(0, time.unit.convert(this.time, unit) - time.time), time.unit);
        }

        return fromUnit(Math.max(0, this.time - unit.convert(time.time, time.unit)), unit);
    }

    /**
     * Converts this duration in minutes.
     *
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration minutesTime() {

        return minutes(toMinutes());
    }

    /**
     * Converts this duration in nanoseconds.
     *
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration nanosTime() {

        return nanos(toNanos());
    }

    /**
     * Returns a new instance whose time value is incremented by the specified one.<br/>
     * Note that the unit of the returned time will match the one with the highest precision.<br/>
     * Note also that, if the resulting time is negative, the value will be clipped to 0.
     *
     * @param time the time to add.
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration plus(@NotNull final Time time) {

        if (unit.compareTo(time.unit) > 0) {
            return fromUnit(Math.max(0, time.unit.convert(this.time, unit) + time.time), time.unit);
        }

        return fromUnit(Math.max(0, this.time + unit.convert(time.time, time.unit)), unit);
    }

    /**
     * Converts this duration in seconds.
     *
     * @return the time duration instance.
     */
    @NotNull
    @Override
    public TimeDuration secondsTime() {

        return seconds(toSeconds());
    }

    /**
     * Checks if this duration represents the infinity.
     *
     * @return whether this duration is infinite.
     */
    public boolean isInfinity() {

        return equals(INFINITY);
    }

    /**
     * Performs a {@link java.lang.Thread#join()} using this duration as timeout.
     *
     * @param target the target thread.
     * @throws java.lang.InterruptedException if the current thread is interrupted.
     */
    public void join(@NotNull final Thread target) throws InterruptedException {

        unit.timedJoin(target, time);
    }

    /**
     * Performs a {@link java.lang.Thread#sleep(long, int)} using this duration as timeout.
     *
     * @throws java.lang.InterruptedException if the current thread is interrupted.
     */
    public void sleep() throws InterruptedException {

        unit.sleep(time);
    }

    /**
     * Performs a {@link java.lang.Thread#sleep(long, int)} using this duration as timeout, ensuring
     * that the sleep time is respected even if spurious wake ups happen in the while.
     *
     * @throws java.lang.InterruptedException if the current thread is interrupted.
     */
    public void sleepAtLeast() throws InterruptedException {

        if (isZero()) {
            return;
        }

        if (((toNanos() % ONE_MILLI_NANOS) == 0) || (toDays() > NANO_DAYS_OVERFLOW)) {
            final long startMillis = System.currentTimeMillis();
            while (true) {
                if (!sleepSinceMillis(startMillis)) {
                    return;
                }
            }
        }

        final long startNanos = System.nanoTime();
        while (true) {
            if (!sleepSinceNanos(startNanos)) {
                return;
            }
        }
    }

    /**
     * Performs a {@link java.lang.Thread#sleep(long, int)} as if started from the specified system
     * time in milliseconds, by using this duration as timeout.
     *
     * @param milliTime the starting system time in milliseconds.
     * @return whether the sleep happened at all.
     * @throws java.lang.IllegalStateException if this duration overflows the maximum sleep time.
     * @throws java.lang.InterruptedException  if the current thread is interrupted.
     * @see java.lang.System#currentTimeMillis()
     */
    public boolean sleepSinceMillis(final long milliTime) throws InterruptedException {

        if (isZero()) {
            return false;
        }

        if (toDays() > MILLI_DAYS_OVERFLOW) {
            throw new IllegalStateException("the duration overflows the maximum sleep time: " +
                                                    toDays() + " days");
        }

        final long millisToSleep = milliTime - System.currentTimeMillis() + toMillis();
        if (millisToSleep <= 0) {
            return false;
        }

        TimeUnit.MILLISECONDS.sleep(millisToSleep);
        return true;
    }

    /**
     * Performs a {@link java.lang.Thread#sleep(long, int)} as if started from the specified high
     * precision system time in nanoseconds, by using this duration as timeout.
     *
     * @param nanoTime the starting system time in nanoseconds.
     * @return whether the sleep happened at all.
     * @throws java.lang.IllegalStateException if this duration overflows the maximum sleep time.
     * @throws java.lang.InterruptedException  if the current thread is interrupted.
     * @see java.lang.System#nanoTime()
     */
    public boolean sleepSinceNanos(final long nanoTime) throws InterruptedException {

        if (isZero()) {
            return false;
        }

        if (toDays() > NANO_DAYS_OVERFLOW) {
            throw new IllegalStateException("the duration overflows the maximum sleep time: " +
                                                    toDays() + " days");
        }

        final long nanosToSleep = nanoTime - System.nanoTime() + toNanos();
        if (nanosToSleep <= 0) {
            return false;
        }

        TimeUnit.NANOSECONDS.sleep(nanosToSleep);
        return true;
    }

    /**
     * Performs an {@link java.lang.Object#wait()} using this duration as timeout.
     *
     * @param target the target object.
     * @throws java.lang.InterruptedException if the current thread is interrupted.
     */
    public void wait(@NotNull final Object target) throws InterruptedException {

        if (isZero()) {
            return;
        }

        if (isInfinity()) {
            target.wait();
            return;
        }

        unit.timedWait(target, time);
    }

    /**
     * Performs an {@link java.lang.Object#wait()} as if started from the specified system time in
     * milliseconds, by using this duration as timeout.
     *
     * @param target    the target object.
     * @param milliTime the starting system time in milliseconds.
     * @return whether the wait happened at all.
     * @throws java.lang.InterruptedException if the current thread is interrupted.
     * @see java.lang.System#currentTimeMillis()
     */
    public boolean waitSinceMillis(@NotNull final Object target, final long milliTime) throws
            InterruptedException {

        if (isZero()) {
            return false;
        }

        if (isInfinity() || (toDays() > MILLI_DAYS_OVERFLOW)) {
            target.wait();
            return true;
        }

        final long millisToWait = milliTime - System.currentTimeMillis() + toMillis();
        if (millisToWait <= 0) {
            return false;
        }

        TimeUnit.MILLISECONDS.timedWait(target, millisToWait);
        return true;
    }

    /**
     * Performs an {@link java.lang.Object#wait()} as if started from the specified high precision
     * system time in nanoseconds, by using this duration as timeout.
     *
     * @param target   the target object.
     * @param nanoTime the starting system time in nanoseconds.
     * @return whether the wait happened at all.
     * @throws java.lang.InterruptedException if the current thread is interrupted.
     * @see java.lang.System#nanoTime()
     */
    public boolean waitSinceNanos(@NotNull final Object target, final long nanoTime) throws
            InterruptedException {

        if (isZero()) {
            return false;
        }

        if (isInfinity() || (toDays() > NANO_DAYS_OVERFLOW)) {
            target.wait();
            return true;
        }

        final long nanosToWait = nanoTime - System.nanoTime() + toNanos();
        if (nanosToWait <= 0) {
            return false;
        }

        TimeUnit.NANOSECONDS.timedWait(target, nanosToWait);
        return true;
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

        if (isZero()) {
            return condition.isTrue();
        }

        if (isInfinity()) {
            while (!condition.isTrue()) {
                target.wait();
            }

            return true;
        }

        if ((toNanos() % ONE_MILLI_NANOS) == 0) {
            final long startMillis = System.currentTimeMillis();
            while (!condition.isTrue()) {
                if (!waitSinceMillis(target, startMillis)) {
                    return false;
                }
            }

        } else {
            final long startNanos = System.nanoTime();
            while (!condition.isTrue()) {
                if (!waitSinceNanos(target, startNanos)) {
                    return false;
                }
            }
        }

        return true;
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
