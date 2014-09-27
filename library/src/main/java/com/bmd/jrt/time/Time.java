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
 * Utility class for handling time in different time units.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class Time {

    /**
     * The time value.
     */
    public final long time;

    /**
     * The time unit.
     */
    public final TimeUnit unit;

    /**
     * Constructor.
     *
     * @param time the time value.
     * @param unit the time unit.
     */
    protected Time(final long time, final TimeUnit unit) {

        this.time = time;
        this.unit = unit;
    }

    /**
     * Creates and returns an object representing the current system time in milliseconds.
     *
     * @return the time instance.
     * @see System#currentTimeMillis()
     */
    public static Time current() {

        return millis(System.currentTimeMillis());
    }

    /**
     * Creates and returns an object representing the specified number of days.
     *
     * @param days the number of days.
     * @return the time instance.
     */
    public static Time days(final long days) {

        return new Time(days, TimeUnit.DAYS);
    }

    /**
     * Creates and returns an object representing the specified time value in the specified time
     * unit.
     *
     * @param time the time value.
     * @param unit the time unit.
     * @return the time instance.
     * @throws java.lang.IllegalArgumentException if the specified time unit is null.
     */
    public static Time fromUnit(final long time, TimeUnit unit) {

        if (unit == null) {

            throw new IllegalArgumentException("the time unit cannot be null");
        }

        return new Time(time, unit);
    }

    /**
     * Creates and returns an object representing the specified number of hours.
     *
     * @param hours the number of hours
     * @return the time instance.
     */
    public static Time hours(final long hours) {

        return new Time(hours, TimeUnit.HOURS);
    }

    /**
     * Creates and returns an object representing the specified number of microseconds.
     *
     * @param micros the number of microseconds.
     * @return the time instance.
     */
    public static Time micros(final long micros) {

        return new Time(micros, TimeUnit.MICROSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of milliseconds.
     *
     * @param millis the number of milliseconds.
     * @return the time instance.
     */
    public static Time millis(final long millis) {

        return new Time(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of minutes.
     *
     * @param minutes the number of minutes.
     * @return the time instance.
     */
    public static Time minutes(final long minutes) {

        return new Time(minutes, TimeUnit.MINUTES);
    }

    /**
     * Creates and returns an object representing the current high precision system time in
     * nanoseconds.
     *
     * @return the time instance.
     * @see System#nanoTime()
     */
    public static Time nano() {

        return nanos(System.nanoTime());
    }

    /**
     * Creates and returns an object representing the specified number of nanoseconds.
     *
     * @param nanos the number of nanoseconds.
     * @return the time instance.
     */
    public static Time nanos(final long nanos) {

        return new Time(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of seconds.
     *
     * @param seconds the number of seconds.
     * @return the time instance.
     */
    public static Time seconds(final long seconds) {

        return new Time(seconds, TimeUnit.SECONDS);
    }

    /**
     * Converts this time in days.
     *
     * @return the time instance.
     */
    public Time daysTime() {

        return days(toDays());
    }

    @Override
    public int hashCode() {

        int result = (int) (time ^ (time >>> 32));
        result = 31 * result + unit.hashCode();

        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof Time)) {

            return false;
        }

        final Time that = (Time) o;

        if (unit != that.unit) {

            return (to(that.unit) == that.time) && (that.to(unit) == time);
        }

        return (time == that.time);
    }

    /**
     * Converts this time in hours.
     *
     * @return the time instance.
     */
    public Time hoursTime() {

        return hours(toHours());
    }

    /**
     * Checks if this time is zero.
     *
     * @return whether this time value is zero.
     */
    public boolean isZero() {

        return (time == 0);
    }

    /**
     * Converts this time in microseconds.
     *
     * @return the time instance.
     */
    public Time microsTime() {

        return micros(toMicros());
    }

    /**
     * Converts this time in milliseconds.
     *
     * @return the time instance.
     */
    public Time millisTime() {

        return millis(toMillis());
    }

    /**
     * Converts this time in minutes.
     *
     * @return the time instance.
     */
    public Time minutesTime() {

        return minutes(toMinutes());
    }

    /**
     * Converts this time in nanoseconds.
     *
     * @return the time instance.
     */
    public Time nanosTime() {

        return nanos(toNanos());
    }

    /**
     * Converts this time in seconds.
     *
     * @return the time instance.
     */
    public Time secondsTime() {

        return seconds(toSeconds());
    }

    /**
     * Converts this time in the specified unit.
     *
     * @return the time value in the specified unit.
     */
    public long to(final TimeUnit timeUnit) {

        return unit.convert(time, timeUnit);
    }

    /**
     * Converts this time in number of days.
     *
     * @return the number of days.
     */
    public long toDays() {

        return unit.toDays(time);
    }

    /**
     * Converts this time in number of hours.
     *
     * @return the number of hours.
     */
    public long toHours() {

        return unit.toHours(time);
    }

    /**
     * Converts this time in number of microseconds.
     *
     * @return the number of microseconds.
     */
    public long toMicros() {

        return unit.toMicros(time);
    }

    /**
     * Converts this time in number of milliseconds.
     *
     * @return the number of milliseconds.
     */
    public long toMillis() {

        return unit.toMillis(time);
    }

    /**
     * Converts this time in number of minutes.
     *
     * @return the number of minutes.
     */
    public long toMinutes() {

        return unit.toMinutes(time);
    }

    /**
     * Converts this time in number of nanoseconds.
     *
     * @return the number of nanoseconds.
     */
    public long toNanos() {

        return unit.toNanos(time);
    }

    /**
     * Converts this time in number of seconds.
     *
     * @return the number of seconds.
     */
    public long toSeconds() {

        return unit.toSeconds(time);
    }
}