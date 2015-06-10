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
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.core.JRoutineChannels.Selectable;
import com.gh.bmd.jrt.invocation.TemplateInvocation;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
    public void testEmpty() {

        try {

            JRoutineChannels.selectFrom(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSelect() {

        final TransportChannelBuilder builder =
                JRoutine.transport().withInvocation().withOutputOrder(OrderType.PASS_ORDER).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<String> channel2 = builder.buildChannel();
        final TransportChannel<String> channel3 = builder.buildChannel();
        final TransportChannel<String> channel4 = builder.buildChannel();

        final Routine<Selectable<String>, String> routine =
                JRoutine.on(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(JRoutineChannels.selectFrom(
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

    private static class Amb<DATA> extends TemplateInvocation<Selectable<DATA>, DATA> {

        private int mFirstIndex;

        @Override
        public void onInitialize() {

            mFirstIndex = -1;
        }

        @Override
        public void onInput(final Selectable<DATA> input,
                @Nonnull final ResultChannel<DATA> result) {

            if (mFirstIndex < 0) {

                mFirstIndex = input.index;
                result.pass(input.data);

            } else if (mFirstIndex == input.index) {

                result.pass(input.data);
            }
        }
    }
}
