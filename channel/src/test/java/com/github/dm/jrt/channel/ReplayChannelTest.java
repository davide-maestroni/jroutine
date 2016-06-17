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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.TemplateOutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Replay channel unit tests.
 * <p>
 * Created by davide-maestroni on 10/26/2014.
 */
public class ReplayChannelTest {

    @Test
    public void testReplay() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Channels.replay(ioChannel).buildChannels();
        assertThat(channel.isBound()).isFalse();
        assertThat(channel.isEmpty()).isTrue();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        assertThat(channel.isOpen()).isTrue();
        ioChannel.pass("test3").close();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        channel.bind(output1);
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
        assertThat(channel.isEmpty()).isFalse();
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testReplayAbort() {

        IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        OutputChannel<Object> channel = Channels.replay(ioChannel).buildChannels();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
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
        channel = Channels.replay(ioChannel).buildChannels();
        ioChannel.pass("test").close();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onOutput(final Object output) throws Exception {

                throw new IllegalAccessError();
            }
        });
        assertThat(channel.getError()).isNull();
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testReplayAbortException() {

        IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        OutputChannel<Object> channel = Channels.replay(ioChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        ioChannel.abort();
        assertThat(channel.getError().getCause()).isNull();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(ioChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new RoutineException();
            }
        });
        ioChannel.abort();
        assertThat(channel.getError().getCause()).isNull();

        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(ioChannel).buildChannels();
        ioChannel.abort();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        assertThat(channel.getError().getCause()).isNull();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(ioChannel).buildChannels();
        ioChannel.abort();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new RoutineException();
            }
        });
        assertThat(channel.getError().getCause()).isNull();
    }

    @Test
    public void testReplayConsumer() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Channels.replay(ioChannel).buildChannels();
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
        channel.bind(consumer);
        assertThat(channel.isOpen()).isTrue();
        ioChannel.pass("test3").close();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        channel.bind(consumer);
        assertThat(outputs).containsExactly("test1", "test2", "test3");
        assertThat(channel.isEmpty()).isFalse();
    }

    @Test
    public void testReplayError() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = Channels.replay(ioChannel).buildChannels();
        channel.eventuallyBreak();
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
    public void testReplayException() {

        IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        OutputChannel<Object> channel = Channels.replay(ioChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        ioChannel.pass("test").close().throwError();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(ioChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new RoutineException();
            }
        });
        ioChannel.pass("test").close().throwError();

        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(ioChannel).buildChannels();
        ioChannel.pass("test").close();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        channel.throwError();
        ioChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(ioChannel).buildChannels();
        ioChannel.pass("test").close();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new RoutineException();
            }
        });
        channel.throwError();
    }

    @Test
    public void testSize() {

        final InvocationChannel<Object, Object> channel =
                JRoutineCore.on(IdentityInvocation.factoryOf()).asyncInvoke();
        assertThat(channel.size()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.size()).isEqualTo(1);
        final OutputChannel<Object> result = Channels.replay(channel.result()).buildChannels();
        assertThat(result.afterMax(seconds(1)).hasCompleted()).isTrue();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.skipNext(1).size()).isEqualTo(0);
    }
}
