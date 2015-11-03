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
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.R;
import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
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
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.invocation.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.log.Log;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.core.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.invocation.DelegatingContextInvocation.factoryFrom;
import static com.github.dm.jrt.android.invocation.FunctionContextInvocations.factoryOf;
import static com.github.dm.jrt.android.v4.core.LoaderContext.contextFrom;
import static com.github.dm.jrt.builder.InvocationConfiguration.builder;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine unit tests.
 * <p/>
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
        final Routine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withInputClashResolution(
                                                                ClashResolutionType.ABORT_THIS)
                                                        .set()
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
        final Routine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THIS)
                                                        .set()
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
                         .set();
        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .with(invocationConfiguration)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.with(contextFrom(getActivity())).onId(0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityBuilderPurgeInputs() throws InterruptedException {

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .set();
        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .with(invocationConfiguration)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.with(contextFrom(getActivity())).onId(0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.with(contextFrom(getActivity())).onId(0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.with(contextFrom(getActivity()))
                .onId(0)
                .purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityClearError() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_SUCCESS)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final AbortException ignored) {

        }

        result1.checkComplete();

        final OutputChannel<Data> result2 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_SUCCESS)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.checkComplete();

        final OutputChannel<Data> result3 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result3.next()).isSameAs(data1);
        result3.checkComplete();
    }

    public void testActivityClearResult() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_ERROR)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        AbortException error = null;
        final OutputChannel<Data> result2 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_ERROR)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result2.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result2.checkComplete();

        final OutputChannel<Data> result3 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result3.checkComplete();
    }

    public void testActivityContext() {

        final ClassToken<GetContextInvocation<String>> classToken =
                new ClassToken<GetContextInvocation<String>>() {};
        assertThat(JRoutine.with(contextFrom(getActivity()))
                           .on(factoryOf(classToken))
                           .syncCall()
                           .next()).isSameAs(getActivity().getApplicationContext());
    }

    public void testActivityDelegation() {

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine1 = JRoutine.with(contextFrom(getActivity()))
                                                         .on(PassingFunctionContextInvocation
                                                                     .factoryOf())
                                                         .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutine.with(contextFrom(getActivity()))
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

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityInvalidIdError() {

        try {

            JRoutine.with(contextFrom(getActivity())).onId(LoaderConfiguration.AUTO).buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityInvalidTokenError() {

        try {

            JRoutine.with(contextFrom(new TestActivity())).on(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityKeep() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.JOIN)
                                                        .set()
                                                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        final TimeDuration timeout = seconds(10);
        final OutputChannel<String> channel =
                JRoutine.with(contextFrom(getActivity())).onId(0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final MissingInvocationException ignored) {

        }
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testActivityNullPointerErrors() {

        try {

            JRoutine.with((LoaderContext) null).on(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on((FunctionContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with((LoaderContext) null).on(classOfType(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with((LoaderContext) null)
                    .on(instanceOf(ToUpperCase.class, Reflection.NO_ARGS));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity())).on(classOfType((Class<?>) null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity())).on(instanceOf(null, Reflection.NO_ARGS));

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

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withInputClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .set()
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
        final Routine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .set()
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
        final OutputChannel<Data> result1 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.checkComplete();

        AbortException error = null;
        final OutputChannel<Data> result3 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result3.checkComplete();

        final OutputChannel<Data> result4 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result4.next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result4.checkComplete();
    }

    public void testActivityRoutinePurge() throws InterruptedException {

        final InvocationConfiguration invocationConfiguration =
                builder().withInputOrder(OrderType.BY_CALL)
                         .withOutputOrder(OrderType.BY_CALL)
                         .set();
        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .with(invocationConfiguration)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.checkComplete());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityRoutinePurgeInputs() throws InterruptedException {

        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(getActivity()))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test");
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.asyncCall("test1", "test2");
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.asyncCall("test1", "test2");
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivitySame() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine =
                JRoutine.with(contextFrom(getActivity())).on(factoryOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testChannelBuilderWarnings() {

        final CountLog countLog = new CountLog();
        final Builder<ChannelConfiguration> builder = ChannelConfiguration.builder();
        final ChannelConfiguration configuration = builder.withAsyncRunner(Runners.taskRunner())
                                                          .withChannelMaxSize(3)
                                                          .withChannelTimeout(seconds(10))
                                                          .withLogLevel(Level.DEBUG)
                                                          .withLog(countLog)
                                                          .set();
        JRoutine.with(contextFrom(getActivity()))
                .onId(0)
                .channels()
                .with(configuration)
                .set()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.with(contextFrom(fragment))
                .onId(0)
                .channels()
                .with(configuration)
                .set()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    public void testClash() {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Delay.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 = JRoutine.with(contextFrom(getActivity()))
                                                    .on(factoryOf(Abort.class))
                                                    .loaders()
                                                    .withId(0)
                                                    .set()
                                                    .asyncCall(data1)
                                                    .afterMax(timeout);

        try {

            result2.next();

            fail();

        } catch (final InvocationTypeException ignored) {

        }

        result2.checkComplete();
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        final FunctionContextInvocationFactory<Object, Object> factory =
                PassingFunctionContextInvocation.factoryOf();

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(contextFrom(getActivity()),
                                                            factory).setConfiguration(
                    (InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(contextFrom(getActivity()),
                                                            factory).setConfiguration(
                    (LoaderConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentAbort() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THIS)
                                                        .set()
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
        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test");
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.with(contextFrom(fragment)).onId(0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentBuilderPurgeInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test");
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.with(contextFrom(fragment)).onId(0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2");
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.with(contextFrom(fragment)).onId(0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2");
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.with(contextFrom(fragment))
                .onId(0)
                .purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentChannel() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .set()
                                                        .invocations()
                                                        .withOutputOrder(OrderType.BY_CALL)
                                                        .set()
                                                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test1", "test2");
        final OutputChannel<String> channel2 =
                JRoutine.with(contextFrom(fragment)).onId(0).buildChannel();

        assertThat(channel1.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testFragmentDelegation() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine1 = JRoutine.with(contextFrom(fragment))
                                                         .on(PassingFunctionContextInvocation
                                                                     .factoryOf())
                                                         .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutine.with(contextFrom(fragment))
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

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
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
            JRoutine.with(contextFrom(fragment)).onId(LoaderConfiguration.AUTO).buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentInvalidTokenError() {

        try {

            JRoutine.with(contextFrom(new TestFragment())).on(factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentKeep() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.JOIN)
                                                        .set()
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
                JRoutine.with(contextFrom(fragment)).onId(0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final MissingInvocationException ignored) {

        }
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testFragmentNullPointerErrors() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.with((LoaderContext) null).on(factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(fragment)).on((FunctionContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with((LoaderContext) null).on(classOfType(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with((LoaderContext) null)
                    .on(instanceOf(ToUpperCase.class, Reflection.NO_ARGS));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(fragment)).on(classOfType((Class<?>) null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(fragment)).on(instanceOf(null, Reflection.NO_ARGS));

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

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withInputClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .set()
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
        final Routine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                        .on(factoryOf(ToUpperCase.class))
                                                        .loaders()
                                                        .withId(0)
                                                        .withClashResolution(
                                                                ClashResolutionType.ABORT_THAT)
                                                        .set()
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
        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test");
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.checkComplete());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentRoutinePurgeInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine = JRoutine.with(contextFrom(fragment))
                                                              .on(factoryOf(
                                                                      PurgeContextInvocation.class))
                                                              .invocations()
                                                              .withInputOrder(OrderType.BY_CALL)
                                                              .withOutputOrder(OrderType.BY_CALL)
                                                              .withReadTimeout(seconds(10))
                                                              .set()
                                                              .loaders()
                                                              .withId(0)
                                                              .withCacheStrategy(
                                                                      CacheStrategyType.CACHE)
                                                              .set()
                                                              .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test");
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.asyncCall("test1", "test2");
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.asyncCall("test1", "test2");
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentSame() throws InterruptedException {

        final TimeDuration timeout = seconds(10);
        final Data data1 = new Data();
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Data, Data> routine =
                JRoutine.with(contextFrom(fragment)).on(factoryOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testInvocations() {

        final TimeDuration timeout = seconds(10);
        final Routine<String, String> routine1 = JRoutine.with(contextFrom(getActivity()))
                                                         .on(PassingFunctionContextInvocation
                                                                     .<String>factoryOf())
                                                         .invocations()
                                                         .withLog(Logs.androidLog())
                                                         .withLogLevel(Level.WARNING)
                                                         .set()
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
        final Routine<String, String> routine2 = JRoutine.with(contextFrom(getActivity()))
                                                         .on(factoryOf(token2))
                                                         .invocations()
                                                         .withLog(Logs.androidLog())
                                                         .withLogLevel(Level.WARNING)
                                                         .set()
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
        final LoaderConfiguration configuration = LoaderConfiguration.DEFAULT_CONFIGURATION;
        final LoaderContext context = contextFrom(getActivity());

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
        final InvocationConfiguration configuration = builder().withRunner(Runners.taskRunner())
                                                               .withInputMaxSize(3)
                                                               .withInputTimeout(seconds(10))
                                                               .withOutputMaxSize(3)
                                                               .withOutputTimeout(seconds(10))
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(countLog)
                                                               .set();
        JRoutine.with(contextFrom(getActivity()))
                .on(factoryOf(ToUpperCase.class))
                .invocations()
                .with(configuration)
                .set()
                .loaders()
                .withId(0)
                .withInputClashResolution(ClashResolutionType.JOIN)
                .set()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.with(contextFrom(fragment))
                .on(factoryOf(ToUpperCase.class))
                .invocations()
                .with(configuration)
                .set()
                .loaders()
                .withId(0)
                .withInputClashResolution(ClashResolutionType.JOIN)
                .set()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        final LoaderConfiguration configuration = LoaderConfiguration.DEFAULT_CONFIGURATION;
        final LoaderContext context = contextFrom(getActivity());

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
                @NotNull final ResultChannel<Data> result) {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException e) {

                throw new InvocationInterruptedException(e);
            }

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
        public void onDestroy() {

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
