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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.TemplateOutputConsumer;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.days;
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
    public void testChannel() {

        Channel<?, String> channel = Channels.replay(JRoutineCore.io().of("test")).buildChannels();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.abort()).isFalse();
        assertThat(channel.abort(null)).isFalse();
        assertThat(channel.close().isOpen()).isFalse();
        assertThat(channel.isEmpty()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        assertThat(channel.isBound()).isFalse();
        final ArrayList<String> results = new ArrayList<String>();
        assertThat(channel.after(1, TimeUnit.SECONDS).hasNext()).isTrue();
        channel.immediately().allInto(results);
        assertThat(results).containsExactly("test");
        channel = Channels.replay(JRoutineCore.io().of("test1", "test2", "test3")).buildChannels();

        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        assertThat(channel.skipNext(1).next(1)).containsExactly("test2");
        assertThat(channel.eventuallyBreak().next(4)).containsExactly("test3");
        assertThat(channel.eventuallyBreak().nextOrElse("test4")).isEqualTo("test4");

        Iterator<String> iterator = Channels.replay(JRoutineCore.io().of("test1", "test2", "test3"))
                                            .buildChannels()
                                            .iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("test1");

        try {
            iterator.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        iterator = Channels.replay(JRoutineCore.io().of("test1", "test2", "test3"))
                           .buildChannels()
                           .eventualIterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("test1");

        try {
            iterator.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel = Channels.replay(
                JRoutineCore.io().<String>buildChannel().after(days(1)).pass("test"))
                          .buildChannels();

        try {
            channel.eventuallyFail().next();
            fail();

        } catch (final TimeoutException ignored) {

        }

        try {
            channel.eventuallyBreak().next();
            fail();

        } catch (final NoSuchElementException ignored) {

        }

        try {
            channel.eventuallyAbort().next();
            fail();

        } catch (final AbortException ignored) {

        }

        try {
            channel.eventuallyAbort(new IllegalArgumentException()).next();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isNull();
        }

        channel = Channels.replay(
                JRoutineCore.io().<String>buildChannel().after(seconds(1)).pass("test"))
                          .buildChannels();

        try {
            channel.eventuallyAbort(new IllegalArgumentException()).next();
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvalidCalls() {
        final Channel<String, String> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<Object, String> channel =
                (Channel<Object, String>) Channels.replay(inputChannel).buildChannels();
        try {
            channel.sortedByCall().pass("test");
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            channel.sortedByDelay().pass("test", "test");
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            channel.pass(Collections.singleton("test"));
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            channel.pass(JRoutineCore.io().buildChannel());
            fail();

        } catch (final IllegalStateException ignored) {
        }
    }

    @Test
    public void testReplay() {

        final Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = Channels.replay(inputChannel).buildChannels();
        assertThat(channel.isBound()).isFalse();
        assertThat(channel.isEmpty()).isTrue();
        inputChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        assertThat(channel.isOpen()).isFalse();
        channel.close();
        assertThat(channel.isOpen()).isFalse();
        inputChannel.pass("test3").close();
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

        Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        Channel<?, Object> channel = Channels.replay(inputChannel).buildChannels();
        inputChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        inputChannel.abort();

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

        inputChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(inputChannel).buildChannels();
        inputChannel.pass("test").close();
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

        Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        Channel<?, Object> channel = Channels.replay(inputChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        inputChannel.abort();
        assertThat(channel.getError().getCause()).isNull();
        inputChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(inputChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new RoutineException();
            }
        });
        inputChannel.abort();
        assertThat(channel.getError().getCause()).isNull();

        inputChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(inputChannel).buildChannels();
        inputChannel.abort();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        assertThat(channel.getError().getCause()).isNull();
        inputChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(inputChannel).buildChannels();
        inputChannel.abort();
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

        final Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = Channels.replay(inputChannel).buildChannels();
        assertThat(channel.isBound()).isFalse();
        assertThat(channel.isEmpty()).isTrue();
        inputChannel.pass("test1", "test2");
        final ArrayList<Object> outputs = new ArrayList<Object>();
        final TemplateOutputConsumer<Object> consumer = new TemplateOutputConsumer<Object>() {

            @Override
            public void onOutput(final Object output) throws Exception {

                outputs.add(output);
            }
        };
        channel.bind(consumer);
        assertThat(channel.isOpen()).isFalse();
        inputChannel.pass("test3").close();
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.hasCompleted()).isTrue();
        channel.bind(consumer);
        assertThat(outputs).containsExactly("test1", "test2", "test3");
        assertThat(channel.isEmpty()).isFalse();
    }

    @Test
    public void testReplayError() {

        final Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = Channels.replay(inputChannel).buildChannels();
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

        channel.eventuallyFail();
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

        channel.after(seconds(3));
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        channel.after(3, TimeUnit.SECONDS);
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }
    }

    @Test
    public void testReplayException() {

        Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        Channel<?, Object> channel = Channels.replay(inputChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        inputChannel.pass("test").close().throwError();
        inputChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(inputChannel).buildChannels();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new RoutineException();
            }
        });
        inputChannel.pass("test").close().throwError();

        inputChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(inputChannel).buildChannels();
        inputChannel.pass("test").close();
        channel.bind(new TemplateOutputConsumer<Object>() {

            @Override
            public void onComplete() throws Exception {

                throw new UnsupportedOperationException();
            }
        });
        channel.throwError();
        inputChannel = JRoutineCore.io().buildChannel();
        channel = Channels.replay(inputChannel).buildChannels();
        inputChannel.pass("test").close();
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

        final Channel<Object, Object> channel =
                JRoutineCore.on(IdentityInvocation.factoryOf()).async();
        assertThat(channel.inputCount()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.inputCount()).isEqualTo(1);
        assertThat(channel.outputCount()).isEqualTo(0);
        final Channel<?, Object> result = Channels.replay(channel.close()).buildChannels();
        assertThat(result.after(seconds(1)).hasCompleted()).isTrue();
        assertThat(result.inputCount()).isEqualTo(0);
        assertThat(result.outputCount()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.skipNext(1).outputCount()).isEqualTo(0);
    }
}
