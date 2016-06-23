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
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.R;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.IdentityContextInvocation;
import com.github.dm.jrt.android.core.invocation.InvocationClashException;
import com.github.dm.jrt.android.core.invocation.InvocationTypeException;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.core.runner.AndroidRunners;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.RoutineContextInvocation.factoryFrom;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine unit tests.
 * <p>
 * Created by davide-maestroni on 12/10/2014.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class LoaderRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private static final int TEST_ROUTINE_ID = 0;

    public LoaderRoutineTest() {

        super(TestActivity.class);
    }

    public void testActivityAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withInputClashResolution(
                                                                      ClashResolutionType
                                                                              .ABORT_THIS)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test1").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");

        try {

            result2.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }
    }

    public void testActivityAbortInput() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType
                                                                              .ABORT_THIS)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");

        try {

            result2.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }
    }

    public void testActivityBuilderPurge() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .apply();
        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .with(invocationConfiguration)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel4 = routine.async("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity())).withId(0).clear();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
        final Channel<?, String> channel5 = routine.async("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityBuilderPurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .apply();
        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .with(invocationConfiguration)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel5 = routine.async("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity())).withId(0).clear("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel6 = routine.async("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity())).withId(0).clear("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel7 = routine.async("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .withId(0)
                      .clear(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel8 = routine.async("test");
        assertThat(channel8.next()).isEqualTo("test");
        assertThat(channel8.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel9 = routine.async("test1", "test2");
        assertThat(channel9.all()).containsExactly("test1", "test2");
        assertThat(channel9.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel10 = routine.async("test1", "test2");
        assertThat(channel10.all()).containsExactly("test1", "test2");
        assertThat(channel10.hasCompleted());
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityClearError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Channel<?, Data> result1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Abort.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .withCacheStrategy(
                                                               CacheStrategyType.CACHE_IF_SUCCESS)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        try {

            result1.next();

            fail();

        } catch (final AbortException ignored) {

        }

        result1.hasCompleted();

        final Channel<?, Data> result2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Delay.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .withCacheStrategy(
                                                               CacheStrategyType.CACHE_IF_SUCCESS)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.hasCompleted();

        final Channel<?, Data> result3 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Delay.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        assertThat(result3.next()).isSameAs(data1);
        result3.hasCompleted();
    }

    public void testActivityClearResult() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Channel<?, Data> result1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Delay.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .withCacheStrategy(
                                                               CacheStrategyType.CACHE_IF_ERROR)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        AbortException error = null;
        final Channel<?, Data> result2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Abort.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .withCacheStrategy(
                                                               CacheStrategyType.CACHE_IF_ERROR)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        try {

            result2.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result2.hasCompleted();

        final Channel<?, Data> result3 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Abort.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result3.hasCompleted();
    }

    public void testActivityContext() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        assertThat(JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(classToken))
                                 .sync()
                                 .close()
                                 .next()).isSameAs(getActivity().getApplicationContext());
        final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
        assertThat(JRoutineLoader.on(loaderFrom(getActivity(), contextWrapper))
                                 .with(factoryOf(classToken, (Object[]) null))
                                 .sync()
                                 .close()
                                 .next()).isSameAs(getActivity().getApplicationContext());
    }

    @SuppressWarnings("ConstantConditions")
    public void testActivityContextError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        try {
            assertThat(JRoutineLoader.on(loaderFrom((Activity) null))
                                     .with(factoryOf(classToken))
                                     .sync()
                                     .close()
                                     .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            assertThat(JRoutineLoader.on(loaderFrom(getActivity(), null))
                                     .with(factoryOf(classToken))
                                     .sync()
                                     .close()
                                     .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        final ContextWrapper contextWrapper = new ContextWrapper(getActivity()) {};
        try {
            assertThat(JRoutineLoader.on(loaderFrom(getActivity(), contextWrapper))
                                     .with(factoryOf(classToken))
                                     .sync()
                                     .close()
                                     .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityDelegation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<Object, Object> routine1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                               .with(IdentityContextInvocation
                                                                       .factoryOf())
                                                               .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                               .with(factoryFrom(routine1,
                                                                       TEST_ROUTINE_ID,
                                                                       InvocationMode.SYNC))
                                                               .buildRoutine();

        assertThat(routine2.async("test1").after(timeout).all()).containsExactly("test1");

        final Channel<Object, Object> channel = routine2.async().after(timeout).pass("test2");
        channel.immediately().abort(new IllegalArgumentException());

        try {

            channel.close().after(seconds(10)).next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testActivityInputs() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityInvalidIdError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutineLoader.on(loaderFrom(getActivity()))
                          .withId(LoaderConfiguration.AUTO)
                          .buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityInvalidTokenError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutineLoader.on(loaderFrom(new TestActivity()))
                          .with(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityKeep() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType.JOIN)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Channel<?, String> channel =
                JRoutineLoader.on(loaderFrom(getActivity())).withId(0).buildChannel();

        try {

            channel.after(timeout).all();

            fail();

        } catch (final MissingLoaderException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testActivityNullPointerErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutineLoader.on(null).with(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoader.on(loaderFrom(getActivity()))
                          .with((ContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoader.on(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testActivityRestart() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withInputClashResolution(
                                                                      ClashResolutionType
                                                                              .ABORT_THAT)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test1").after(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityRestartOnInput() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType
                                                                              .ABORT_THAT)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityRetain() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Channel<?, Data> result1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Delay.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        final Channel<?, Data> result2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Delay.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.hasCompleted();

        AbortException error = null;
        final Channel<?, Data> result3 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Abort.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result3.hasCompleted();

        final Channel<?, Data> result4 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Abort.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        try {

            result4.next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result4.hasCompleted();
    }

    public void testActivityRoutinePurge() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .apply();
        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .with(invocationConfiguration)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel = routine.async("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.hasCompleted());
        routine.clear();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityRoutinePurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .withInputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel1 = routine.async("test");
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.hasCompleted());
        routine.clear("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel2 = routine.async("test1", "test2");
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.hasCompleted());
        routine.clear("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel3 = routine.async("test1", "test2");
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.hasCompleted());
        routine.clear(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivitySame() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine = JRoutineLoader.on(loaderFrom(getActivity()))
                                                          .with(factoryOf(Delay.class))
                                                          .buildRoutine();
        final Channel<?, Data> result1 = routine.async(data1).after(timeout);
        final Channel<?, Data> result2 = routine.async(data1).after(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testChannelBuilderWarnings() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final CountLog countLog = new CountLog();
        final Builder<ChannelConfiguration> builder = ChannelConfiguration.builder();
        final ChannelConfiguration configuration = builder.withRunner(AndroidRunners.taskRunner())
                                                          .withMaxSize(3)
                                                          .withBackoff(seconds(10))
                                                          .withLogLevel(Level.DEBUG)
                                                          .withLog(countLog)
                                                          .apply();
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .withId(0)
                      .channelConfiguration()
                      .with(configuration)
                      .apply()
                      .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutineLoader.on(loaderFrom(fragment))
                      .withId(0)
                      .channelConfiguration()
                      .with(configuration)
                      .apply()
                      .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    public void testClash() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Channel<?, Data> result1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Delay.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        final Channel<?, Data> result2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                       .with(factoryOf(Abort.class))
                                                       .loaderConfiguration()
                                                       .withLoaderId(0)
                                                       .apply()
                                                       .async(data1)
                                                       .after(timeout);

        try {

            result2.next();

            fail();

        } catch (final InvocationTypeException ignored) {

        }

        result2.hasCompleted();
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ContextInvocationFactory<Object, Object> factory =
                IdentityContextInvocation.factoryOf();

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(loaderFrom(getActivity()),
                    factory).apply((InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(loaderFrom(getActivity()),
                    factory).apply((LoaderConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConstructor() {

        boolean failed = false;
        try {
            new JRoutineLoader();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    public void testFactoryId() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(StringCallInvocation.class))
                                 .loaderConfiguration()
                                 .withFactoryId(0)
                                 .withCacheStrategy(CacheStrategyType.CACHE)
                                 .apply()
                                 .async("test")
                                 .after(seconds(10))
                                 .all()).containsExactly("test");
        assertThat(JRoutineLoader.on(loaderFrom(getActivity()))
                                 .with(factoryOf(ToUpperCase.class))
                                 .loaderConfiguration()
                                 .withFactoryId(0)
                                 .withCacheStrategy(CacheStrategyType.CACHE)
                                 .apply()
                                 .async("test")
                                 .after(seconds(10))
                                 .all()).containsExactly("test");
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        assertThat(JRoutineLoader.on(loaderFrom(fragment))
                                 .with(factoryOf(ToUpperCase.class))
                                 .loaderConfiguration()
                                 .withFactoryId(0)
                                 .withCacheStrategy(CacheStrategyType.CACHE)
                                 .apply()
                                 .async("test")
                                 .after(seconds(10))
                                 .all()).containsExactly("TEST");
        assertThat(JRoutineLoader.on(loaderFrom(fragment))
                                 .with(factoryOf(StringCallInvocation.class))
                                 .loaderConfiguration()
                                 .withFactoryId(0)
                                 .withCacheStrategy(CacheStrategyType.CACHE)
                                 .apply()
                                 .async("test")
                                 .after(seconds(10))
                                 .all()).containsExactly("TEST");
    }

    public void testFragmentAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType
                                                                              .ABORT_THIS)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");

        try {

            result2.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }
    }

    public void testFragmentBuilderPurge() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .withInputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel4 = routine.async("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment)).withId(0).clear();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
        final Channel<?, String> channel5 = routine.async("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentBuilderPurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .withInputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel5 = routine.async("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment)).withId(0).clear("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel6 = routine.async("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment)).withId(0).clear("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel7 = routine.async("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment))
                      .withId(0)
                      .clear(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel8 = routine.async("test");
        assertThat(channel8.next()).isEqualTo("test");
        assertThat(channel8.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel9 = routine.async("test1", "test2");
        assertThat(channel9.all()).containsExactly("test1", "test2");
        assertThat(channel9.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel10 = routine.async("test1", "test2");
        assertThat(channel10.all()).containsExactly("test1", "test2");
        assertThat(channel10.hasCompleted());
        JRoutineLoader.on(loaderFrom(fragment))
                      .with(factoryOf(PurgeContextInvocation.class))
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .apply()
                      .clear(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentChannel() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .apply()
                                                              .invocationConfiguration()
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> channel1 = routine.async("test1", "test2");
        final Channel<?, String> channel2 =
                JRoutineLoader.on(loaderFrom(fragment)).withId(0).buildChannel();

        assertThat(channel1.after(timeout).all()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.after(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testFragmentContext() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        assertThat(JRoutineLoader.on(loaderFrom(fragment))
                                 .with(factoryOf(classToken))
                                 .sync()
                                 .close()
                                 .next()).isSameAs(getActivity().getApplicationContext());
        final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
        assertThat(JRoutineLoader.on(loaderFrom(fragment, contextWrapper))
                                 .with(factoryOf(classToken))
                                 .sync()
                                 .close()
                                 .next()).isSameAs(getActivity().getApplicationContext());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFragmentContextError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        try {
            assertThat(JRoutineLoader.on(loaderFrom((Fragment) null))
                                     .with(factoryOf(classToken))
                                     .sync()
                                     .close()
                                     .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            assertThat(JRoutineLoader.on(loaderFrom(fragment, null))
                                     .with(factoryOf(classToken))
                                     .sync()
                                     .close()
                                     .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final NullPointerException ignored) {

        }

        final ContextWrapper contextWrapper = new ContextWrapper(getActivity()) {};
        try {
            assertThat(JRoutineLoader.on(loaderFrom(fragment, contextWrapper))
                                     .with(factoryOf(classToken))
                                     .sync()
                                     .close()
                                     .next()).isSameAs(getActivity().getApplicationContext());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentDelegation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine1 = JRoutineLoader.on(loaderFrom(fragment))
                                                               .with(IdentityContextInvocation
                                                                       .factoryOf())
                                                               .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutineLoader.on(loaderFrom(fragment))
                                                               .with(factoryFrom(routine1,
                                                                       TEST_ROUTINE_ID,
                                                                       InvocationMode.ASYNC))
                                                               .buildRoutine();

        assertThat(routine2.async("test1").after(timeout).all()).containsExactly("test1");

        final Channel<Object, Object> channel = routine2.async().after(timeout).pass("test2");
        channel.immediately().abort(new IllegalArgumentException());

        try {

            channel.close().after(seconds(10)).next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testFragmentInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testFragmentInvalidIdError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                      .findFragmentById(
                                                                              R.id.test_fragment);
            JRoutineLoader.on(loaderFrom(fragment)).withId(LoaderConfiguration.AUTO).buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentInvalidTokenError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutineLoader.on(loaderFrom(new TestFragment(), getActivity()))
                          .with(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentKeep() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType.JOIN)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testFragmentMissingRoutine() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Channel<?, String> channel =
                JRoutineLoader.on(loaderFrom(fragment)).withId(0).buildChannel();

        try {

            channel.after(timeout).all();

            fail();

        } catch (final MissingLoaderException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testFragmentNullPointerErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutineLoader.on(null).with(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoader.on(loaderFrom(fragment)).with((ContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineLoader.on(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentReset() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withInputClashResolution(
                                                                      ClashResolutionType
                                                                              .ABORT_THAT)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test1").after(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testFragmentRestart() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                              .with(factoryOf(ToUpperCase.class))
                                                              .loaderConfiguration()
                                                              .withLoaderId(0)
                                                              .withClashResolution(
                                                                      ClashResolutionType
                                                                              .ABORT_THAT)
                                                              .apply()
                                                              .buildRoutine();
        final Channel<?, String> result1 = routine.async("test1").after(timeout);
        final Channel<?, String> result2 = routine.async("test2").after(timeout);

        try {

            result1.next();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testFragmentRoutinePurge() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .withInputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel = routine.async("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.hasCompleted());
        routine.clear();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentRoutinePurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutineLoader.on(loaderFrom(fragment))
                                                                    .with(factoryOf(
                                                                            PurgeContextInvocation.class))
                                                                    .invocationConfiguration()
                                                                    .withInputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputOrder(
                                                                            OrderType.BY_CALL)
                                                                    .withOutputTimeout(seconds(10))
                                                                    .apply()
                                                                    .loaderConfiguration()
                                                                    .withLoaderId(0)
                                                                    .withCacheStrategy(
                                                                            CacheStrategyType.CACHE)
                                                                    .apply()
                                                                    .buildRoutine();
        final Channel<?, String> channel1 = routine.async("test");
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.hasCompleted());
        routine.clear("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel2 = routine.async("test1", "test2");
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.hasCompleted());
        routine.clear("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final Channel<?, String> channel3 = routine.async("test1", "test2");
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.hasCompleted());
        routine.clear(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentSame() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Data data1 = new Data();
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Data, Data> routine =
                JRoutineLoader.on(loaderFrom(fragment)).with(factoryOf(Delay.class)).buildRoutine();
        final Channel<?, Data> result1 = routine.async(data1).after(timeout);
        final Channel<?, Data> result2 = routine.async(data1).after(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testInvocations() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine1 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                               .with(IdentityContextInvocation
                                                                       .<String>factoryOf())
                                                               .invocationConfiguration()
                                                               .withLog(AndroidLogs.androidLog())
                                                               .withLogLevel(Level.WARNING)
                                                               .apply()
                                                               .buildRoutine();
        assertThat(routine1.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine1.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine1.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");

        final ClassToken<StringCallInvocation> token2 =
                ClassToken.tokenOf(StringCallInvocation.class);
        final Routine<String, String> routine2 = JRoutineLoader.on(loaderFrom(getActivity()))
                                                               .with(factoryOf(token2))
                                                               .invocationConfiguration()
                                                               .withLog(AndroidLogs.androidLog())
                                                               .withLogLevel(Level.WARNING)
                                                               .apply()
                                                               .buildRoutine();
        assertThat(routine2.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine2.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine2.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLoaderError() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Logger logger = Logger.newLogger(null, null, this);
        final LoaderConfiguration configuration = LoaderConfiguration.defaultConfiguration();
        final LoaderContext context = loaderFrom(getActivity());

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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final CountLog countLog = new CountLog();
        final InvocationConfiguration configuration =
                builder().withRunner(AndroidRunners.taskRunner())
                         .withInputLimit(3)
                         .withInputBackoff(seconds(10))
                         .withInputMaxSize(33)
                         .withOutputLimit(3)
                         .withOutputBackoff(seconds(10))
                         .withOutputMaxSize(33)
                         .withLogLevel(Level.DEBUG)
                         .withLog(countLog)
                         .apply();
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .with(factoryOf(ToUpperCase.class))
                      .invocationConfiguration()
                      .with(configuration)
                      .apply()
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .withInputClashResolution(ClashResolutionType.JOIN)
                      .apply()
                      .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutineLoader.on(loaderFrom(fragment))
                      .with(factoryOf(ToUpperCase.class))
                      .invocationConfiguration()
                      .with(configuration)
                      .apply()
                      .loaderConfiguration()
                      .withLoaderId(0)
                      .withInputClashResolution(ClashResolutionType.JOIN)
                      .apply()
                      .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderConfiguration configuration = LoaderConfiguration.defaultConfiguration();
        final LoaderContext context = loaderFrom(getActivity());

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

    public void testSize() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Channel<Object, Object> channel = JRoutineLoader.on(loaderFrom(getActivity()))
                                                              .with(IdentityContextInvocation
                                                                      .factoryOf())
                                                              .async();
        assertThat(channel.inputCount()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.inputCount()).isEqualTo(1);
        final Channel<?, Object> result = channel.close();
        assertThat(result.after(seconds(10)).hasCompleted()).isTrue();
        assertThat(result.outputCount()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.skipNext(1).outputCount()).isEqualTo(0);
    }

    private static class Abort extends CallContextInvocation<Data, Data> {

        @Override
        protected void onCall(@NotNull final List<? extends Data> inputs,
                @NotNull final Channel<Data, ?> result) throws Exception {

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
                @NotNull final Channel<Data, ?> result) {

            result.after(UnitDuration.millis(500)).pass(inputs);
        }
    }

    @SuppressWarnings("unused")
    private static class ErrorInvocation extends CallContextInvocation<String, String> {

        private ErrorInvocation(final int ignored) {

        }

        @Override
        protected void onCall(@NotNull final List<? extends String> inputs,
                @NotNull final Channel<String, ?> result) {

        }
    }

    private static class GetContextInvocation<DATA> extends CallContextInvocation<DATA, Context> {

        @Override
        protected void onCall(@NotNull final List<? extends DATA> inputs,
                @NotNull final Channel<Context, ?> result) {

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
        public void onDiscard() throws Exception {

            super.onDiscard();
            sSemaphore.release();
        }

        @Override
        protected void onCall(@NotNull final List<? extends String> inputs,
                @NotNull final Channel<String, ?> result) {

            result.pass(inputs);
        }
    }

    private static class StringCallInvocation extends CallContextInvocation<String, String> {

        @Override
        protected void onCall(@NotNull final List<? extends String> strings,
                @NotNull final Channel<String, ?> result) {

            result.pass(strings);
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
