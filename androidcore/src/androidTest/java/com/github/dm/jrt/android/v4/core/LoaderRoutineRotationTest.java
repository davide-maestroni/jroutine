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

package com.github.dm.jrt.android.v4.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Semaphore;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.zero;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine rotation unit tests.
 * <p>
 * Created by davide-maestroni on 01/28/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderRoutineRotationTest
        extends ActivityInstrumentationTestCase2<RotationTestActivity> {

    public LoaderRoutineRotationTest() {

        super(RotationTestActivity.class);
    }

    public void testActivityNotStaleResult() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = UnitDuration.seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .JOIN)
                                                                    .withResultStaleTime(
                                                                            UnitDuration.minutes(1))
                                                                    .apply()
                                                                    .buildRoutine();
        routine.async("test1");

        simulateRotation();
        UnitDuration.seconds(5).sleepAtLeast();
        assertThat(routine.async("test2").after(timeout).next()).isEqualTo("TEST1");
    }

    public void testActivityRotationChannel() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = UnitDuration.seconds(10);
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .on(factoryOf(ToUpperCase.class))
                            .invocationConfiguration()
                            .withOutputOrder(OrderType.BY_CALL)
                            .apply()
                            .loaderConfiguration()
                            .withLoaderId(0)
                            .apply()
                            .async("test1", "test2");

        simulateRotation();

        final Channel<?, String> channel =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).onId(0).buildChannel();

        assertThat(channel.after(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testActivityRotationInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = UnitDuration.seconds(10);
        final Routine<String, String> routine1 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(ToUpperCase.class))
                                    .buildRoutine();
        routine1.async("test1");
        routine1.async("test2");

        simulateRotation();

        final Routine<String, String> routine2 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(ToUpperCase.class))
                                    .buildRoutine();
        final Channel<?, String> result1 = routine2.async("test1").after(timeout);
        final Channel<?, String> result2 = routine2.async("test2").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityRotationSame() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = UnitDuration.seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                 .on(factoryOf(Delay.class))
                                                                 .buildRoutine();
        routine1.async(data1);
        routine1.async(data1);

        simulateRotation();

        final Routine<Data, Data> routine2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                 .on(factoryOf(Delay.class))
                                                                 .buildRoutine();
        final Channel<?, Data> result1 = routine2.async(data1).after(timeout);
        final Channel<?, Data> result2 = routine2.async(data1).after(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testActivityStaleResult() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = UnitDuration.seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .JOIN)
                                                                    .withResultStaleTime(zero())
                                                                    .apply()
                                                                    .buildRoutine();
        routine.async("test1");

        simulateRotation();
        UnitDuration.seconds(5).sleepAtLeast();
        assertThat(routine.async("test2").after(timeout).next()).isEqualTo("TEST2");
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private void simulateRotation() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);

        getActivity().runOnUiThread(new Runnable() {

            public void run() {

                getActivity().recreate();
                semaphore.release();
            }
        });

        semaphore.acquire();
        getInstrumentation().waitForIdleSync();
    }

    private static class Data {

    }

    private static class Delay extends CallContextInvocation<Data, Data> {

        @Override
        protected void onCall(@NotNull final List<? extends Data> inputs,
                @NotNull final Channel<Data, ?> result) {

            result.after(UnitDuration.millis(500)).pass(inputs);
        }
    }

    private static class ToUpperCase extends CallContextInvocation<String, String> {

        @Override
        protected void onCall(@NotNull final List<? extends String> inputs,
                @NotNull final Channel<String, ?> result) {

            result.after(UnitDuration.millis(500));

            for (final String input : inputs) {

                result.pass(input.toUpperCase());
            }
        }
    }
}
