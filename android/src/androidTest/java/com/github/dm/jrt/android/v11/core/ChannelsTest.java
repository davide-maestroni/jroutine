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
package com.github.dm.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;
import android.util.SparseArray;

import com.github.dm.jrt.android.core.Channels.ParcelableSelectable;
import com.github.dm.jrt.android.core.JRoutine;
import com.github.dm.jrt.android.invocation.FilterContextInvocation;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.builder.TransportChannelBuilder;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.TransportChannel;
import com.github.dm.jrt.routine.Routine;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nonnull;

import static com.github.dm.jrt.android.core.InvocationFactoryTarget.invocationOf;
import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ArrayList<ParcelableSelectable<Object>> outputs =
                new ArrayList<ParcelableSelectable<Object>>();
        outputs.add(new ParcelableSelectable<Object>("test21", Sort.STRING));
        outputs.add(new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine = JRoutine
                .with(serviceFrom(getActivity()))
                .on(invocationOf(Sort.class))
                .buildRoutine();
        SparseArray<TransportChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
    }

    public void testInputMapAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine = JRoutine
                .with(serviceFrom(getActivity()))
                .on(invocationOf(Sort.class))
                .buildRoutine();
        SparseArray<TransportChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).pass("test21").close();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testInputMapError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            Channels.mapParcelable(0, 0, JRoutine.with(serviceFrom(getActivity()))
                                                 .on(invocationOf(Sort.class))
                                                 .asyncInvoke());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testMerge() {

        final TransportChannelBuilder builder =
                JRoutine.transport().channels().withChannelOrder(OrderType.BY_CALL).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<Integer> channel2 = builder.buildChannel();
        final SparseArray<OutputChannel<?>> channelMap = new SparseArray<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                Channels.mergeParcelable(channelMap);
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.eventually().all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMergeAbort() {

        final TransportChannelBuilder builder =
                JRoutine.transport().channels().withChannelOrder(OrderType.BY_CALL).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<Integer> channel2 = builder.buildChannel();
        final SparseArray<OutputChannel<?>> channelMap = new SparseArray<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                Channels.mergeParcelable(channelMap);
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

            Channels.mergeParcelable(new SparseArray<OutputChannel<?>>(0));

            fail();

        } catch (final IllegalArgumentException ignored) {

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
