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
public class TimeDuration extends Time {

    public static final TimeDuration INFINITE = days(Long.MAX_VALUE);

    public static final TimeDuration ZERO = fromUnit(0, TimeUnit.MILLISECONDS);

    private static final long MILLI_DAYS_OVERFLOW = 106751991167L;

    private static final long NANO_DAYS_OVERFLOW = 106750L;

    private static final long ONE_MILLI_NANOS = millis(1).toNanos();

    protected TimeDuration(final long time, final TimeUnit unit) {

        super(time, unit);

        if (time < 0) {

            throw new IllegalArgumentException();
        }
    }

    public static TimeDuration days(final long days) {

        return new TimeDuration(days, TimeUnit.DAYS);
    }

    public static TimeDuration fromUnit(final long time, final TimeUnit unit) {

        if (unit == null) {

            throw new IllegalArgumentException();
        }

        return new TimeDuration(time, unit);
    }

    public static TimeDuration hours(final long hours) {

        return new TimeDuration(hours, TimeUnit.HOURS);
    }

    public static TimeDuration micros(final long micros) {

        return new TimeDuration(micros, TimeUnit.MICROSECONDS);
    }

    public static TimeDuration millis(final long millis) {

        return new TimeDuration(millis, TimeUnit.MILLISECONDS);
    }

    public static TimeDuration minutes(final long minutes) {

        return new TimeDuration(minutes, TimeUnit.MINUTES);
    }

    public static TimeDuration nanos(final long nanos) {

        return new TimeDuration(nanos, TimeUnit.NANOSECONDS);
    }

    public static TimeDuration seconds(final long seconds) {

        return new TimeDuration(seconds, TimeUnit.SECONDS);
    }

    public TimeDuration daysTime() {

        return days(toDays());
    }

    public TimeDuration hoursTime() {

        return hours(toHours());
    }

    public TimeDuration microsTime() {

        return micros(toMicros());
    }

    public TimeDuration millisTime() {

        return millis(toMillis());
    }

    public TimeDuration minutesTime() {

        return minutes(toMinutes());
    }

    public TimeDuration nanosTime() {

        return nanos(toNanos());
    }

    public TimeDuration secondsTime() {

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

    public boolean sleepSinceMillis(final long milliTime) throws InterruptedException {

        if (isZero()) {

            return false;
        }

        if (toDays() > MILLI_DAYS_OVERFLOW) {

            throw new IllegalStateException();
        }

        final long millisToSleep = milliTime - System.currentTimeMillis() + toMillis();

        if (millisToSleep <= 0) {

            return false;
        }

        TimeUnit.MILLISECONDS.sleep(millisToSleep);

        return true;
    }

    public boolean sleepSinceNanos(final long nanoTime) throws InterruptedException {

        if (isZero()) {

            return false;
        }

        if (toDays() > NANO_DAYS_OVERFLOW) {

            throw new IllegalStateException();
        }

        final long nanosToSleep = nanoTime - System.nanoTime() + toNanos();

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

    public interface Check {

        public boolean isTrue();
    }
}