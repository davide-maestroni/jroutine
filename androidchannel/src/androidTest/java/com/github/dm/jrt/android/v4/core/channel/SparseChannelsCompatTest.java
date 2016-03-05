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

package com.github.dm.jrt.android.v4.core.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.support.v4.util.SparseArrayCompat;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.channel.ParcelableSelectable;
import com.github.dm.jrt.android.invocation.FilterContextInvocation;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.IOChannelBuilder;
import com.github.dm.jrt.core.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p/>
 * Created by davide-maestroni on 08/03/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class SparseChannelsCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public SparseChannelsCompatTest() {

        super(TestActivity.class);
    }

    public void testCombine() {

        final InvocationChannel<String, String> channel1 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingInteger.class))
                               .asyncInvoke()
                               .orderByCall();
        SparseChannelsCompat.combine(channel1, channel2)
                            .build()
                            .pass(new ParcelableSelectable<String>("test1", 0))
                            .pass(new ParcelableSelectable<Integer>(1, 1))
                            .close();
        SparseChannelsCompat.combine(3, channel1, channel2)
                            .build()
                            .pass(new ParcelableSelectable<String>("test2", 3))
                            .pass(new ParcelableSelectable<Integer>(2, 4))
                            .close();
        SparseChannelsCompat.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .build()
                            .pass(new ParcelableSelectable<String>("test3", 0))
                            .pass(new ParcelableSelectable<Integer>(3, 1))
                            .close();
        SparseChannelsCompat.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .build()
                            .pass(new ParcelableSelectable<String>("test4", -5))
                            .pass(new ParcelableSelectable<Integer>(4, -4))
                            .close();
        final SparseArrayCompat<InvocationChannel<?, ?>> map =
                new SparseArrayCompat<InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        SparseChannelsCompat.combine(map)
                            .build()
                            .pass(new ParcelableSelectable<String>("test5", 31))
                            .pass(new ParcelableSelectable<Integer>(5, 17))
                            .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1", "test2",
                                                                                  "test3", "test4",
                                                                                  "test5");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
    }

    public void testCombineAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<Integer, Integer> channel2;
        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        SparseChannelsCompat.combine(channel1, channel2).build().abort();

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

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        SparseChannelsCompat.combine(3, channel1, channel2).build().abort();

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

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        SparseChannelsCompat.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .build()
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

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        SparseChannelsCompat.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                            .build()
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

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        final SparseArrayCompat<InvocationChannel<?, ?>> map =
                new SparseArrayCompat<InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        SparseChannelsCompat.combine(map).build().abort();

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
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(Sort.class))
                               .buildRoutine();
        SparseArrayCompat<IOChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(channel,
                                                           Arrays.asList(Sort.INTEGER, Sort.STRING))
                                         .build();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap =
                SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING).build();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                                           channel).build();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
    }

    public void testInputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(Sort.class))
                               .buildRoutine();
        SparseArrayCompat<IOChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(channel,
                                                           Arrays.asList(Sort.INTEGER, Sort.STRING))
                                         .build();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap =
                SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING).build();
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).pass("test21").close();

        try {

            channel.result().afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = SparseChannelsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                                           channel).build();
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
                                                  JRoutineService.with(serviceFrom(getActivity()))
                                                                 .on(factoryOf(Sort.class))
                                                                 .asyncInvoke());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMap() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                SparseChannelsCompat.merge(Arrays.<IOChannel<?>>asList(channel1, channel2)).build();
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(Sort.class))
                               .withInvocations()
                               .withInputOrder(OrderType.BY_CALL)
                               .getConfigured()
                               .asyncCall(channel);
        final SparseArrayCompat<OutputChannel<Object>> channelMap =
                SparseChannelsCompat.selectParcelable(output, Sort.INTEGER, Sort.STRING).build();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsExactly("0",
                                                                                            "1",
                                                                                            "2",
                                                                                            "3");
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsExactly(0, 1,
                                                                                             2, 3);
    }

    @SuppressWarnings("unchecked")
    public void testMerge() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                SparseChannelsCompat.merge(channelMap).build();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMergeAbort() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        final OutputChannel<? extends ParcelableSelectable<?>> outputChannel =
                SparseChannelsCompat.merge(channelMap).build();
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
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(Sort.class))
                               .buildRoutine();
        SparseArrayCompat<OutputChannel<Object>> channelMap;
        OutputChannel<ParcelableSelectable<Object>> channel;
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                    new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannelsCompat.selectParcelable(channel,
                                                           Arrays.asList(Sort.INTEGER, Sort.STRING))
                                         .build();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                                    new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap =
                SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING).build();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                    new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = SparseChannelsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                                                           channel).build();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
    }

    @SuppressWarnings("unchecked")
    public void testOutputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineService.with(serviceFrom(getActivity()))
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
                                                           Arrays.asList(Sort.INTEGER, Sort.STRING))
                                         .build();
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
        channelMap =
                SparseChannelsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING).build();
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
                                                           channel).build();
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

    public void testOutputMapError() {

        try {

            SparseChannelsCompat.selectParcelable(0, 0,
                                                  JRoutineService.with(serviceFrom(getActivity()))
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
                SparseChannelsCompat.selectParcelable(channel.asOutput(), 33).build().get(33);
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
                SparseChannelsCompat.selectParcelable(channel.asOutput(), 33).build().get(33);
        channel.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    private static class PassingInteger extends FilterContextInvocation<Integer, Integer> {

        private PassingInteger() {

            super(null);
        }

        public void onInput(final Integer i, @NotNull final ResultChannel<Integer> result) {

            result.pass(i);
        }
    }

    private static class PassingString extends FilterContextInvocation<String, String> {

        private PassingString() {

            super(null);
        }

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.pass(s);
        }
    }

    private static class Sort extends
            FilterContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        private Sort() {

            super(null);
        }

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    SparseChannelsCompat.<Object, Integer>selectParcelable(result, INTEGER)
                                        .build()
                                        .pass((Integer) selectable.data)
                                        .close();
                    break;

                case STRING:
                    SparseChannelsCompat.<Object, String>selectParcelable(result, STRING)
                                        .build()
                                        .pass((String) selectable.data)
                                        .close();
                    break;
            }
        }
    }
}
