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
 * Created by davide on 9/9/14.
 */
public class Time {

    public static final Time ZERO = from(0, TimeUnit.MILLISECONDS);

    public final long time;

    public final TimeUnit unit;

    protected Time(final long time, final TimeUnit unit) {

        this.time = time;
        this.unit = unit;
    }

    public static Time current() {

        return millis(System.currentTimeMillis());
    }

    public static Time days(final long days) {

        return new Time(days, TimeUnit.DAYS);
    }

    public static Time from(final long time, TimeUnit unit) {

        if (unit == null) {

            throw new IllegalArgumentException();
        }

        return new Time(time, unit);
    }

    public static Time hours(final long hours) {

        return new Time(hours, TimeUnit.HOURS);
    }

    public static Time micros(final long micros) {

        return new Time(micros, TimeUnit.MICROSECONDS);
    }

    public static Time millis(final long millis) {

        return new Time(millis, TimeUnit.MILLISECONDS);
    }

    public static Time minutes(final long minutes) {

        return new Time(minutes, TimeUnit.MINUTES);
    }

    public static Time nano() {

        return nanos(System.nanoTime());
    }

    public static Time nanos(final long nanos) {

        return new Time(nanos, TimeUnit.NANOSECONDS);
    }

    public static Time seconds(final long seconds) {

        return new Time(seconds, TimeUnit.SECONDS);
    }

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

        final Time time = (Time) o;

        return (this.time == time.time) && (unit == time.unit);
    }

    public Time hoursTime() {

        return hours(toHours());
    }

    public boolean isZero() {

        return (time == 0);
    }

    public Time microsTime() {

        return micros(toMicros());
    }

    public Time millisTime() {

        return millis(toMillis());
    }

    public Time minutesTime() {

        return minutes(toMinutes());
    }

    public Time nanosTime() {

        return nanos(toNanos());
    }

    public Time secondsTime() {

        return seconds(toSeconds());
    }

    public long to(final TimeUnit unit) {

        return this.unit.convert(time, unit);
    }

    public long toDays() {

        return unit.toDays(time);
    }

    public long toHours() {

        return unit.toHours(time);
    }

    public long toMicros() {

        return unit.toMicros(time);
    }

    public long toMillis() {

        return unit.toMillis(time);
    }

    public long toMinutes() {

        return unit.toMinutes(time);
    }

    public long toNanos() {

        return unit.toNanos(time);
    }

    public long toSeconds() {

        return unit.toSeconds(time);
    }
}