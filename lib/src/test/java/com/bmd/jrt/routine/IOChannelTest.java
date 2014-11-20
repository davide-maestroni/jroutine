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
package com.bmd.jrt.routine;

import com.bmd.jrt.builder.RoutineBuilder.ChannelDataOrder;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * I/O channel unit tests.
 * <p/>
 * Created by davide on 10/26/14.
 */
public class IOChannelTest extends TestCase {

    public void testAbort() {

        final IOChannel<String> channel = JavaRoutine.io().buildChannel();
        final Routine<String, String> routine = JavaRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.runAsync(channel.output());

        channel.input().abort(new IllegalStateException());

        try {

            outputChannel.readFirst();

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    public void testAsynchronousInput() {

        final IOChannel<String> channel = JavaRoutine.io().buildChannel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    channel.input().pass("test").close();
                }
            }
        }.start();

        final Routine<String, String> routine = JavaRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.runAsync(channel.output());
        assertThat(outputChannel.readFirst()).isEqualTo("test");
        assertThat(outputChannel.isComplete()).isTrue();

        final IOChannel<String> channel1 =
                JavaRoutine.io().dataOrder(ChannelDataOrder.INSERTION).buildChannel();

        new Thread() {

            @Override
            public void run() {

                channel1.input()
                        .after(1, TimeUnit.MILLISECONDS)
                        .after(TimeDuration.millis(200))
                        .pass("test1", "test2")
                        .pass(Collections.singleton("test3"))
                        .close();
            }
        }.start();

        final Routine<String, String> routine1 = JavaRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel1 = routine1.runAsync(channel1.output());
        assertThat(outputChannel1.readAll()).containsExactly("test1", "test2", "test3");
    }

    public void testPartialOut() {

        final IOChannel<String> channel = JavaRoutine.io().buildChannel();

        new Thread() {

            @Override
            public void run() {

                channel.input().pass("test");
            }
        }.start();

        final long startTime = System.currentTimeMillis();

        final Routine<String, String> routine = JavaRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.runAsync(channel.output());
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).readAll()).containsExactly(
                "test");

        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        assertThat(outputChannel.immediately().isComplete()).isFalse();
        channel.input().close();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).isComplete()).isTrue();
    }

    @SuppressWarnings("UnusedAssignment")
    public void testWeak() throws InterruptedException {

        IOChannel<String> channel = JavaRoutine.io().buildChannel();

        new WeakThread(channel).start();

        final Routine<String, String> routine = JavaRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.runAsync(channel.output());
        assertThat(outputChannel.readFirst()).isEqualTo("test");

        channel = null;

        // this is not guaranteed to work, so let's try a few times...
        for (int i = 0; i < 3; i++) {

            System.gc();

            if (outputChannel.afterMax(TimeDuration.seconds(500)).isComplete()) {

                return;
            }
        }

        fail();
    }

    private static class WeakThread extends Thread {

        private final WeakReference<IOChannel<String>> mChannelRef;

        public WeakThread(final IOChannel<String> channel) {

            mChannelRef = new WeakReference<IOChannel<String>>(channel);
        }

        @Override
        public void run() {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException ignored) {

            } finally {

                final IOChannel<String> channel = mChannelRef.get();

                if (channel != null) {

                    channel.input().pass("test");
                }
            }
        }
    }
}
