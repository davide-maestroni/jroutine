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
package com.gh.bmd.jrt.channel;

import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.StandaloneChannelBuilder;
import com.gh.bmd.jrt.channel.Channels.Selectable;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.invocation.TemplateInvocation;
import com.gh.bmd.jrt.routine.JRoutine;
import com.gh.bmd.jrt.routine.Routine;

import org.junit.Test;

import java.util.Arrays;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.withOutputOrder;
import static com.gh.bmd.jrt.time.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Channels unit tests.
 * <p/>
 * Created by davide on 3/18/15.
 */
public class ChannelsTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testSelect() {

        final StandaloneChannelBuilder builder =
                JRoutine.standalone().withConfiguration(withOutputOrder(OrderType.PASSING_ORDER));
        final StandaloneChannel<String> channel1 = builder.buildChannel();
        final StandaloneChannel<String> channel2 = builder.buildChannel();
        final StandaloneChannel<String> channel3 = builder.buildChannel();
        final StandaloneChannel<String> channel4 = builder.buildChannel();

        final Routine<Selectable<String>, String> routine =
                JRoutine.on(new ClassToken<Amb<String>>() {}).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(Channels.select(
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

        assertThat(outputChannel.eventually().readAll()).containsExactly("0", "1", "2", "3");
    }

    private static class Amb<DATA> extends TemplateInvocation<Selectable<DATA>, DATA> {

        private int mFirstIndex;

        @Override
        public void onInit() {

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
