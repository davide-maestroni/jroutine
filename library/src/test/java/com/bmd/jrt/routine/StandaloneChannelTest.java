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

import com.bmd.jrt.builder.RoutineChannelBuilder.OrderBy;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ReadDeadlockException;
import com.bmd.jrt.channel.StandaloneChannel;
import com.bmd.jrt.channel.StandaloneChannel.StandaloneOutput;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.invocation.PassingInvocation;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Standalone channel unit tests.
 * <p/>
 * Created by davide on 10/26/14.
 */
public class StandaloneChannelTest extends TestCase {

    public void testAbort() {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<String> standaloneChannel = JRoutine.on().buildChannel();
        final Routine<String, String> routine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(standaloneChannel.output());

        standaloneChannel.input().abort(new IllegalStateException());

        try {

            outputChannel.afterMax(timeout).readNext();

            fail();

        } catch (final InvocationException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    public void testAbortDelay() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.on().buildChannel();
        standaloneChannel.input().after(TimeDuration.days(1)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        final ArrayList<String> results = new ArrayList<String>();
        output.afterMax(10, TimeUnit.MILLISECONDS).readAllInto(results);
        assertThat(results).isEmpty();
        assertThat(output.immediately().eventuallyExit().checkComplete()).isFalse();
        assertThat(output.abort()).isTrue();

        try {

            output.readNext();

            fail();

        } catch (final InvocationException ignored) {

        }

        assertThat(output.isOpen()).isFalse();
    }

    public void testAsynchronousInput() {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<String> standaloneChannel = JRoutine.on().buildChannel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    standaloneChannel.input().pass("test").close();
                }
            }
        }.start();

        final Routine<String, String> routine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(standaloneChannel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
        assertThat(outputChannel.checkComplete()).isTrue();

        final StandaloneChannel<String> standaloneChannel1 =
                JRoutine.on().withDataOrder(OrderBy.INSERTION).buildChannel();

        new Thread() {

            @Override
            public void run() {

                standaloneChannel1.input()
                                  .after(1, TimeUnit.MILLISECONDS)
                                  .after(TimeDuration.millis(200))
                                  .pass("test1", "test2")
                                  .pass(Collections.singleton("test3"))
                                  .close();
            }
        }.start();

        final Routine<String, String> routine1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();
        final OutputChannel<String> outputChannel1 =
                routine1.callAsync(standaloneChannel1.output());
        assertThat(outputChannel1.afterMax(timeout).readAll()).containsExactly("test1", "test2",
                                                                               "test3");
    }

    public void testPartialOut() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.on().buildChannel();

        new Thread() {

            @Override
            public void run() {

                standaloneChannel.input().pass("test");
            }
        }.start();

        final long startTime = System.currentTimeMillis();

        final Routine<String, String> routine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();
        final OutputChannel<String> outputChannel =
                routine.callAsync(standaloneChannel.output()).eventuallyExit();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).readAll()).containsExactly(
                "test");

        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        assertThat(outputChannel.immediately().checkComplete()).isFalse();
        standaloneChannel.input().close();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).checkComplete()).isTrue();
    }

    public void testReadFirst() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<String> standaloneChannel = JRoutine.on().buildChannel();

        new WeakThread(standaloneChannel).start();

        final Routine<String, String> routine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(standaloneChannel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
    }

    public void testTimeout() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.on().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock();

        try {

            output.readNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.readAllInto(new ArrayList<String>());

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.iterator().hasNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.iterator().next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        output.afterMax(TimeDuration.millis(10));

        try {

            output.readNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.readAllInto(new ArrayList<String>());

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.iterator().hasNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            output.iterator().next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    private static class WeakThread extends Thread {

        private final WeakReference<StandaloneChannel<String>> mChannelRef;

        public WeakThread(final StandaloneChannel<String> standaloneChannel) {

            mChannelRef = new WeakReference<StandaloneChannel<String>>(standaloneChannel);
        }

        @Override
        public void run() {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException ignored) {

            } finally {

                final StandaloneChannel<String> standaloneChannel = mChannelRef.get();

                if (standaloneChannel != null) {

                    standaloneChannel.input().pass("test");
                }
            }
        }
    }
}
