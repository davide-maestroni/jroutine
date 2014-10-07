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

import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling a time duration in different time units.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class TimeDuration extends Time {

    /**
     * Time duration instance representing the infinity.
     */
    public static final TimeDuration INFINITY = days(Long.MAX_VALUE);

    /**
     * Time duration instance representing the zero.
     */
    public static final TimeDuration ZERO = fromUnit(0, TimeUnit.MILLISECONDS);

    private static final long MILLI_DAYS_OVERFLOW = 106751991167L;

    private static final long NANO_DAYS_OVERFLOW = 106750L;

    private static final long ONE_MILLI_NANOS = millis(1).toNanos();

    /**
     * Constructor.
     *
     * @param duration the time value.
     * @param unit     the time unit.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    protected TimeDuration(final long duration, final TimeUnit unit) {

        super(duration, unit);

        if (duration < 0) {

            throw new IllegalArgumentException("the time duration cannot be negative");
        }
    }

    /**
     * Creates and returns an object representing the specified number of days.
     *
     * @param days the number of days.
     * @return the time instance.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration days(final long days) {

        return new TimeDuration(days, TimeUnit.DAYS);
    }

    /**
     * Creates and returns an object representing the specified time value in the specified time
     * unit.
     *
     * @param time the time value.
     * @param unit the time unit.
     * @return the time instance.
     * @throws NullPointerException     if the time unit is null.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration fromUnit(final long time, final TimeUnit unit) {

        if (unit == null) {

            throw new NullPointerException("the time unit cannot be null");
        }

        return new TimeDuration(time, unit);
    }

    /**
     * Creates and returns an object representing the specified number of hours.
     *
     * @param hours the number of hours
     * @return the time instance.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration hours(final long hours) {

        return new TimeDuration(hours, TimeUnit.HOURS);
    }

    /**
     * Creates and returns an object representing the specified number of microseconds.
     *
     * @param micros the number of microseconds.
     * @return the time duration instance.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration micros(final long micros) {

        return new TimeDuration(micros, TimeUnit.MICROSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of milliseconds.
     *
     * @param millis the number of milliseconds.
     * @return the time duration instance.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration millis(final long millis) {

        return new TimeDuration(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of minutes.
     *
     * @param minutes the number of minutes.
     * @return the time duration instance.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration minutes(final long minutes) {

        return new TimeDuration(minutes, TimeUnit.MINUTES);
    }

    /**
     * Creates and returns an object representing the specified number of nanoseconds.
     *
     * @param nanos the number of nanoseconds.
     * @return the time duration instance.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration nanos(final long nanos) {

        return new TimeDuration(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of seconds.
     *
     * @param seconds the number of seconds.
     * @return the time duration instance.
     * @throws IllegalArgumentException if the specified duration is negative.
     */
    public static TimeDuration seconds(final long seconds) {

        return new TimeDuration(seconds, TimeUnit.SECONDS);
    }

    /**
     * Converts this duration in days.
     *
     * @return the time duration instance.
     */
    public TimeDuration daysTime() {

        return days(toDays());
    }

    /**
     * Converts this duration in hours.
     *
     * @return the time duration instance.
     */
    public TimeDuration hoursTime() {

        return hours(toHours());
    }

    /**
     * Converts this duration in microseconds.
     *
     * @return the time duration instance.
     */
    public TimeDuration microsTime() {

        return micros(toMicros());
    }

    /**
     * Converts this duration in milliseconds.
     *
     * @return the time duration instance.
     */
    public TimeDuration millisTime() {

        return millis(toMillis());
    }

    /**
     * Converts this duration in minutes.
     *
     * @return the time duration instance.
     */
    public TimeDuration minutesTime() {

        return minutes(toMinutes());
    }

    /**
     * Converts this duration in nanoseconds.
     *
     * @return the time duration instance.
     */
    public TimeDuration nanosTime() {

        return nanos(toNanos());
    }

    /**
     * Converts this duration in seconds.
     *
     * @return the time duration instance.
     */
    public TimeDuration secondsTime() {

        return seconds(toSeconds());
    }

    /**
     * Checks if this duration represents the infinity.
     *
     * @return whether this duration is infinite.
     */
    public boolean isInfinite() {

        return equals(INFINITY);
    }

    /**
     * Performs a {@link Thread#join()} using this duration as timeout.
     *
     * @param target the target thread.
     * @throws InterruptedException if the current thread is interrupted.
     * @throws NullPointerException if the time unit is null.
     */
    public void join(final Thread target) throws InterruptedException {

        unit.timedJoin(target, time);
    }

    /**
     * Performs a {@link Thread#sleep(long, int)} using this duration as timeout.
     *
     * @throws InterruptedException if the current thread is interrupted.
     */
    public void sleep() throws InterruptedException {

        unit.sleep(time);
    }

    /**
     * Performs a {@link Thread#sleep(long, int)} using this duration as timeout, ensuring that
     * the sleep time is respected even if spurious wake ups happen in the while.
     *
     * @throws InterruptedException if the current thread is interrupted.
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
     * Performs a {@link Thread#sleep(long, int)} as if started from the specified system time in
     * milliseconds, by using this duration as timeout.
     *
     * @param milliTime the starting system time in milliseconds.
     * @return whether the sleep happened at all.
     * @throws InterruptedException  if the current thread is interrupted.
     * @throws IllegalStateException if this duration overflows the maximum sleep time.
     * @see System#currentTimeMillis()
     */
    public boolean sleepSinceMillis(final long milliTime) throws InterruptedException {

        if (isZero()) {

            return false;
        }

        if (toDays() > MILLI_DAYS_OVERFLOW) {

            throw new IllegalStateException("the duration overflows maximum sleep time");
        }

        final long millisToSleep = milliTime - System.currentTimeMillis() + toMillis();

        if (millisToSleep <= 0) {

            return false;
        }

        TimeUnit.MILLISECONDS.sleep(millisToSleep);

        return true;
    }

    /**
     * Performs a {@link Thread#sleep(long, int)} as if started from the specified high precision
     * system time in nanoseconds, by using this duration as timeout.
     *
     * @param nanoTime the starting system time in nanoseconds.
     * @return whether the sleep happened at all.
     * @throws InterruptedException  if the current thread is interrupted.
     * @throws IllegalStateException if this duration overflows the maximum sleep time.
     * @see System#nanoTime()
     */
    public boolean sleepSinceNanos(final long nanoTime) throws InterruptedException {

        if (isZero()) {

            return false;
        }

        if (toDays() > NANO_DAYS_OVERFLOW) {

            throw new IllegalStateException("the duration overflows maximum sleep time");
        }

        final long nanosToSleep = nanoTime - System.nanoTime() + toNanos();

        if (nanosToSleep <= 0) {

            return false;
        }

        TimeUnit.NANOSECONDS.sleep(nanosToSleep);

        return true;
    }

    /**
     * Performs an {@link Object#wait()} using this duration as timeout.
     *
     * @param target the target object.
     * @throws InterruptedException if the current thread is interrupted.
     * @throws NullPointerException if the target object is null.
     */
    public void wait(final Object target) throws InterruptedException {

        if (isZero()) {

            return;
        }

        if (isInfinite()) {

            target.wait();

            return;
        }

        unit.timedWait(target, time);
    }

    /**
     * Performs an {@link Object#wait()} as if started from the specified system time in
     * milliseconds, by using this duration as timeout.
     *
     * @param target    the target object.
     * @param milliTime the starting system time in milliseconds.
     * @return whether the wait happened at all.
     * @throws InterruptedException if the current thread is interrupted.
     * @throws NullPointerException if the target object is null.
     * @see System#currentTimeMillis()
     */
    public boolean waitSinceMillis(final Object target, final long milliTime) throws
            InterruptedException {

        if (isZero()) {

            return false;
        }

        if (isInfinite() || (toDays() > MILLI_DAYS_OVERFLOW)) {

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
     * Performs an {@link Object#wait()} as if started from the specified high precision system
     * time in nanoseconds, by using this duration as timeout.
     *
     * @param target   the target object.
     * @param nanoTime the starting system time in nanoseconds.
     * @return whether the wait happened at all.
     * @throws InterruptedException if the current thread is interrupted.
     * @throws NullPointerException if the target object is null.
     * @see System#nanoTime()
     */
    public boolean waitSinceNanos(final Object target, final long nanoTime) throws
            InterruptedException {

        if (isZero()) {

            return false;
        }

        if (isInfinite() || (toDays() > NANO_DAYS_OVERFLOW)) {

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
     * Waits for the specified check to be true by performing an {@link Object#wait()} and using
     * this duration as timeout.
     *
     * @param target the target object.
     * @param check  the check to verify.
     * @return whether the check became true before the timeout elapsed.
     * @throws InterruptedException if the current thread is interrupted.
     * @throws NullPointerException if the target object or the specified check are null.
     */
    public boolean waitTrue(final Object target, final Check check) throws InterruptedException {

        if (isZero()) {

            return check.isTrue();
        }

        if (isInfinite()) {

            while (!check.isTrue()) {

                target.wait();
            }

            return true;
        }

        if ((toNanos() % ONE_MILLI_NANOS) == 0) {

            final long startMillis = System.currentTimeMillis();

            while (!check.isTrue()) {

                if (!waitSinceMillis(target, startMillis)) {

                    return false;
                }
            }

        } else {

            final long startNanos = System.nanoTime();

            while (!check.isTrue()) {

                if (!waitSinceNanos(target, startNanos)) {

                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Interface defining a check to be performed.
     */
    public interface Check {

        /**
         * Checks if true.
         *
         * @return whether the check is verified.
         */
        public boolean isTrue();
    }
}