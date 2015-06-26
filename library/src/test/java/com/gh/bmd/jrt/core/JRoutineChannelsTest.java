/*
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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.TransportChannelBuilder;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportOutput;
import com.gh.bmd.jrt.core.JRoutineChannels.Selectable;
import com.gh.bmd.jrt.invocation.FilterInvocation;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.invocation.TemplateInvocation;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.invocation.Invocations.factoryOf;
import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine channels unit tests.
 * <p/>
 * Created by davide-maestroni on 3/18/15.
 */
public class JRoutineChannelsTest {

    @Test
    public void testCombine() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).invokeAsync().orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutine.on(PassingInvocation.<Integer>factoryOf()).invokeAsync().orderByCall();
        JRoutineChannels.combine(channel1, channel2)
                        .pass(new Selectable<Object>("test1", 0))
                        .pass(new Selectable<Object>(1, 1));
        JRoutineChannels.combine(3, channel1, channel2)
                        .pass(new Selectable<Object>("test2", 3))
                        .pass(new Selectable<Object>(2, 4));
        JRoutineChannels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                        .pass(new Selectable<Object>("test3", 0))
                        .pass(new Selectable<Object>(3, 1));
        JRoutineChannels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                        .pass(new Selectable<Object>("test4", -5))
                        .pass(new Selectable<Object>(4, -4));
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        JRoutineChannels.combine(map)
                        .pass(new Selectable<Object>("test5", 31))
                        .pass(new Selectable<Object>(5, 17));
        assertThat(channel1.result().eventually().all()).containsExactly("test1", "test2", "test3",
                                                                         "test4", "test5");
        assertThat(channel2.result().eventually().all()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testCombineAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<Integer, Integer> channel2;
        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).invokeAsync().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).invokeAsync().orderByCall();
        JRoutineChannels.combine(channel1, channel2).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).invokeAsync().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).invokeAsync().orderByCall();
        JRoutineChannels.combine(3, channel1, channel2).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).invokeAsync().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).invokeAsync().orderByCall();
        JRoutineChannels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                        .abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).invokeAsync().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).invokeAsync().orderByCall();
        JRoutineChannels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                        .abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).invokeAsync().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).invokeAsync().orderByCall();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        JRoutineChannels.combine(map).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testCombineError() {

        try {

            JRoutineChannels.combine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineChannels.combine(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineChannels.combine(Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineChannels.combine(0, Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineChannels.combine(Collections.<Integer, InputChannel<?>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMap() {

        final TransportChannelBuilder builder =
                JRoutine.transport().invocations().withOutputOrder(OrderType.BY_CALL).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends Selectable<Object>> channel = JRoutineChannels.merge(
                Arrays.<TransportOutput<?>>asList(channel1.output(), channel2.output()));
        final OutputChannel<Selectable<Object>> output = JRoutine.on(new Sort())
                                                                 .invocations()
                                                                 .withInputOrder(OrderType.BY_CALL)
                                                                 .set()
                                                                 .callAsync(channel);
        final Map<Integer, OutputChannel<Object>> channelMap =
                JRoutineChannels.map(output, Sort.INTEGER, Sort.STRING);

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.input().after(millis(20)).pass(input);
            channel2.input().after(millis(20)).pass(i);
        }

        channel1.input().close();
        channel2.input().close();

        assertThat(channelMap.get(Sort.STRING).eventually().all()).containsExactly("0", "1", "2",
                                                                                   "3");
        assertThat(channelMap.get(Sort.INTEGER).eventually().all()).containsExactly(0, 1, 2, 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMerge() {

        final TransportChannelBuilder builder =
                JRoutine.transport().invocations().withOutputOrder(OrderType.BY_CALL).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<String> channel2 = builder.buildChannel();
        final TransportChannel<String> channel3 = builder.buildChannel();
        final TransportChannel<String> channel4 = builder.buildChannel();

        final Routine<Selectable<String>, String> routine =
                JRoutine.on(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(JRoutineChannels.merge(
                Arrays.asList(channel1.output(), channel2.output(), channel3.output(),
                              channel4.output())));

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.input().after(millis(20)).pass(input);
            channel2.input().after(millis(20)).pass(input);
            channel3.input().after(millis(20)).pass(input);
            channel4.input().after(millis(20)).pass(input);
        }

        channel1.input().close();
        channel2.input().close();
        channel3.input().close();
        channel4.input().close();

        assertThat(outputChannel.eventually().all()).containsExactly("0", "1", "2", "3");
    }

    @Test
    public void testMergeError() {

        try {

            JRoutineChannels.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static class Amb<DATA> extends TemplateInvocation<Selectable<DATA>, DATA> {

        private static final int NO_INDEX = Integer.MIN_VALUE;

        private int mFirstIndex;

        @Override
        public void onInitialize() {

            mFirstIndex = NO_INDEX;
        }

        @Override
        public void onInput(final Selectable<DATA> input,
                @Nonnull final ResultChannel<DATA> result) {

            if (mFirstIndex == NO_INDEX) {

                mFirstIndex = input.index;
                result.pass(input.data);

            } else if (mFirstIndex == input.index) {

                result.pass(input.data);
            }
        }
    }

    private static class Sort extends FilterInvocation<Selectable<Object>, Selectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final Selectable<Object> selectable,
                @Nonnull final ResultChannel<Selectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    JRoutineChannels.<Object, Integer>select(result, INTEGER)
                                    .pass(selectable.<Integer>data());
                    break;

                case STRING:
                    JRoutineChannels.<Object, String>select(result, STRING)
                                    .pass(selectable.<String>data());
                    break;
            }
        }
    }
}
