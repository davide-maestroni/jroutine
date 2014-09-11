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
public class Duration {

    public static final Duration ZERO = time(0, TimeUnit.MILLISECONDS);

    public final long time;

    public final TimeUnit unit;

    protected Duration(final long time, final TimeUnit unit) {

        this.time = time;
        this.unit = unit;
    }

    public static Duration days(final long days) {

        return new Duration(days, TimeUnit.DAYS);
    }

    public static Duration hours(final long hours) {

        return new Duration(hours, TimeUnit.HOURS);
    }

    public static Duration micros(final long micros) {

        return new Duration(micros, TimeUnit.MICROSECONDS);
    }

    public static Duration millis(final long millis) {

        return new Duration(millis, TimeUnit.MILLISECONDS);
    }

    public static Duration minutes(final long minutes) {

        return new Duration(minutes, TimeUnit.MINUTES);
    }

    public static Duration nanos(final long nanos) {

        return new Duration(nanos, TimeUnit.NANOSECONDS);
    }

    public static Duration seconds(final long seconds) {

        return new Duration(seconds, TimeUnit.SECONDS);
    }

    public static Duration time(final long time, TimeUnit unit) {

        if (unit == null) {

            throw new IllegalArgumentException();
        }

        return new Duration(time, unit);
    }

    public Duration daysDuration() {

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

        if (!(o instanceof Duration)) {

            return false;
        }

        final Duration duration = (Duration) o;

        return (time == duration.time) && (unit == duration.unit);
    }

    public Duration hoursDuration() {

        return hours(toHours());
    }

    public boolean isZero() {

        return (time == 0);
    }

    public Duration microsDuration() {

        return micros(toMicros());
    }

    public Duration millisDuration() {

        return millis(toMillis());
    }

    public Duration minutesDuration() {

        return minutes(toMinutes());
    }

    public Duration nanosDuration() {

        return nanos(toNanos());
    }

    public Duration secondsDuration() {

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