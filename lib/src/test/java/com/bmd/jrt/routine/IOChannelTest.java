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

import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * I/O channel unit tests.
 * <p/>
 * Created by davide on 10/26/14.
 */
public class IOChannelTest extends TestCase {

    public void testAsynchronousInput() {

        final IOChannel<String> channel = JavaRoutine.io().buildChannel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    channel.input().pass("test");
                    channel.close();
                }
            }
        }.start();

        final Routine<String, String> routine = JavaRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.runAsync(channel.output());
        assertThat(outputChannel.readFirst()).isEqualTo("test");
        assertThat(outputChannel.isComplete()).isTrue();
    }

    public void testMaxAge() {

        final IOChannel<String> channel =
                JavaRoutine.io().withMaxAge(1, TimeUnit.SECONDS).buildChannel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    channel.input().pass("test");
                }
            }
        }.start();

        final Routine<String, String> routine = JavaRoutine.<String>on().buildRoutine();
        final OutputChannel<String> outputChannel = routine.runAsync(channel.output());
        assertThat(outputChannel.readFirst()).isEqualTo("test");
        assertThat(outputChannel.afterMax(TimeDuration.seconds(1)).isComplete()).isTrue();
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
        channel.close();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).isComplete()).isTrue();
    }
}