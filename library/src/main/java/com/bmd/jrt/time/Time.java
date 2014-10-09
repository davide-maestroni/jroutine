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

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Utility class for handling time in different time units.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class Time {

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

    protected static final long MAX_DAYS = Long.MAX_VALUE / SECONDS_IN_DAY;

    protected static final long MAX_HOURS = Long.MAX_VALUE / SECONDS_IN_HOUR;

    protected static final long MAX_MINUTES = Long.MAX_VALUE / SECONDS_IN_MINUTE;

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
    protected Time(final long time, @NonNull final TimeUnit unit) {

        this.time = time;
        this.unit = unit;
    }

    /**
     * Creates and returns an object representing the current system time in milliseconds.
     *
     * @return the time instance.
     * @see System#currentTimeMillis()
     */
    @NonNull
    public static Time current() {

        return millis(System.currentTimeMillis());
    }

    /**
     * Creates and returns an object representing the specified number of days.
     *
     * @param days the number of days.
     * @return the time instance.
     */
    @NonNull
    public static Time days(final long days) {

        if ((days > MAX_DAYS) || (days < -MAX_DAYS)) {

            throw new IllegalArgumentException("time value overflow");
        }

        return new Time(days * SECONDS_IN_DAY, TimeUnit.SECONDS);
    }

    /**
     * Creates and returns an object representing the specified time value in the specified time
     * unit.
     *
     * @param time the time value.
     * @param unit the time unit.
     * @return the time instance.
     * @throws NullPointerException if the specified time unit is null.
     */
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public static Time fromUnit(final long time, @NonNull TimeUnit unit) {

        if (unit == null) {

            throw new NullPointerException("the time unit cannot be null");
        }

        return new Time(time, unit);
    }

    /**
     * Creates and returns an object representing the specified number of hours.
     *
     * @param hours the number of hours
     * @return the time instance.
     */
    @NonNull
    public static Time hours(final long hours) {

        if ((hours > MAX_HOURS) || (hours < -MAX_HOURS)) {

            throw new IllegalArgumentException("time value overflow");
        }

        return new Time(hours * SECONDS_IN_HOUR, TimeUnit.SECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of microseconds.
     *
     * @param micros the number of microseconds.
     * @return the time instance.
     */
    @NonNull
    public static Time micros(final long micros) {

        return new Time(micros, TimeUnit.MICROSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of milliseconds.
     *
     * @param millis the number of milliseconds.
     * @return the time instance.
     */
    @NonNull
    public static Time millis(final long millis) {

        return new Time(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of minutes.
     *
     * @param minutes the number of minutes.
     * @return the time instance.
     */
    @NonNull
    public static Time minutes(final long minutes) {

        if ((minutes > MAX_MINUTES) || (minutes < -MAX_MINUTES)) {

            throw new IllegalArgumentException("time value overflow");
        }

        return new Time(minutes * SECONDS_IN_MINUTE, TimeUnit.SECONDS);
    }

    /**
     * Creates and returns an object representing the current high precision system time in
     * nanoseconds.
     *
     * @return the time instance.
     * @see System#nanoTime()
     */
    @NonNull
    public static Time nano() {

        return nanos(System.nanoTime());
    }

    /**
     * Creates and returns an object representing the specified number of nanoseconds.
     *
     * @param nanos the number of nanoseconds.
     * @return the time instance.
     */
    @NonNull
    public static Time nanos(final long nanos) {

        return new Time(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Creates and returns an object representing the specified number of seconds.
     *
     * @param seconds the number of seconds.
     * @return the time instance.
     */
    @NonNull
    public static Time seconds(final long seconds) {

        return new Time(seconds, TimeUnit.SECONDS);
    }

    /**
     * Converts this time in days.
     *
     * @return the time instance.
     */
    @NonNull
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

    @Override
    public String toString() {

        return time + " " + unit.toString();
    }

    /**
     * Converts this time in hours.
     *
     * @return the time instance.
     */
    @NonNull
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
    @NonNull
    public Time microsTime() {

        return micros(toMicros());
    }

    /**
     * Converts this time in milliseconds.
     *
     * @return the time instance.
     */
    @NonNull
    public Time millisTime() {

        return millis(toMillis());
    }

    /**
     * Converts this time in minutes.
     *
     * @return the time instance.
     */
    @NonNull
    public Time minutesTime() {

        return minutes(toMinutes());
    }

    /**
     * Converts this time in nanoseconds.
     *
     * @return the time instance.
     */
    @NonNull
    public Time nanosTime() {

        return nanos(toNanos());
    }

    /**
     * Converts this time in seconds.
     *
     * @return the time instance.
     */
    @NonNull
    public Time secondsTime() {

        return seconds(toSeconds());
    }

    /**
     * Converts this time in the specified unit.
     *
     * @return the time value in the specified unit.
     * @throws NullPointerException if the specified time unit is null.
     */
    public long to(final TimeUnit timeUnit) {

        return timeUnit.convert(time, unit);
    }

    /**
     * Converts this time in number of days.
     *
     * @return the number of days.
     */
    public long toDays() {

        return unit.toSeconds(time) / SECONDS_IN_DAY;
    }

    /**
     * Converts this time in number of hours.
     *
     * @return the number of hours.
     */
    public long toHours() {

        return unit.toSeconds(time) / SECONDS_IN_HOUR;
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

        return unit.toSeconds(time) / SECONDS_IN_MINUTE;
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