/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.ext.channel;

import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.TemplateOutputConsumer;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.core.JRoutineCore;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Repeated channel unit tests.
 * <p/>
 * Created by davide-maestroni on 10/26/2014.
 */
public class RepeatedChannelTest {

    @Test
    public void testRepeat() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Channels.repeat(ioChannel).build();
        assertThat(channel.isBound()).isFalse();
        assertThat(channel.isEmpty()).isTrue();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.passTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.passTo(output2).close();
        assertThat(channel.isOpen()).isTrue();
        ioChannel.pass("test3").close();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        channel.passTo(output1);
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
        assertThat(channel.isEmpty()).isFalse();
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testRepeatAbort() {

        IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        OutputChannel<Object> channel = Channels.repeat(ioChannel).build();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.passTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.passTo(output2).close();
        ioChannel.abort();

        try {
            output1.all();
            fail();

        } catch (final AbortException ignored) {

        }

        try {
            output2.all();
            fail();

        } catch (final AbortException ignored) {

        }

        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.repeat(ioChannel).build();
        ioChannel.pass("test").close();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onOutput(final Object output) throws Exception {

                throw new IllegalAccessError();
            }
        });
        assertThat(channel.getError()).isNull();
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testRepeatAbortException() {

        IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        OutputChannel<Object> channel = Channels.repeat(ioChannel).build();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        ioChannel.abort();
        assertThat(channel.getError().getCause()).isNull();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.repeat(ioChannel).build();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new RoutineException();
            }
        });
        ioChannel.abort();
        assertThat(channel.getError().getCause()).isNull();

        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.repeat(ioChannel).build();
        ioChannel.abort();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        assertThat(channel.getError().getCause()).isNull();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.repeat(ioChannel).build();
        ioChannel.abort();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new RoutineException();
            }
        });
        assertThat(channel.getError().getCause()).isNull();
    }

    @Test
    public void testRepeatConsumer() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Channels.repeat(ioChannel).build();
        assertThat(channel.isBound()).isFalse();
        assertThat(channel.isEmpty()).isTrue();
        ioChannel.pass("test1", "test2");
        final ArrayList<Object> outputs = new ArrayList<Object>();
        final TemplateOutputConsumer<Object> consumer = new TemplateOutputConsumer<Object>() {

            @Override
            public void onOutput(final Object output) throws Exception {

                outputs.add(output);
            }
        };
        channel.passTo(consumer);
        assertThat(channel.isOpen()).isTrue();
        ioChannel.pass("test3").close();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        channel.passTo(consumer);
        assertThat(outputs).containsExactly("test1", "test2", "test3");
        assertThat(channel.isEmpty()).isFalse();
    }

    @Test
    public void testRepeatError() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Channels.repeat(ioChannel).build();
        channel.eventuallyExit();
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel.eventuallyAbort();
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel.eventuallyAbort(new NullPointerException());
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel.eventuallyThrow();
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel.immediately();
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel.afterMax(seconds(3));
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel.afterMax(3, TimeUnit.SECONDS);
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }
    }

    @Test
    public void testRepeatException() {

        IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        OutputChannel<Object> channel = Channels.repeat(ioChannel).build();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        ioChannel.pass("test").close().throwError();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.repeat(ioChannel).build();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new RoutineException();
            }
        });
        ioChannel.pass("test").close().throwError();

        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.repeat(ioChannel).build();
        ioChannel.pass("test").close();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        channel.throwError();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.repeat(ioChannel).build();
        ioChannel.pass("test").close();
        channel.passTo(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new RoutineException();
            }
        });
        channel.throwError();
    }
}
