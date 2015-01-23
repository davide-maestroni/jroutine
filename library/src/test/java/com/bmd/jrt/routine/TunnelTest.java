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

import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ReadDeadlockException;
import com.bmd.jrt.channel.Tunnel;
import com.bmd.jrt.channel.Tunnel.TunnelOutput;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tunnel unit tests.
 * <p/>
 * Created by davide on 10/26/14.
 */
public class TunnelTest extends TestCase {

    public void testAbort() {

        final TimeDuration timeout = seconds(1);

        final Tunnel<String> tunnel = JRoutine.io().buildTunnel();
        final Routine<String, String> routine = JRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(tunnel.output());

        tunnel.input().abort(new IllegalStateException());

        try {

            outputChannel.afterMax(timeout).readNext();

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    public void testAbortDelay() {

        final Tunnel<String> tunnel = JRoutine.io().buildTunnel();
        tunnel.input().after(TimeDuration.days(1)).pass("test").close();

        final TunnelOutput<String> output = tunnel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        final ArrayList<String> results = new ArrayList<String>();
        output.afterMax(10, TimeUnit.MILLISECONDS).readAllInto(results);
        assertThat(results).isEmpty();
        assertThat(output.immediately().eventuallyExit().checkComplete()).isFalse();
        assertThat(output.abort()).isTrue();

        try {

            output.readNext();

            fail();

        } catch (final RoutineException ignored) {

        }

        assertThat(output.isOpen()).isFalse();
    }

    public void testAsynchronousInput() {

        final TimeDuration timeout = seconds(1);

        final Tunnel<String> tunnel = JRoutine.io().buildTunnel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    tunnel.input().pass("test").close();
                }
            }
        }.start();

        final Routine<String, String> routine = JRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(tunnel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
        assertThat(outputChannel.checkComplete()).isTrue();

        final Tunnel<String> tunnel1 = JRoutine.io().dataOrder(DataOrder.INSERTION).buildTunnel();

        new Thread() {

            @Override
            public void run() {

                tunnel1.input()
                       .after(1, TimeUnit.MILLISECONDS)
                       .after(TimeDuration.millis(200))
                       .pass("test1", "test2")
                       .pass(Collections.singleton("test3"))
                       .close();
            }
        }.start();

        final Routine<String, String> routine1 = JRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel1 = routine1.callAsync(tunnel1.output());
        assertThat(outputChannel1.afterMax(timeout).readAll()).containsExactly("test1", "test2",
                                                                               "test3");
    }

    public void testPartialOut() {

        final Tunnel<String> tunnel = JRoutine.io().buildTunnel();

        new Thread() {

            @Override
            public void run() {

                tunnel.input().pass("test");
            }
        }.start();

        final long startTime = System.currentTimeMillis();

        final Routine<String, String> routine = JRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel =
                routine.callAsync(tunnel.output()).eventuallyExit();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).readAll()).containsExactly(
                "test");

        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        assertThat(outputChannel.immediately().checkComplete()).isFalse();
        tunnel.input().close();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).checkComplete()).isTrue();
    }

    public void testReadFirst() throws InterruptedException {

        final TimeDuration timeout = seconds(1);

        final Tunnel<String> tunnel = JRoutine.io().buildTunnel();

        new WeakThread(tunnel).start();

        final Routine<String, String> routine = JRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(tunnel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
    }

    public void testTimeout() {

        final Tunnel<String> tunnel = JRoutine.io().buildTunnel();
        tunnel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TunnelOutput<String> output = tunnel.output();
        assertThat(output.immediately().readAll()).isEmpty();

        output.afterMax(TimeDuration.millis(10)).eventuallyDeadlock();

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

        private final WeakReference<Tunnel<String>> mTunnelRef;

        public WeakThread(final Tunnel<String> tunnel) {

            mTunnelRef = new WeakReference<Tunnel<String>>(tunnel);
        }

        @Override
        public void run() {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException ignored) {

            } finally {

                final Tunnel<String> tunnel = mTunnelRef.get();

                if (tunnel != null) {

                    tunnel.input().pass("test");
                }
            }
        }
    }
}
