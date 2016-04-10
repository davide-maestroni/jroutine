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
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.R;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.InvocationClashException;
import com.github.dm.jrt.android.core.invocation.InvocationTypeException;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.android.core.invocation.PassingContextInvocation;
import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.core.runner.AndroidRunners;
import com.github.dm.jrt.core.RoutineInvocation.InvocationMode;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine unit tests.
 * <p>
 * Created by davide-maestroni on 12/10/2014.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private static final int TEST_ROUTINE_ID = 0;

    public LoaderRoutineTest() {

        super(TestActivity.class);
    }

    public void testActivityAbort() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withInputClashResolution(
                                                                            ClashResolutionType
                                                                                    .ABORT_THIS)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test1").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");

        try {

            result2.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }
    }

    public void testActivityAbortInput() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .ABORT_THIS)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");

        try {

            result2.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }
    }

    public void testActivityBuilderPurge() throws InterruptedException {

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .setConfiguration();
        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .with(invocationConfiguration)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity())).onId(0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityBuilderPurgeInputs() throws InterruptedException {

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .setConfiguration();
        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .with(invocationConfiguration)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity())).onId(0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity())).onId(0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .onId(0)
                            .purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel8 = routine.asyncCall("test");
        assertThat(channel8.next()).isEqualTo("test");
        assertThat(channel8.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel9 = routine.asyncCall("test1", "test2");
        assertThat(channel9.all()).containsExactly("test1", "test2");
        assertThat(channel9.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel10 = routine.asyncCall("test1", "test2");
        assertThat(channel10.all()).containsExactly("test1", "test2");
        assertThat(channel10.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityClearError() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Abort.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .withCacheStrategy(
                                                                        CacheStrategyType
                                                                                .CACHE_IF_SUCCESS)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final AbortException ignored) {

        }

        result1.hasCompleted();

        final OutputChannel<Data> result2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Delay.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .withCacheStrategy(
                                                                        CacheStrategyType
                                                                                .CACHE_IF_SUCCESS)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.hasCompleted();

        final OutputChannel<Data> result3 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Delay.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        assertThat(result3.next()).isSameAs(data1);
        result3.hasCompleted();
    }

    public void testActivityClearResult() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Delay.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .withCacheStrategy(
                                                                        CacheStrategyType
                                                                                .CACHE_IF_ERROR)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        AbortException error = null;
        final OutputChannel<Data> result2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Abort.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .withCacheStrategy(
                                                                        CacheStrategyType
                                                                                .CACHE_IF_ERROR)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        try {

            result2.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result2.hasCompleted();

        final OutputChannel<Data> result3 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Abort.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result3.hasCompleted();
    }

    public void testActivityContext() {

        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        assertThat(JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(classToken))
                                       .syncCall()
                                       .next()).isSameAs(getActivity().getApplicationContext());
        final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
        assertThat(JRoutineLoaderCompat.with(loaderFrom(getActivity(), contextWrapper))
                                       .on(factoryOf(classToken, (Object[]) null))
                                       .syncCall()
                                       .next()).isSameAs(getActivity().getApplicationContext());
    }

    @SuppressWarnings("ConstantConditions")
    public void testActivityContextError() {

        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        try {
            assertThat(JRoutineLoaderCompat.with(loaderFrom((FragmentActivity) null))
                                           .on(factoryOf(classToken))
                                           .syncCall()
                                           .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            assertThat(JRoutineLoaderCompat.with(loaderFrom(getActivity(), null))
                                           .on(factoryOf(classToken))
                                           .syncCall()
                                           .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        final ContextWrapper contextWrapper = new ContextWrapper(getActivity()) {};
        try {
            assertThat(JRoutineLoaderCompat.with(loaderFrom(getActivity(), contextWrapper))
                                           .on(factoryOf(classToken))
                                           .syncCall()
                                           .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityDelegation() {

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine1 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(PassingContextInvocation.factoryOf())
                                    .buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryFrom(routine1, TEST_ROUTINE_ID, InvocationMode.SYNC))
                                    .buildRoutine();

        assertThat(routine2.asyncCall("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.asyncInvoke().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().afterMax(seconds(10)).next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testActivityInputs() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityInvalidIdError() {

        try {

            JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                .onId(LoaderConfiguration.AUTO)
                                .buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityInvalidTokenError() {

        try {

            JRoutineLoaderCompat.with(loaderFrom(new TestActivity()))
                                .on(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityKeep() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .JOIN)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        final TimeDuration timeout = seconds(10);
        final OutputChannel<String> channel =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).onId(0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final MissingLoaderException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testActivityNullPointerErrors() {

        try {

            JRoutineLoaderCompat.with(null).on(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                .on((ContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoaderCompat.with(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testActivityRestart() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withInputClashResolution(
                                                                            ClashResolutionType
                                                                                    .ABORT_THAT)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test1").afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityRestartOnInput() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .ABORT_THAT)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityRetain() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Delay.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .withCacheStrategy(
                                                                        CacheStrategyType.CACHE)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        final OutputChannel<Data> result2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Delay.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.hasCompleted();

        AbortException error = null;
        final OutputChannel<Data> result3 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Abort.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .withCacheStrategy(
                                                                        CacheStrategyType.CACHE)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result3.hasCompleted();

        final OutputChannel<Data> result4 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Abort.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        try {

            result4.next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result4.hasCompleted();
    }

    public void testActivityRoutinePurge() throws InterruptedException {

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .setConfiguration();
        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .with(invocationConfiguration)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.hasCompleted());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityRoutinePurgeInputs() throws InterruptedException {

        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .withOutputOrder(OrderType.BY_CALL)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test");
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.hasCompleted());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.asyncCall("test1", "test2");
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.hasCompleted());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.asyncCall("test1", "test2");
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.hasCompleted());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivitySame() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Delay.class))
                                                                .buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testChannelBuilderWarnings() {

        final CountLog countLog = new CountLog();
        final Builder<ChannelConfiguration> builder = ChannelConfiguration.builder();
        final ChannelConfiguration configuration = builder.withRunner(AndroidRunners.taskRunner())
                                                          .withChannelMaxSize(3)
                                                          .withChannelMaxDelay(seconds(10))
                                                          .withLogLevel(Level.DEBUG)
                                                          .withLog(countLog)
                                                          .setConfiguration();
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .onId(0)
                            .getChannelConfiguration()
                            .with(configuration)
                            .setConfiguration()
                            .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutineLoaderCompat.with(loaderFrom(fragment))
                            .onId(0)
                            .getChannelConfiguration()
                            .with(configuration)
                            .setConfiguration()
                            .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    public void testClash() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Delay.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .withCacheStrategy(
                                                                        CacheStrategyType.CACHE)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        final OutputChannel<Data> result2 = JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                                                .on(factoryOf(Abort.class))
                                                                .getLoaderConfiguration()
                                                                .withLoaderId(0)
                                                                .setConfiguration()
                                                                .asyncCall(data1)
                                                                .afterMax(timeout);

        try {

            result2.next();

            fail();

        } catch (final InvocationTypeException ignored) {

        }

        result2.hasCompleted();
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        final ContextInvocationFactory<Object, Object> factory =
                PassingContextInvocation.factoryOf();

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(loaderFrom(getActivity()),
                    factory).setConfiguration((InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(loaderFrom(getActivity()),
                    factory).setConfiguration((LoaderConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFactoryId() {

        assertThat(JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(StringCallInvocation.class))
                                       .getLoaderConfiguration()
                                       .withFactoryId(0)
                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                       .setConfiguration()
                                       .asyncCall("test")
                                       .afterMax(seconds(10))
                                       .all()).containsExactly("test");
        assertThat(JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factoryOf(ToUpperCase.class))
                                       .getLoaderConfiguration()
                                       .withFactoryId(0)
                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                       .setConfiguration()
                                       .asyncCall("test")
                                       .afterMax(seconds(10))
                                       .all()).containsExactly("test");
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        assertThat(JRoutineLoaderCompat.with(loaderFrom(fragment))
                                       .on(factoryOf(ToUpperCase.class))
                                       .getLoaderConfiguration()
                                       .withFactoryId(0)
                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                       .setConfiguration()
                                       .asyncCall("test")
                                       .afterMax(seconds(10))
                                       .all()).containsExactly("TEST");
        assertThat(JRoutineLoaderCompat.with(loaderFrom(fragment))
                                       .on(factoryOf(StringCallInvocation.class))
                                       .getLoaderConfiguration()
                                       .withFactoryId(0)
                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                       .setConfiguration()
                                       .asyncCall("test")
                                       .afterMax(seconds(10))
                                       .all()).containsExactly("TEST");
    }

    public void testFragmentAbort() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .ABORT_THIS)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");

        try {

            result2.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }
    }

    public void testFragmentBuilderPurge() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(fragment))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .withOutputOrder(OrderType.BY_CALL)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment)).onId(0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentBuilderPurgeInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(fragment))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .withOutputOrder(OrderType.BY_CALL)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment)).onId(0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment)).onId(0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment))
                            .onId(0)
                            .purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel8 = routine.asyncCall("test");
        assertThat(channel8.next()).isEqualTo("test");
        assertThat(channel8.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel9 = routine.asyncCall("test1", "test2");
        assertThat(channel9.all()).containsExactly("test1", "test2");
        assertThat(channel9.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel10 = routine.asyncCall("test1", "test2");
        assertThat(channel10.all()).containsExactly("test1", "test2");
        assertThat(channel10.hasCompleted());
        JRoutineLoaderCompat.with(loaderFrom(fragment))
                            .on(factoryOf(PurgeContextInvocation.class))
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .setConfiguration()
                            .purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentChannel() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .setConfiguration()
                                                                    .getInvocationConfiguration()
                                                                    .withOutputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test1", "test2");
        final OutputChannel<String> channel2 =
                JRoutineLoaderCompat.with(loaderFrom(fragment)).onId(0).buildChannel();

        assertThat(channel1.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testFragmentContext() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        assertThat(JRoutineLoaderCompat.with(loaderFrom(fragment))
                                       .on(factoryOf(classToken))
                                       .syncCall()
                                       .next()).isSameAs(getActivity().getApplicationContext());
        final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
        assertThat(JRoutineLoaderCompat.with(loaderFrom(fragment, contextWrapper))
                                       .on(factoryOf(classToken))
                                       .syncCall()
                                       .next()).isSameAs(getActivity().getApplicationContext());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFragmentContextError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        try {
            assertThat(JRoutineLoaderCompat.with(loaderFrom((Fragment) null))
                                           .on(factoryOf(classToken))
                                           .syncCall()
                                           .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            assertThat(JRoutineLoaderCompat.with(loaderFrom(fragment, null))
                                           .on(factoryOf(classToken))
                                           .syncCall()
                                           .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        final ContextWrapper contextWrapper = new ContextWrapper(getActivity()) {};
        try {
            assertThat(JRoutineLoaderCompat.with(loaderFrom(fragment, contextWrapper))
                                           .on(factoryOf(classToken))
                                           .syncCall()
                                           .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentDelegation() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine1 = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                     .on(PassingContextInvocation
                                                                             .factoryOf())
                                                                     .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                     .on(factoryFrom(routine1,
                                                                             TEST_ROUTINE_ID,
                                                                             InvocationMode.ASYNC))
                                                                     .buildRoutine();

        assertThat(routine2.asyncCall("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.asyncInvoke().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().afterMax(seconds(10)).next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testFragmentInputs() throws InterruptedException {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testFragmentInvalidIdError() {

        try {

            final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                      .findFragmentById(
                                                                              R.id.test_fragment);
            JRoutineLoaderCompat.with(loaderFrom(fragment))
                                .onId(LoaderConfiguration.AUTO)
                                .buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentInvalidTokenError() {

        try {

            JRoutineLoaderCompat.with(loaderFrom(new TestFragment(), getActivity()))
                                .on(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentKeep() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .JOIN)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testFragmentMissingRoutine() throws InterruptedException {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> channel =
                JRoutineLoaderCompat.with(loaderFrom(fragment)).onId(0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final MissingLoaderException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testFragmentNullPointerErrors() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutineLoaderCompat.with(null).on(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoaderCompat.with(loaderFrom(fragment))
                                .on((ContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoaderCompat.with(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentReset() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withInputClashResolution(
                                                                            ClashResolutionType
                                                                                    .ABORT_THAT)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test1").afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testFragmentRestart() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                    .on(factoryOf(
                                                                            ToUpperCase.class))
                                                                    .getLoaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withClashResolution(
                                                                            ClashResolutionType
                                                                                    .ABORT_THAT)
                                                                    .setConfiguration()
                                                                    .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testFragmentRoutinePurge() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(fragment))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .withOutputOrder(OrderType.BY_CALL)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.hasCompleted());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentRoutinePurgeInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(fragment))
                                    .on(factoryOf(PurgeContextInvocation.class))
                                    .getInvocationConfiguration()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .withOutputOrder(OrderType.BY_CALL)
                                    .withReadTimeout(seconds(10))
                                    .setConfiguration()
                                    .getLoaderConfiguration()
                                    .withLoaderId(0)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .setConfiguration()
                                    .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test");
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.hasCompleted());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.asyncCall("test1", "test2");
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.hasCompleted());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.asyncCall("test1", "test2");
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.hasCompleted());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentSame() throws InterruptedException {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Data, Data> routine = JRoutineLoaderCompat.with(loaderFrom(fragment))
                                                                .on(factoryOf(Delay.class))
                                                                .buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testInvocations() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine1 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(PassingContextInvocation.<String>factoryOf())
                                    .getInvocationConfiguration()
                                    .withLog(AndroidLogs.androidLog())
                                    .withLogLevel(Level.WARNING)
                                    .setConfiguration()
                                    .buildRoutine();
        assertThat(routine1.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine1.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");

        final ClassToken<StringCallInvocation> token2 =
                ClassToken.tokenOf(StringCallInvocation.class);
        final Routine<String, String> routine2 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(token2))
                                    .getInvocationConfiguration()
                                    .withLog(AndroidLogs.androidLog())
                                    .withLogLevel(Level.WARNING)
                                    .setConfiguration()
                                    .buildRoutine();
        assertThat(routine2.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine2.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLoaderError() throws NoSuchMethodException {

        final Logger logger = Logger.newLogger(null, null, this);
        final LoaderConfiguration configuration = LoaderConfiguration.defaultConfiguration();
        final LoaderContextCompat context = loaderFrom(getActivity());

        try {

            new LoaderInvocation<String, String>(null, factoryOf(ToUpperCase.class), configuration,
                    null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(context, null, configuration, null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(context, factoryOf(ToUpperCase.class), null, null,
                    logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(context, factoryOf(ToUpperCase.class),
                    configuration, null, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testRoutineBuilderWarnings() {

        final CountLog countLog = new CountLog();
        final InvocationConfiguration configuration =
                builder().withRunner(AndroidRunners.taskRunner())
                         .withInputLimit(3)
                         .withInputMaxDelay(seconds(10))
                         .withInputMaxSize(33)
                         .withOutputLimit(3)
                         .withOutputMaxDelay(seconds(10))
                         .withOutputMaxSize(33)
                         .withLogLevel(Level.DEBUG)
                         .withLog(countLog)
                         .setConfiguration();
        JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                            .on(factoryOf(ToUpperCase.class))
                            .getInvocationConfiguration()
                            .with(configuration)
                            .setConfiguration()
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .withInputClashResolution(ClashResolutionType.JOIN)
                            .setConfiguration()
                            .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutineLoaderCompat.with(loaderFrom(fragment))
                            .on(factoryOf(ToUpperCase.class))
                            .getInvocationConfiguration()
                            .with(configuration)
                            .setConfiguration()
                            .getLoaderConfiguration()
                            .withLoaderId(0)
                            .withInputClashResolution(ClashResolutionType.JOIN)
                            .setConfiguration()
                            .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        final LoaderConfiguration configuration = LoaderConfiguration.defaultConfiguration();
        final LoaderContextCompat context = loaderFrom(getActivity());

        try {

            new DefaultLoaderRoutine<String, String>(null, factoryOf(ToUpperCase.class),
                    InvocationConfiguration.defaultConfiguration(), configuration);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutine<String, String>(context, null,
                    InvocationConfiguration.defaultConfiguration(), configuration);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutine<String, String>(context, factoryOf(ToUpperCase.class), null,
                    configuration);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutine<String, String>(context, factoryOf(ToUpperCase.class),
                    InvocationConfiguration.defaultConfiguration(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class Abort extends CallContextInvocation<Data, Data> {

        @Override
        protected void onCall(@NotNull final List<? extends Data> inputs,
                @NotNull final ResultChannel<Data> result) throws Exception {

            Thread.sleep(500);
            result.abort(new IllegalStateException());
        }
    }

    @SuppressWarnings("unused")
    private static class CountLog implements Log {

        private int mDgbCount;

        private int mErrCount;

        private int mWrnCount;

        public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mDgbCount;
        }

        public void err(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mErrCount;
        }

        public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mWrnCount;
        }

        public int getDgbCount() {

            return mDgbCount;
        }

        public int getErrCount() {

            return mErrCount;
        }

        public int getWrnCount() {

            return mWrnCount;
        }
    }

    private static class Data {

    }

    private static class Delay extends CallContextInvocation<Data, Data> {

        @Override
        protected void onCall(@NotNull final List<? extends Data> inputs,
                @NotNull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(inputs);
        }
    }

    @SuppressWarnings("unused")
    private static class ErrorInvocation extends CallContextInvocation<String, String> {

        private ErrorInvocation(final int ignored) {

        }

        @Override
        protected void onCall(@NotNull final List<? extends String> inputs,
                @NotNull final ResultChannel<String> result) {

        }
    }

    private static class GetContextInvocation<DATA> extends CallContextInvocation<DATA, Context> {

        @Override
        protected void onCall(@NotNull final List<? extends DATA> inputs,
                @NotNull final ResultChannel<Context> result) {

            result.pass(getContext());
        }
    }

    private static class PurgeContextInvocation extends CallContextInvocation<String, String> {

        private static final Semaphore sSemaphore = new Semaphore(0);

        public static boolean waitDestroy(final int count, final long timeoutMs) throws
                InterruptedException {

            return sSemaphore.tryAcquire(count, timeoutMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onDestroy() throws Exception {

            super.onDestroy();
            sSemaphore.release();
        }

        @Override
        protected void onCall(@NotNull final List<? extends String> inputs,
                @NotNull final ResultChannel<String> result) {

            result.pass(inputs);
        }
    }

    private static class StringCallInvocation extends CallContextInvocation<String, String> {

        @Override
        protected void onCall(@NotNull final List<? extends String> strings,
                @NotNull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class ToUpperCase extends CallContextInvocation<String, String> {

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
