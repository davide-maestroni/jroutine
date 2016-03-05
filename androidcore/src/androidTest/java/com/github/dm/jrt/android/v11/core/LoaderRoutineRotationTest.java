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

package com.github.dm.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.invocation.FunctionContextInvocation;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Semaphore;

import static com.github.dm.jrt.android.invocation.FunctionContextInvocationFactories.factoryOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine rotation unit tests.
 * <p/>
 * Created by davide-maestroni on 01/28/2015.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class LoaderRoutineRotationTest
        extends ActivityInstrumentationTestCase2<RotationTestActivity> {

    public LoaderRoutineRotationTest() {

        super(RotationTestActivity.class);
    }

    public void testActivityNotStaleResult() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                              .on(factoryOf(ToUpperCase.class))
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType.JOIN)
                                                              .withResultStaleTime(
                                                                      TimeDuration.minutes(1))
                                                              .getConfigured()
                                                              .buildRoutine();
        routine.asyncCall("test1");

        simulateRotation();
        TimeDuration.millis(1000).sleepAtLeast();
        assertThat(routine.asyncCall("test2").afterMax(timeout).next()).isEqualTo("TEST1");
    }

    public void testActivityRotationChannel() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        JRoutineLoader.with(loaderFrom(getActivity()))
                      .on(factoryOf(ToUpperCase.class))
                      .withInvocations()
                      .withOutputOrder(OrderType.BY_CALL)
                      .getConfigured()
                      .withLoaders()
                      .withLoaderId(0)
                      .getConfigured()
                      .asyncCall("test1", "test2");

        simulateRotation();

        final OutputChannel<String> channel =
                JRoutineLoader.with(loaderFrom(getActivity())).onId(0).buildChannel();

        assertThat(channel.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testActivityRotationInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine1 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                               .on(factoryOf(ToUpperCase.class))
                                                               .buildRoutine();
        routine1.asyncCall("test1");
        routine1.asyncCall("test2");

        simulateRotation();

        final Routine<String, String> routine2 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                               .on(factoryOf(ToUpperCase.class))
                                                               .buildRoutine();
        final OutputChannel<String> result1 = routine2.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine2.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityRotationSame() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine1 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                           .on(factoryOf(Delay.class))
                                                           .buildRoutine();
        routine1.asyncCall(data1);
        routine1.asyncCall(data1);

        simulateRotation();

        final Routine<Data, Data> routine2 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                           .on(factoryOf(Delay.class))
                                                           .buildRoutine();
        final OutputChannel<Data> result1 = routine2.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine2.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testActivityStaleResult() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                              .on(factoryOf(ToUpperCase.class))
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType.JOIN)
                                                              .withResultStaleTime(
                                                                      TimeDuration.ZERO)
                                                              .getConfigured()
                                                              .buildRoutine();
        routine.asyncCall("test1");

        simulateRotation();
        TimeDuration.millis(1000).sleepAtLeast();
        assertThat(routine.asyncCall("test2").afterMax(timeout).next()).isEqualTo("TEST2");
    }

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

    private static class Delay extends FunctionContextInvocation<Data, Data> {

        @Override
        protected void onCall(@NotNull final List<? extends Data> inputs,
                @NotNull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(inputs);
        }
    }

    private static class ToUpperCase extends FunctionContextInvocation<String, String> {

        @Override
        protected void onCall(@NotNull final List<? extends String> inputs,
                @NotNull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(500));

            for (final String input : inputs) {

                result.pass(input.toUpperCase());
            }
        }
    }
}
