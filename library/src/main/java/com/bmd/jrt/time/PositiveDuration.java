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
public class PositiveDuration extends Duration {

    public static final PositiveDuration INFINITE = days(Long.MAX_VALUE);

    public static final PositiveDuration ZERO = time(0, TimeUnit.MILLISECONDS);

    private static final long MILLI_DAYS_OVERFLOW = 106751991167L;

    private static final long NANO_DAYS_OVERFLOW = 106750L;

    protected PositiveDuration(final long time, final TimeUnit unit) {

        super(time, unit);

        if (time < 0) {

            throw new IllegalArgumentException();
        }
    }

    public static PositiveDuration days(final long days) {

        return new PositiveDuration(days, TimeUnit.DAYS);
    }

    public static PositiveDuration hours(final long hours) {

        return new PositiveDuration(hours, TimeUnit.HOURS);
    }

    public static PositiveDuration micros(final long micros) {

        return new PositiveDuration(micros, TimeUnit.MICROSECONDS);
    }

    public static PositiveDuration millis(final long millis) {

        return new PositiveDuration(millis, TimeUnit.MILLISECONDS);
    }

    public static PositiveDuration minutes(final long minutes) {

        return new PositiveDuration(minutes, TimeUnit.MINUTES);
    }

    public static PositiveDuration nanos(final long nanos) {

        return new PositiveDuration(nanos, TimeUnit.NANOSECONDS);
    }

    public static PositiveDuration seconds(final long seconds) {

        return new PositiveDuration(seconds, TimeUnit.SECONDS);
    }

    public static PositiveDuration time(final long time, final TimeUnit unit) {

        if (unit == null) {

            throw new IllegalArgumentException();
        }

        return new PositiveDuration(time, unit);
    }

    public PositiveDuration daysDuration() {

        return days(toDays());
    }

    public PositiveDuration hoursDuration() {

        return hours(toHours());
    }

    public PositiveDuration microsDuration() {

        return micros(toMicros());
    }

    public PositiveDuration millisDuration() {

        return millis(toMillis());
    }

    public PositiveDuration minutesDuration() {

        return minutes(toMinutes());
    }

    public PositiveDuration nanosDuration() {

        return nanos(toNanos());
    }

    public PositiveDuration secondsDuration() {

        return seconds(toSeconds());
    }

    public boolean isInfinite() {

        return equals(INFINITE);
    }

    public void join(final Thread target) throws InterruptedException {

        unit.timedJoin(target, time);
    }

    public void sleep() throws InterruptedException {

        unit.sleep(time);
    }

    public boolean sleepMillis(final long fromMillis) throws InterruptedException {

        if (isZero()) {

            return false;
        }

        if (toDays() > MILLI_DAYS_OVERFLOW) {

            throw new IllegalStateException();
        }

        final long millisToSleep = System.currentTimeMillis() - fromMillis - toMillis();

        if (millisToSleep <= 0) {

            return false;
        }

        TimeUnit.MILLISECONDS.sleep(millisToSleep);

        return true;
    }

    public boolean sleepNanos(final long fromNanos) throws InterruptedException {

        if (isZero()) {

            return false;
        }

        if (toDays() > NANO_DAYS_OVERFLOW) {

            throw new IllegalStateException();
        }

        final long nanosToSleep = System.nanoTime() - fromNanos - toNanos();

        if (nanosToSleep <= 0) {

            return false;
        }

        TimeUnit.NANOSECONDS.sleep(nanosToSleep);

        return true;
    }

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

    public boolean waitMillis(final Object target, final long fromMillis) throws
            InterruptedException {

        if (isZero()) {

            return false;
        }

        if (isInfinite() || (toDays() > MILLI_DAYS_OVERFLOW)) {

            target.wait();

            return true;
        }

        final long millisToWait = System.currentTimeMillis() - fromMillis - toMillis();

        if (millisToWait <= 0) {

            return false;
        }

        TimeUnit.MILLISECONDS.timedWait(target, millisToWait);

        return true;
    }

    public boolean waitNanos(final Object target, final long fromNanos) throws
            InterruptedException {

        if (isZero()) {

            return false;
        }

        if (isInfinite() || (toDays() > NANO_DAYS_OVERFLOW)) {

            target.wait();

            return true;
        }

        final long nanosToWait = System.nanoTime() - fromNanos - toNanos();

        if (nanosToWait <= 0) {

            return false;
        }

        TimeUnit.NANOSECONDS.timedWait(target, nanosToWait);

        return true;
    }
}