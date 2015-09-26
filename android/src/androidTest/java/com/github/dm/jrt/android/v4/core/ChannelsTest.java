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
package com.github.dm.jrt.android.v4.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.support.v4.util.SparseArrayCompat;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.Channels.ParcelableSelectable;
import com.github.dm.jrt.android.core.JRoutine;
import com.github.dm.jrt.android.invocation.FilterContextInvocation;
import com.github.dm.jrt.builder.IOChannelBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.TargetInvocationFactory.factoryOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p/>
 * Created by davide-maestroni on 08/03/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ChannelsTest() {

        super(TestActivity.class);
    }

    public void testInputMap() {

        final ArrayList<ParcelableSelectable<Object>> outputs =
                new ArrayList<ParcelableSelectable<Object>>();
        outputs.add(new ParcelableSelectable<Object>("test21", Sort.STRING));
        outputs.add(new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutine.with(serviceFrom(getActivity())).on(factoryOf(Sort.class)).buildRoutine();
        SparseArrayCompat<IOChannel<Object, Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.spread(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.spread(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.spread(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
    }

    public void testInputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutine.with(serviceFrom(getActivity())).on(factoryOf(Sort.class)).buildRoutine();
        SparseArrayCompat<IOChannel<Object, Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.spread(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.spread(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).pass("test21").close();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.spread(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testInputMapError() {

        try {

            Channels.spread(0, 0, JRoutine.with(serviceFrom(getActivity()))
                                          .on(factoryOf(Sort.class))
                                          .asyncInvoke());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testMerge() {

        final IOChannelBuilder builder =
                JRoutine.io().channels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String, String> channel1 = builder.buildChannel();
        final IOChannel<Integer, Integer> channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                Channels.merge(channelMap);
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.eventually().all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMergeAbort() {

        final IOChannelBuilder builder =
                JRoutine.io().channels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String, String> channel1 = builder.buildChannel();
        final IOChannel<Integer, Integer> channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                Channels.merge(channelMap);
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        try {

            Channels.merge(new SparseArrayCompat<OutputChannel<?>>(0));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static class Sort extends
            FilterContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    Channels.<Object, Integer>selectParcelable(result, INTEGER)
                            .pass((Integer) selectable.data)
                            .close();
                    break;

                case STRING:
                    Channels.<Object, String>selectParcelable(result, STRING)
                            .pass((String) selectable.data)
                            .close();
                    break;
            }
        }
    }
}
