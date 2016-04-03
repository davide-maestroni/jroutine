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

package com.github.dm.jrt.android.v4.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.support.v4.util.SparseArrayCompat;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.IOChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 08/03/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class SparseChannelsCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public SparseChannelsCompatTest() {

        super(TestActivity.class);
    }

    @NotNull
    private static <T> Map<Integer, OutputChannel<T>> toMap(
            @NotNull final SparseArrayCompat<OutputChannel<T>> sparseArray) {

        final int size = sparseArray.size();
        final HashMap<Integer, OutputChannel<T>> map = new HashMap<Integer, OutputChannel<T>>(size);
        for (int i = 0; i < size; ++i) {
            map.put(sparseArray.keyAt(i), sparseArray.valueAt(i));
        }

        return map;
    }

    public void testCombine() {

        final InvocationChannel<String, String> channel1 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(PassingString.class))
                                    .asyncInvoke()
                                    .orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(PassingInteger.class))
                                    .asyncInvoke()
                                    .orderByCall();
        SparseChannelsCompat.combine(channel1, channel2)
                            .buildChannels()
                            .pass(new ParcelableSelectable<String>("test1", 0))
                            .pass(new ParcelableSelectable<Integer>(1, 1))
                            .close();
        SparseChannelsCompat.combine(3, channel1, channel2)
                            .buildChannels()
                            .pass(new ParcelableSelectable<String>("test2", 3))
                            .pass(new ParcelableSelectable<Integer>(2, 4))
                            .close();
        SparseChannelsCompat.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .buildChannels()
                            .pass(new ParcelableSelectable<String>("test3", 0))
                            .pass(new ParcelableSelectable<Integer>(3, 1))
                            .close();
        SparseChannelsCompat.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .buildChannels()
                            .pass(new ParcelableSelectable<String>("test4", -5))
                            .pass(new ParcelableSelectable<Integer>(4, -4))
                            .close();
        final SparseArrayCompat<InvocationChannel<?, ?>> map =
                new SparseArrayCompat<InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        SparseChannelsCompat.combine(map)
                            .buildChannels()
                            .pass(new ParcelableSelectable<String>("test5", 31))
                            .pass(new ParcelableSelectable<Integer>(5, 17))
                            .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1", "test2",
                "test3", "test4", "test5");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
    }

    public void testCombineAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<Integer, Integer> channel2;
        channel1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingString.class))
                                       .asyncInvoke()
                                       .orderByCall();
        channel2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingInteger.class))
                                       .asyncInvoke()
                                       .orderByCall();
        SparseChannelsCompat.combine(channel1, channel2).buildChannels().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingString.class))
                                       .asyncInvoke()
                                       .orderByCall();
        channel2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingInteger.class))
                                       .asyncInvoke()
                                       .orderByCall();
        SparseChannelsCompat.combine(3, channel1, channel2).buildChannels().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingString.class))
                                       .asyncInvoke()
                                       .orderByCall();
        channel2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingInteger.class))
                                       .asyncInvoke()
                                       .orderByCall();
        SparseChannelsCompat.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .buildChannels()
                            .abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingString.class))
                                       .asyncInvoke()
                                       .orderByCall();
        channel2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingInteger.class))
                                       .asyncInvoke()
                                       .orderByCall();
        SparseChannelsCompat.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .buildChannels()
                            .abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingString.class))
                                       .asyncInvoke()
                                       .orderByCall();
        channel2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(PassingInteger.class))
                                       .asyncInvoke()
                                       .orderByCall();
        final SparseArrayCompat<InvocationChannel<?, ?>> map =
                new SparseArrayCompat<InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        SparseChannelsCompat.combine(map).buildChannels().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testCombineError() {

        try {

            SparseChannelsCompat.combine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannelsCompat.combine(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannelsCompat.combine(Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannelsCompat.combine(0, Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            SparseChannelsCompat.combine(new SparseArrayCompat<InputChannel<?>>(0));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInputMap() {

        final ArrayList<ParcelableSelectable<Object>> outputs =
                new ArrayList<ParcelableSelectable<Object>>();
        outputs.add(new ParcelableSelectable<Object>("test21", Sort.STRING));
        outputs.add(new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(Sort.class))
                                    .buildRoutine();
        SparseArrayCompat<IOChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING)
                                         .buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
    }

    public void testInputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(Sort.class))
                                    .buildRoutine();
        SparseArrayCompat<IOChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING)
                                         .buildChannels();
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).pass("test21").close();

        try {

            channel.result().afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testInputMapError() {

        try {

            SparseChannelsCompat.selectParcelable(0, 0,
                    JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                        .on(factoryOf(Sort.class))
                                        .asyncInvoke());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMap() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .channelConfiguration()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .setConfiguration();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                SparseChannelsCompat.merge(Arrays.<IOChannel<?>>asList(channel1, channel2))
                                    .buildChannels();
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(Sort.class))
                                    .invocationConfiguration()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .setConfiguration()
                                    .asyncCall(channel);
        final SparseArrayCompat<OutputChannel<Object>> channelMap =
                SparseChannelsCompat.selectParcelable(output, Sort.INTEGER, Sort.STRING)
                                    .buildChannels();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsExactly("0",
                "1", "2", "3");
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsExactly(0, 1,
                2, 3);
    }

    @SuppressWarnings("unchecked")
    public void testMerge() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .channelConfiguration()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .setConfiguration();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                SparseChannelsCompat.merge(channelMap).buildChannels();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMergeAbort() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .channelConfiguration()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .setConfiguration();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                SparseChannelsCompat.merge(channelMap).buildChannels();
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        try {

            SparseChannelsCompat.merge(new SparseArrayCompat<OutputChannel<?>>(0));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputMap() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(Sort.class))
                                    .buildRoutine();
        SparseArrayCompat<OutputChannel<Object>> channelMap;
        OutputChannel<ParcelableSelectable<Object>> channel;
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannelsCompat.selectParcelable(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap = SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING)
                                         .buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannelsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
    }

    @SuppressWarnings("unchecked")
    public void testOutputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(Sort.class))
                                    .buildRoutine();
        SparseArrayCompat<OutputChannel<Object>> channelMap;
        OutputChannel<ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                 new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap = SparseChannelsCompat.selectParcelable(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                                 new ParcelableSelectable<Object>("test21", Sort.STRING))
                         .result();
        channelMap = SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING)
                                         .buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                 new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap = SparseChannelsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testOutputMapCache() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(Sort.class))
                                    .buildRoutine();
        final OutputChannel<ParcelableSelectable<Object>> channel = routine.asyncCall();
        final SparseArrayCompat<OutputChannel<Object>> channelMap =
                SparseChannelsCompat.selectParcelable(channel,
                        Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        assertThat(toMap(channelMap)).isEqualTo(
                toMap(SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING)
                                          .buildChannels()));
    }

    public void testOutputMapError() {

        try {

            SparseChannelsCompat.selectParcelable(0, 0,
                    JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                        .on(factoryOf(Sort.class))
                                        .asyncCall());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputSelect() {

        final IOChannel<ParcelableSelectable<String>> channel = JRoutineCore.io().buildChannel();
        final OutputChannel<String> outputChannel =
                SparseChannelsCompat.selectParcelable(channel.asOutput(), 33)
                                    .buildChannels()
                                    .get(33);
        channel.pass(new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", -33),
                new ParcelableSelectable<String>("test3", 33),
                new ParcelableSelectable<String>("test4", 333));
        channel.close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testOutputSelectAbort() {

        final IOChannel<ParcelableSelectable<String>> channel = JRoutineCore.io().buildChannel();
        final OutputChannel<String> outputChannel =
                SparseChannelsCompat.selectParcelable(channel.asOutput(), 33)
                                    .buildChannels()
                                    .get(33);
        channel.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    private static class PassingInteger extends TemplateContextInvocation<Integer, Integer> {

        public void onInput(final Integer i, @NotNull final ResultChannel<Integer> result) {

            result.pass(i);
        }
    }

    private static class PassingString extends TemplateContextInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.pass(s);
        }
    }

    private static class Sort extends
            TemplateContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    SparseChannelsCompat.<Object, Integer>selectParcelable(result, INTEGER)
                                        .buildChannels()
                                        .pass((Integer) selectable.data)
                                        .close();
                    break;

                case STRING:
                    SparseChannelsCompat.<Object, String>selectParcelable(result, STRING)
                                        .buildChannels()
                                        .pass((String) selectable.data)
                                        .close();
                    break;
            }
        }
    }
}
