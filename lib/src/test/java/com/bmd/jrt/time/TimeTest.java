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

import junit.framework.TestCase;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Time unit tests.
 * <p/>
 * Created by davide on 10/2/14.
 */
public class TimeTest extends TestCase {

    private static final long ONE_DAY_NANOS = Time.days(1).toNanos();

    private static final long MAX_TIME = Long.MAX_VALUE / ONE_DAY_NANOS;

    private static final long MIN_TIME = Long.MIN_VALUE / ONE_DAY_NANOS;

    private static long clip(final int i) {

        return Math.min(Math.max(MIN_TIME, i), MAX_TIME);
    }

    public void testConstants() {

        assertThat(Time.SECONDS_IN_MINUTE).isEqualTo(60);
        assertThat(Time.SECONDS_IN_HOUR).isEqualTo(60 * 60);
        assertThat(Time.SECONDS_IN_DAY).isEqualTo(60 * 60 * 24);
        assertThat(Time.MINUTES_IN_HOUR).isEqualTo(60);
        assertThat(Time.MINUTES_IN_DAY).isEqualTo(60 * 24);
        assertThat(Time.HOURS_IN_DAY).isEqualTo(24);
    }

    public void testConversions() {

        final Random random = new Random();

        testConversions(Time.nanos(clip(random.nextInt())), true);
        testConversions(Time.micros(clip(random.nextInt())), true);
        testConversions(Time.millis(clip(random.nextInt())), true);
        testConversions(Time.seconds(clip(random.nextInt())), true);
        testConversions(Time.minutes(clip(random.nextInt())), true);
        testConversions(Time.hours(clip(random.nextInt())), true);
        testConversions(Time.days(clip(random.nextInt())), true);

        testConversions(Time.fromUnit(clip(random.nextInt()), TimeUnit.NANOSECONDS), true);
        testConversions(Time.fromUnit(clip(random.nextInt()), TimeUnit.MICROSECONDS), true);
        testConversions(Time.fromUnit(clip(random.nextInt()), TimeUnit.MILLISECONDS), true);
        testConversions(Time.fromUnit(clip(random.nextInt()), TimeUnit.SECONDS), true);
        // Java 6
        //        testConversions(Time.fromUnit(clip(random.nextInt()), TimeUnit.MINUTES), true);
        //        testConversions(Time.fromUnit(clip(random.nextInt()), TimeUnit.HOURS), true);
        //        testConversions(Time.fromUnit(clip(random.nextInt()), TimeUnit.DAYS), true);

        final Time time = Time.nanos(clip(random.nextInt()));
        assertThat(time).isEqualTo(time);
        assertThat(time).isEqualTo(time.nanosTime());
        assertThat(time).isNotEqualTo(time.millisTime());
        assertThat(time.equals(new Object())).isFalse();
        assertThat(time.hashCode()).isEqualTo(time.nanosTime().hashCode());
    }

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            Time.fromUnit(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Time.seconds(1).to(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Time.minutes(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Time.minutes(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Time.hours(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Time.hours(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Time.days(Long.MAX_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Time.days(Long.MIN_VALUE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testZero() {

        assertThat(Time.nanos(0).isZero()).isTrue();
        assertThat(Time.micros(0).isZero()).isTrue();
        assertThat(Time.millis(0).isZero()).isTrue();
        assertThat(Time.seconds(0).isZero()).isTrue();
        assertThat(Time.minutes(0).isZero()).isTrue();
        assertThat(Time.hours(0).isZero()).isTrue();
        assertThat(Time.days(0).isZero()).isTrue();

        assertThat(Time.fromUnit(0, TimeUnit.NANOSECONDS).isZero()).isTrue();
        assertThat(Time.fromUnit(0, TimeUnit.MICROSECONDS).isZero()).isTrue();
        assertThat(Time.fromUnit(0, TimeUnit.MILLISECONDS).isZero()).isTrue();
        assertThat(Time.fromUnit(0, TimeUnit.SECONDS).isZero()).isTrue();
        // Java 6
        //        assertThat(Time.fromUnit(0, TimeUnit.MINUTES).isZero()).isTrue();
        //        assertThat(Time.fromUnit(0, TimeUnit.HOURS).isZero()).isTrue();
        //        assertThat(Time.fromUnit(0, TimeUnit.DAYS).isZero()).isTrue();
    }

    private void testConversions(final Time time, final boolean isFirst) {

        final long value = time.time;
        final TimeUnit unit = time.unit;

        assertThat(time.toNanos()).isEqualTo(unit.toNanos(value));
        assertThat(time.toMicros()).isEqualTo(unit.toMicros(value));
        assertThat(time.toMillis()).isEqualTo(unit.toMillis(value));
        assertThat(time.toSeconds()).isEqualTo(unit.toSeconds(value));
        // Java 6
        //        assertThat(time.toMinutes()).isEqualTo(unit.toMinutes(value));
        //        assertThat(time.toHours()).isEqualTo(unit.toHours(value));
        //        assertThat(time.toDays()).isEqualTo(unit.toDays(value));

        assertThat(time.to(TimeUnit.NANOSECONDS)).isEqualTo(
                TimeUnit.NANOSECONDS.convert(value, unit));
        assertThat(time.to(TimeUnit.MICROSECONDS)).isEqualTo(
                TimeUnit.MICROSECONDS.convert(value, unit));
        assertThat(time.to(TimeUnit.MILLISECONDS)).isEqualTo(
                TimeUnit.MILLISECONDS.convert(value, unit));
        assertThat(time.to(TimeUnit.SECONDS)).isEqualTo(TimeUnit.SECONDS.convert(value, unit));
        // Java 6
        //        assertThat(time.to(TimeUnit.MINUTES)).isEqualTo(TimeUnit.MINUTES.convert(value,
        // unit));
        //        assertThat(time.to(TimeUnit.HOURS)).isEqualTo(TimeUnit.HOURS.convert(value,
        // unit));
        //        assertThat(time.to(TimeUnit.DAYS)).isEqualTo(TimeUnit.DAYS.convert(value, unit));

        assertThat(time).isEqualTo(time);
        assertThat(time).isEqualTo(Time.fromUnit(time.time, time.unit));
        assertThat(time.nanosTime()).isEqualTo(
                Time.fromUnit(time.nanosTime().to(time.unit), time.unit));
        assertThat(time.microsTime()).isEqualTo(
                Time.fromUnit(time.microsTime().to(time.unit), time.unit));
        assertThat(time.millisTime()).isEqualTo(
                Time.fromUnit(time.millisTime().to(time.unit), time.unit));
        assertThat(time.secondsTime()).isEqualTo(
                Time.fromUnit(time.secondsTime().to(time.unit), time.unit));
        assertThat(time.minutesTime()).isEqualTo(
                Time.fromUnit(time.minutesTime().to(time.unit), time.unit));
        assertThat(time.hoursTime()).isEqualTo(
                Time.fromUnit(time.hoursTime().to(time.unit), time.unit));
        assertThat(time.daysTime()).isEqualTo(
                Time.fromUnit(time.daysTime().to(time.unit), time.unit));

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