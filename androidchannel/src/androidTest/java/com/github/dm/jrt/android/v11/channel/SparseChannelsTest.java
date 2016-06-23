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

package com.github.dm.jrt.android.v11.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;
import android.util.SparseArray;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 08/03/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class SparseChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public SparseChannelsTest() {

        super(TestActivity.class);
    }

    @NotNull
    private static <T> Map<Integer, Channel<?, T>> toMap(
            @NotNull final SparseArray<Channel<?, T>> sparseArray) {

        final int size = sparseArray.size();
        final HashMap<Integer, Channel<?, T>> map = new HashMap<Integer, Channel<?, T>>(size);
        for (int i = 0; i < size; ++i) {
            map.put(sparseArray.keyAt(i), sparseArray.valueAt(i));
        }

        return map;
    }

    public void testCombine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Channel<String, String> channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                               .with(factoryOf(PassingString.class))
                                                               .async()
                                                               .sortedByCall();
        final Channel<Integer, Integer> channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                                 .with(factoryOf(
                                                                         PassingInteger.class))
                                                                 .async()
                                                                 .sortedByCall();
        SparseChannels.combine(channel1, channel2)
                      .buildChannels()
                      .pass(new ParcelableSelectable<String>("test1", 0))
                      .pass(new ParcelableSelectable<Integer>(1, 1))
                      .close();
        SparseChannels.combine(3, channel1, channel2)
                      .buildChannels()
                      .pass(new ParcelableSelectable<String>("test2", 3))
                      .pass(new ParcelableSelectable<Integer>(2, 4))
                      .close();
        SparseChannels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                      .buildChannels()
                      .pass(new ParcelableSelectable<String>("test3", 0))
                      .pass(new ParcelableSelectable<Integer>(3, 1))
                      .close();
        SparseChannels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                      .buildChannels()
                      .pass(new ParcelableSelectable<String>("test4", -5))
                      .pass(new ParcelableSelectable<Integer>(4, -4))
                      .close();
        final SparseArray<Channel<?, ?>> map = new SparseArray<Channel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        SparseChannels.combine(map)
                      .buildChannels()
                      .pass(new ParcelableSelectable<String>("test5", 31))
                      .pass(new ParcelableSelectable<Integer>(5, 17))
                      .close();
        assertThat(channel1.close().after(seconds(10)).all()).containsExactly("test1", "test2",
                "test3", "test4", "test5");
        assertThat(channel2.close().after(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
    }

    public void testCombineAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingString.class))
                                 .async()
                                 .sortedByCall();
        channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingInteger.class))
                                 .async()
                                 .sortedByCall();
        SparseChannels.combine(channel1, channel2).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingString.class))
                                 .async()
                                 .sortedByCall();
        channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingInteger.class))
                                 .async()
                                 .sortedByCall();
        SparseChannels.combine(3, channel1, channel2).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingString.class))
                                 .async()
                                 .sortedByCall();
        channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingInteger.class))
                                 .async()
                                 .sortedByCall();
        SparseChannels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                      .buildChannels()
                      .abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingString.class))
                                 .async()
                                 .sortedByCall();
        channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingInteger.class))
                                 .async()
                                 .sortedByCall();
        SparseChannels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                      .buildChannels()
                      .abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingString.class))
                                 .async()
                                 .sortedByCall();
        channel2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(PassingInteger.class))
                                 .async()
                                 .sortedByCall();
        final SparseArray<Channel<?, ?>> map = new SparseArray<Channel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        SparseChannels.combine(map).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testCombineError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            SparseChannels.combine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannels.combine(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannels.combine(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannels.combine(0, Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannels.combine(new SparseArray<Channel<?, ?>>(0));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testConstructor() {

        boolean failed = false;
        try {
            new SparseChannels();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    public void testInputMap() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ArrayList<ParcelableSelectable<Object>> outputs =
                new ArrayList<ParcelableSelectable<Object>>();
        outputs.add(new ParcelableSelectable<Object>("test21", Sort.STRING));
        outputs.add(new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(factoryOf(Sort.class))
                              .buildRoutine();
        SparseArray<Channel<Object, ?>> channelMap;
        Channel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.async();
        channelMap = SparseChannels.selectParcelableInput(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.close().after(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.async();
        channelMap = SparseChannels.selectParcelableInput(channel, Sort.INTEGER, Sort.STRING)
                                   .buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.close().after(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.async();
        channelMap = SparseChannels.selectParcelableInput(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.close().after(seconds(10)).all()).containsOnlyElementsOf(outputs);
    }

    public void testInputMapAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(factoryOf(Sort.class))
                              .buildRoutine();
        SparseArray<Channel<Object, ?>> channelMap;
        Channel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.async();
        channelMap = SparseChannels.selectParcelableInput(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.close().after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.async();
        channelMap = SparseChannels.selectParcelableInput(channel, Sort.INTEGER, Sort.STRING)
                                   .buildChannels();
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).pass("test21").close();

        try {

            channel.close().after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.async();
        channelMap = SparseChannels.selectParcelableInput(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.close().after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testInputMapError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            SparseChannels.selectParcelableInput(0, 0, JRoutineLoader.on(loaderFrom(getActivity()))
                                                                     .with(factoryOf(Sort.class))
                                                                     .async());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMap() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).applied();
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<Integer, Integer> channel2 = builder.buildChannel();

        final Channel<?, ? extends ParcelableSelectable<Object>> channel =
                SparseChannels.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                              .buildChannels();
        final Channel<?, ParcelableSelectable<Object>> output =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(factoryOf(Sort.class))
                              .invocationConfiguration()
                              .withInputOrder(OrderType.BY_CALL)
                              .applied()
                              .async(channel);
        final SparseArray<Channel<?, Object>> channelMap =
                SparseChannels.selectParcelableOutput(output, Sort.INTEGER, Sort.STRING)
                              .buildChannels();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsExactly("0", "1",
                "2", "3");
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsExactly(0, 1, 2,
                3);
    }

    @SuppressWarnings("unchecked")
    public void testMerge() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).applied();
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<Integer, Integer> channel2 = builder.buildChannel();
        final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final Channel<?, ? extends ParcelableSelectable<?>> outputChannel =
                SparseChannels.merge(channelMap).buildChannels();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMergeAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).applied();
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<Integer, Integer> channel2 = builder.buildChannel();
        final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final Channel<?, ? extends ParcelableSelectable<?>> outputChannel =
                SparseChannels.merge(channelMap).buildChannels();
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            SparseChannels.merge(new SparseArray<Channel<?, ?>>(0));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputMap() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(factoryOf(Sort.class))
                              .buildRoutine();
        SparseArray<Channel<?, Object>> channelMap;
        Channel<?, ParcelableSelectable<Object>> channel;
        channel = routine.async(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannels.selectParcelableOutput(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsOnly("test21");
        channel = routine.async(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap = SparseChannels.selectParcelableOutput(channel, Sort.INTEGER, Sort.STRING)
                                   .buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsOnly("test21");
        channel = routine.async(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannels.selectParcelableOutput(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsOnly("test21");
    }

    @SuppressWarnings("unchecked")
    public void testOutputMapAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(factoryOf(Sort.class))
                              .buildRoutine();
        SparseArray<Channel<?, Object>> channelMap;
        Channel<?, ParcelableSelectable<Object>> channel;
        channel = routine.async()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                 new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannels.selectParcelableOutput(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.async()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                                 new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap = SparseChannels.selectParcelableOutput(channel, Sort.INTEGER, Sort.STRING)
                                   .buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.async()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                 new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannels.selectParcelableOutput(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testOutputMapCache() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(factoryOf(Sort.class))
                              .buildRoutine();
        final Channel<?, ParcelableSelectable<Object>> channel = routine.async();
        final SparseArray<Channel<?, Object>> channelMap =
                SparseChannels.selectParcelableOutput(channel,
                        Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        assertThat(toMap(channelMap)).isEqualTo(
                toMap(SparseChannels.selectParcelableOutput(channel, Sort.INTEGER, Sort.STRING)
                                    .buildChannels()));
    }

    public void testOutputMapError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            SparseChannels.selectParcelableOutput(0, 0, JRoutineLoader.on(loaderFrom(getActivity()))
                                                                      .with(factoryOf(Sort.class))
                                                                      .async());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputSelect() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Channel<ParcelableSelectable<String>, ParcelableSelectable<String>> channel =
                JRoutineCore.io().buildChannel();
        final Channel<?, String> outputChannel =
                SparseChannels.selectParcelableOutput(channel, 33).buildChannels().get(33);
        channel.pass(new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", -33),
                new ParcelableSelectable<String>("test3", 33),
                new ParcelableSelectable<String>("test4", 333));
        channel.close();
        assertThat(outputChannel.after(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testOutputSelectAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Channel<ParcelableSelectable<String>, ParcelableSelectable<String>> channel =
                JRoutineCore.io().buildChannel();
        final Channel<?, String> outputChannel =
                SparseChannels.selectParcelableOutput(channel, 33).buildChannels().get(33);
        channel.abort();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    private static class PassingInteger extends TemplateContextInvocation<Integer, Integer> {

        public void onInput(final Integer i, @NotNull final Channel<Integer, ?> result) {

            result.pass(i);
        }
    }

    private static class PassingString extends TemplateContextInvocation<String, String> {

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {

            result.pass(s);
        }
    }

    private static class Sort extends
            TemplateContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final Channel<ParcelableSelectable<Object>, ?> result) {

            switch (selectable.index) {

                case INTEGER:
                    SparseChannels.<Object, Integer>selectParcelableInput(result,
                            INTEGER).buildChannels().pass((Integer) selectable.data).close();
                    break;

                case STRING:
                    SparseChannels.<Object, String>selectParcelableInput(result,
                            STRING).buildChannels().pass((String) selectable.data).close();
                    break;
            }
        }
    }
}
