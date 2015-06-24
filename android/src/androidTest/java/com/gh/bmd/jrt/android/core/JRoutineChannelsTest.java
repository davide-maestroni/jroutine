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
package com.gh.bmd.jrt.android.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.core.JRoutineChannels.ParcelableSelectable;
import com.gh.bmd.jrt.android.invocation.FilterContextInvocation;
import com.gh.bmd.jrt.android.invocation.TemplateContextInvocation;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.TransportChannelBuilder;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportOutput;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p/>
 * Created by davide-maestroni on 6/18/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class JRoutineChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public JRoutineChannelsTest() {

        super(TestActivity.class);
    }

    public void testEmpty() {

        try {

            JRoutineChannels.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testSelect() {

        final TransportChannelBuilder builder =
                JRoutine.transport().invocations().withOutputOrder(OrderType.PASS_ORDER).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<String> channel2 = builder.buildChannel();
        final TransportChannel<String> channel3 = builder.buildChannel();
        final TransportChannel<String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutine.onService(getActivity(), new ClassToken<Amb<String>>() {}).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync(
                JRoutineChannels.mergeParcelable(
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

    public void testSorting() {

        final TransportChannelBuilder builder =
                JRoutine.transport().invocations().withOutputOrder(OrderType.PASS_ORDER).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                JRoutineChannels.mergeParcelable(
                        Arrays.<TransportOutput<?>>asList(channel1.output(), channel2.output()));
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(Sort.class))
                        .invocations()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withOutputOrder(OrderType.PASS_ORDER)
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

    private static class Amb<DATA>
            extends TemplateContextInvocation<ParcelableSelectable<DATA>, DATA> {

        private static final int NO_INDEX = Integer.MIN_VALUE;

        private int mFirstIndex;

        @Override
        public void onInitialize() {

            mFirstIndex = NO_INDEX;
        }

        @Override
        public void onInput(final ParcelableSelectable<DATA> input,
                @Nonnull final ResultChannel<DATA> result) {

            if (mFirstIndex == NO_INDEX) {

                mFirstIndex = input.index;
                result.pass(input.data);

            } else if (mFirstIndex == input.index) {

                result.pass(input.data);
            }
        }
    }

    private static class Sort extends
            FilterContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final ParcelableSelectable<Object> selectable,
                @Nonnull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    JRoutineChannels.<Object, Integer>selectParcelable(result, INTEGER)
                                    .pass((Integer) selectable.data);
                    break;

                case STRING:
                    JRoutineChannels.<Object, String>selectParcelable(result, STRING)
                                    .pass((String) selectable.data);
                    break;
            }
        }
    }
}
