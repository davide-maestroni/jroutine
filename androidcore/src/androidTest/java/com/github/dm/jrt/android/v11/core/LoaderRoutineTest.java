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
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.core.R;
import com.github.dm.jrt.android.invocation.FunctionContextInvocation;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.invocation.InvocationClashException;
import com.github.dm.jrt.android.invocation.InvocationTypeException;
import com.github.dm.jrt.android.invocation.MissingInvocationException;
import com.github.dm.jrt.android.invocation.PassingFunctionContextInvocation;
import com.github.dm.jrt.android.log.Logs;
import com.github.dm.jrt.android.routine.LoaderRoutine;
import com.github.dm.jrt.android.runner.Runners;
import com.github.dm.jrt.builder.ChannelConfiguration;
import com.github.dm.jrt.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.log.Log;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.DelegatingContextInvocation.factoryFrom;
import static com.github.dm.jrt.android.invocation.FunctionContextInvocations.factoryOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.builder.InvocationConfiguration.builder;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine unit tests.
 * <p/>
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

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withInputClashResolution(
                                                                ClashResolutionType.ABORT_THIS)
                                                        .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THIS)
                                                        .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .getConfigured();
        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .with(invocationConfiguration)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
                                                              .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.hasCompleted());
        JRoutine.with(loaderFrom(getActivity())).onId(0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityBuilderPurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .getConfigured();
        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .with(invocationConfiguration)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
                                                              .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutine.with(loaderFrom(getActivity())).onId(0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.hasCompleted());
        JRoutine.with(loaderFrom(getActivity())).onId(0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.hasCompleted());
        JRoutine.with(loaderFrom(getActivity()))
                .onId(0)
                .purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityClearError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_SUCCESS)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final AbortException ignored) {

        }

        result1.hasCompleted();

        final OutputChannel<Data> result2 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_SUCCESS)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.hasCompleted();

        final OutputChannel<Data> result3 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result3.next()).isSameAs(data1);
        result3.hasCompleted();
    }

    public void testActivityClearResult() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_ERROR)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        AbortException error = null;
        final OutputChannel<Data> result2 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_ERROR)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result2.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result2.hasCompleted();

        final OutputChannel<Data> result3 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        assertThat(JRoutine.with(loaderFrom(getActivity()))
                           .on(factoryOf(classToken))
                           .syncCall()
                           .next()).isSameAs(getActivity().getApplicationContext());
    }

    public void testActivityDelegation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine1 = JRoutine.with(loaderFrom(getActivity()))
                                                         .on(PassingFunctionContextInvocation
                                                                     .factoryOf())
                                                         .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutine.with(loaderFrom(getActivity()))
                                                         .on(factoryFrom(routine1, TEST_ROUTINE_ID,
                                                                         DelegationType.SYNC))
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityInvalidIdError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.with(loaderFrom(getActivity())).onId(LoaderConfiguration.AUTO).buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityInvalidTokenError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.with(loaderFrom(new TestActivity())).on(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityKeep() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.JOIN)
                                                        .getConfigured()
                                                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final OutputChannel<String> channel =
                JRoutine.with(loaderFrom(getActivity())).onId(0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final MissingInvocationException ignored) {

        }
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testActivityNullPointerErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.with((LoaderContext) null).on(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(loaderFrom(getActivity()))
                    .on((FunctionContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with((LoaderContext) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testActivityRestart() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withInputClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        final OutputChannel<Data> result2 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.hasCompleted();

        AbortException error = null;
        final OutputChannel<Data> result3 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result3.hasCompleted();

        final OutputChannel<Data> result4 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .getConfigured();
        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .with(invocationConfiguration)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
                                                              .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.hasCompleted());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityRoutinePurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine =
                JRoutine.with(loaderFrom(getActivity())).on(factoryOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testChannelBuilderWarnings() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final CountLog countLog = new CountLog();
        final Builder<ChannelConfiguration> builder = ChannelConfiguration.builder();
        final ChannelConfiguration configuration = builder.withRunner(Runners.taskRunner())
                                                          .withChannelMaxSize(3)
                                                          .withChannelMaxDelay(seconds(10))
                                                          .withLogLevel(Level.DEBUG)
                                                          .withLog(countLog)
                                                          .getConfigured();
        JRoutine.with(loaderFrom(getActivity()))
                .onId(0)
                .withChannels()
                .with(configuration)
                .getConfigured()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.with(loaderFrom(fragment))
                .onId(0)
                .withChannels()
                .with(configuration)
                .getConfigured()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    public void testClash() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                                    .getConfigured()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.hasCompleted();

        final OutputChannel<Data> result2 = JRoutine.with(loaderFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .withLoaders()
                                                    .withLoaderId(0)
                                                    .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final FunctionContextInvocationFactory<Object, Object> factory =
                PassingFunctionContextInvocation.factoryOf();

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(loaderFrom(getActivity()),
                                                            factory).setConfiguration(
                    (InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(loaderFrom(getActivity()),
                                                            factory).setConfiguration(
                    (LoaderConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THIS)
                                                        .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
                                                              .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.hasCompleted());
        JRoutine.with(loaderFrom(fragment)).onId(0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentBuilderPurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
                                                              .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.hasCompleted());
        JRoutine.with(loaderFrom(fragment)).onId(0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.hasCompleted());
        JRoutine.with(loaderFrom(fragment)).onId(0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.hasCompleted());
        JRoutine.with(loaderFrom(fragment)).onId(0).purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentChannel() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .getConfigured()
                                                        .withInvocations()
                                                        .withOutputOrder(OrderType.BY_CALL)
                                                        .getConfigured()
                                                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test1", "test2");
        final OutputChannel<String> channel2 =
                JRoutine.with(loaderFrom(fragment)).onId(0).buildChannel();

        assertThat(channel1.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testFragmentDelegation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine1 = JRoutine.with(loaderFrom(fragment))
                                                         .on(PassingFunctionContextInvocation
                                                                     .factoryOf())
                                                         .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutine.with(loaderFrom(fragment))
                                                         .on(factoryFrom(routine1, TEST_ROUTINE_ID,
                                                                         DelegationType.ASYNC))
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.with(loaderFrom(fragment)).on(factoryOf(ToUpperCase.class)).buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

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
            JRoutine.with(loaderFrom(fragment)).onId(LoaderConfiguration.AUTO).buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentInvalidTokenError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.with(loaderFrom(new TestFragment())).on(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentKeep() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.JOIN)
                                                        .getConfigured()
                                                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testFragmentMissingRoutine() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> channel =
                JRoutine.with(loaderFrom(fragment)).onId(0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final MissingInvocationException ignored) {

        }
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testFragmentNullPointerErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.with((LoaderContext) null).on(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(loaderFrom(fragment)).on((FunctionContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with((LoaderContext) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentReset() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withInputClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .withLoaders()
                                                        .withLoaderId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
                                                              .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.hasCompleted());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentRoutinePurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutine.with(loaderFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .withInvocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .getConfigured()
                                                              .withLoaders()
                                                              .withLoaderId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Data, Data> routine =
                JRoutine.with(loaderFrom(fragment)).on(factoryOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testInvocations() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine1 = JRoutine.with(loaderFrom(getActivity()))
                                                         .on(PassingFunctionContextInvocation
                                                                     .<String>factoryOf())
                                                         .withInvocations()
                                                         .withLog(Logs.androidLog())
                                                         .withLogLevel(Level.WARNING)
                                                         .getConfigured()
                                                         .buildRoutine();
        assertThat(routine1.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine1.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");

        final ClassToken<StringFunctionInvocation> token2 =
                ClassToken.tokenOf(StringFunctionInvocation.class);
        final Routine<String, String> routine2 = JRoutine.with(loaderFrom(getActivity()))
                                                         .on(factoryOf(token2))
                                                         .withInvocations()
                                                         .withLog(Logs.androidLog())
                                                         .withLogLevel(Level.WARNING)
                                                         .getConfigured()
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Logger logger = Logger.newLogger(null, null, this);
        final LoaderConfiguration configuration = LoaderConfiguration.DEFAULT_CONFIGURATION;
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
        final InvocationConfiguration configuration = builder().withRunner(Runners.taskRunner())
                                                               .withInputLimit(3)
                                                               .withInputMaxDelay(seconds(10))
                                                               .withInputMaxSize(33)
                                                               .withOutputLimit(3)
                                                               .withOutputMaxDelay(seconds(10))
                                                               .withOutputMaxSize(33)
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(countLog)
                                                               .getConfigured();
        JRoutine.with(loaderFrom(getActivity()))
                .on(factoryOf(ToUpperCase.class))
                .withInvocations()
                .with(configuration)
                .getConfigured()
                .withLoaders()
                .withLoaderId(0)
                .withInputClashResolution(ClashResolutionType.JOIN)
                .getConfigured()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.with(loaderFrom(fragment))
                .on(factoryOf(ToUpperCase.class))
                .withInvocations()
                .with(configuration)
                .getConfigured()
                .withLoaders()
                .withLoaderId(0)
                .withInputClashResolution(ClashResolutionType.JOIN)
                .getConfigured()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderConfiguration configuration = LoaderConfiguration.DEFAULT_CONFIGURATION;
        final LoaderContext context = loaderFrom(getActivity());

        try {

            new DefaultLoaderRoutine<String, String>(null, factoryOf(ToUpperCase.class),
                                                     InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     configuration);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutine<String, String>(context, null,
                                                     InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     configuration);

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
                                                     InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class Abort extends FunctionContextInvocation<Data, Data> {

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

    private static class Delay extends FunctionContextInvocation<Data, Data> {

        @Override
        protected void onCall(@NotNull final List<? extends Data> inputs,
                @NotNull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(inputs);
        }
    }

    @SuppressWarnings("unused")
    private static class ErrorInvocation extends FunctionContextInvocation<String, String> {

        private ErrorInvocation(final int ignored) {

        }

        @Override
        protected void onCall(@NotNull final List<? extends String> inputs,
                @NotNull final ResultChannel<String> result) {

        }
    }

    private static class GetContextInvocation<DATA>
            extends FunctionContextInvocation<DATA, Context> {

        @Override
        protected void onCall(@NotNull final List<? extends DATA> inputs,
                @NotNull final ResultChannel<Context> result) {

            result.pass(getContext());
        }
    }

    private static class PurgeContextInvocation extends FunctionContextInvocation<String, String> {

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

    private static class StringFunctionInvocation
            extends FunctionContextInvocation<String, String> {

        @Override
        protected void onCall(@NotNull final List<? extends String> strings,
                @NotNull final ResultChannel<String> result) {

            result.pass(strings);
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
