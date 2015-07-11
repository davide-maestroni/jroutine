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
package com.gh.bmd.jrt.android.v4.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.R;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.DelegatingContextInvocation;
import com.gh.bmd.jrt.android.invocation.FunctionContextInvocation;
import com.gh.bmd.jrt.android.invocation.InvocationClashException;
import com.gh.bmd.jrt.android.invocation.InvocationTypeException;
import com.gh.bmd.jrt.android.invocation.MissingInvocationException;
import com.gh.bmd.jrt.android.invocation.PassingContextInvocation;
import com.gh.bmd.jrt.android.invocation.TemplateContextInvocation;
import com.gh.bmd.jrt.android.log.Logs;
import com.gh.bmd.jrt.android.routine.LoaderRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.invocation.TemplateInvocation;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.invocation.ContextInvocations.factoryFrom;
import static com.gh.bmd.jrt.android.invocation.ContextInvocations.factoryOf;
import static com.gh.bmd.jrt.android.v4.core.RoutineContext.contextFrom;
import static com.gh.bmd.jrt.builder.InvocationConfiguration.builder;
import static com.gh.bmd.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine unit tests.
 * <p/>
 * Created by davide-maestroni on 12/10/14.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderRoutineTest() {

        super(TestActivity.class);
    }

    public void testActivityAbort() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withInputClashResolution(ClashResolutionType.ABORT_THIS)
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THIS)
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

        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .with(builder().withInputOrder(OrderType.BY_CALL)
                                       .withOutputOrder(OrderType.BY_CALL)
                                       .set())
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test").eventually();
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.on(contextFrom(getActivity())).loaders().withId(0).set().purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityBuilderPurgeInputs() throws InterruptedException {

        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .with(builder().withInputOrder(OrderType.BY_CALL)
                                       .withOutputOrder(OrderType.BY_CALL)
                                       .set())
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test").eventually();
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.on(contextFrom(getActivity())).loaders().withId(0).set().purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.on(contextFrom(getActivity())).loaders().withId(0).set().purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.on(contextFrom(getActivity()))
                .loaders()
                .withId(0)
                .set()
                .purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityClearError() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.on(contextFrom(getActivity()), factoryFrom(
                Invocations.factoryOf(Abort.class)))
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

        final OutputChannel<Data> result2 =
                JRoutine.on(contextFrom(getActivity()), factoryOf(Delay.class))
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE_IF_SUCCESS)
                        .set()
                        .asyncCall(data1)
                        .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.checkComplete();

        final OutputChannel<Data> result3 =
                JRoutine.on(contextFrom(getActivity()), factoryOf(Delay.class))
                        .loaders()
                        .withId(0)
                        .set()
                        .asyncCall(data1)
                        .afterMax(timeout);

        assertThat(result3.next()).isSameAs(data1);
        result3.checkComplete();
    }

    public void testActivityClearResult() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.on(contextFrom(getActivity()), factoryOf(Delay.class))
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                        .set()
                        .asyncCall(data1)
                        .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        AbortException error = null;
        final OutputChannel<Data> result2 = JRoutine.on(contextFrom(getActivity()), factoryFrom(
                Invocations.factoryOf(Abort.class)))
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

        final OutputChannel<Data> result3 = JRoutine.on(contextFrom(getActivity()), factoryFrom(
                Invocations.factoryOf(Abort.class)))
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
        assertThat(JRoutine.on(factoryFrom(getActivity(), factoryOf(classToken))).syncCall().next())
                .isSameAs(getActivity());
        assertThat(JRoutine.on(contextFrom(getActivity()), factoryOf(classToken)).syncCall().next())
                .isSameAs(getActivity().getApplicationContext());
    }

    public void testActivityDelegation() {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutine.on(contextFrom(getActivity()), PassingContextInvocation.factoryOf())
                        .buildRoutine();
        final ContextInvocationFactory<Object, Object> factory =
                DelegatingContextInvocation.factoryFrom(routine1, "test_routine");
        final Routine<Object, Object> routine2 =
                JRoutine.on(contextFrom(getActivity()), factory).buildRoutine();

        assertThat(routine2.asyncCall("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.asyncInvoke().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().eventually().next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testActivityInputs() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(ToUpperCase.class))
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityInvalidIdError() {

        try {

            JRoutine.on(contextFrom(getActivity())).buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityInvalidTokenError() {

        try {

            JRoutine.on(contextFrom(new TestActivity()), factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityKeep() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.MERGE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final OutputChannel<String> channel =
                JRoutine.on(contextFrom(getActivity())).loaders().withId(0).set().buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final MissingInvocationException ignored) {

        }
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testActivityNullPointerErrors() {

        try {

            JRoutine.on((RoutineContext) null, factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(contextFrom(getActivity()), (ContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on((RoutineContext) null, ToUpperCase.class);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on((RoutineContext) null, ToUpperCase.class, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(contextFrom(getActivity()), (Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(contextFrom(getActivity()), null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on((RoutineContext) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testActivityRestart() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withInputClashResolution(ClashResolutionType.ABORT_THAT)
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THAT)
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.on(contextFrom(getActivity()), factoryOf(Delay.class))
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .asyncCall(data1)
                        .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.on(contextFrom(getActivity()), factoryOf(Delay.class))
                        .loaders()
                        .withId(0)
                        .set()
                        .asyncCall(data1)
                        .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.checkComplete();

        AbortException error = null;
        final OutputChannel<Data> result3 = JRoutine.on(contextFrom(getActivity()), factoryFrom(
                Invocations.factoryOf(Abort.class)))
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

        final OutputChannel<Data> result4 = JRoutine.on(contextFrom(getActivity()), factoryFrom(
                Invocations.factoryOf(Abort.class)))
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

        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .with(builder().withInputOrder(OrderType.BY_CALL)
                                       .withOutputOrder(OrderType.BY_CALL)
                                       .set())
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test").eventually();
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.checkComplete());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityRoutinePurgeInputs() throws InterruptedException {

        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test").eventually();
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivitySame() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine =
                JRoutine.on(contextFrom(getActivity()), factoryOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testChannelBuilderWarnings() {

        final CountLog countLog = new CountLog();
        final InvocationConfiguration configuration =
                builder().withAsyncRunner(Runners.taskRunner())
                         .withInputMaxSize(3)
                         .withInputTimeout(seconds(10))
                         .withOutputMaxSize(3)
                         .withOutputTimeout(seconds(10))
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(countLog)
                         .set();
        JRoutine.on(contextFrom(getActivity()))
                .loaders()
                .withId(0)
                .set()
                .invocations()
                .with(configuration)
                .set()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.on(contextFrom(fragment))
                .loaders()
                .withId(0)
                .set()
                .invocations()
                .with(configuration)
                .set()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    public void testClash() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.on(contextFrom(getActivity()), factoryOf(Delay.class))
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .asyncCall(data1)
                        .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 = JRoutine.on(contextFrom(getActivity()), factoryFrom(
                Invocations.factoryOf(Abort.class)))
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

        final ContextInvocationFactory<Object, Object> factory =
                PassingContextInvocation.factoryOf();

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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THIS)
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
        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel4 = routine.asyncCall("test").eventually();
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.on(contextFrom(fragment)).loaders().withId(0).set().purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentBuilderPurgeInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel5 = routine.asyncCall("test").eventually();
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.on(contextFrom(fragment)).loaders().withId(0).set().purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.on(contextFrom(fragment)).loaders().withId(0).set().purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.on(contextFrom(fragment))
                .loaders()
                .withId(0)
                .set()
                .purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentChannel() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .set()
                        .invocations()
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test1", "test2");
        final OutputChannel<String> channel2 =
                JRoutine.on(contextFrom(fragment)).loaders().withId(0).set().buildChannel();

        assertThat(channel1.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testFragmentDelegation() {

        final TimeDuration timeout = seconds(1);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine1 =
                JRoutine.on(contextFrom(fragment), PassingContextInvocation.factoryOf())
                        .buildRoutine();
        final ContextInvocationFactory<Object, Object> factory =
                DelegatingContextInvocation.factoryFrom(routine1, "test_routine");
        final Routine<Object, Object> routine2 =
                JRoutine.on(contextFrom(fragment), factory).buildRoutine();

        assertThat(routine2.asyncCall("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.asyncInvoke().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().eventually().next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testFragmentInputs() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(ToUpperCase.class)).buildRoutine();
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
            JRoutine.on(contextFrom(fragment)).buildChannel();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentInvalidTokenError() {

        try {

            JRoutine.on(contextFrom(new TestFragment()), factoryOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentKeep() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.MERGE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.asyncCall("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.asyncCall("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testFragmentMissingRoutine() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> channel =
                JRoutine.on(contextFrom(fragment)).loaders().withId(0).set().buildChannel();

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

            JRoutine.on((RoutineContext) null, factoryOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(contextFrom(fragment), (ContextInvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on((RoutineContext) null, ToUpperCase.class);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on((RoutineContext) null, ToUpperCase.class, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(contextFrom(fragment), (Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(contextFrom(fragment), null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on((RoutineContext) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentReset() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withInputClashResolution(ClashResolutionType.ABORT_THAT)
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(ToUpperCase.class))
                        .loaders()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THAT)
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
        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel = routine.asyncCall("test").eventually();
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.checkComplete());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentRoutinePurgeInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(PurgeContextInvocation.class))
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
                        .loaders()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.asyncCall("test").eventually();
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.asyncCall("test1", "test2").eventually();
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentSame() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Data, Data> routine =
                JRoutine.on(contextFrom(fragment), factoryOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.asyncCall(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.asyncCall(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testInvocations() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine1 = JRoutine.on(contextFrom(getActivity()),
                                                             PassingContextInvocation
                                                                     .<String>factoryOf())
                                                         .invocations()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withLog(Logs.androidLog())
                                                         .withLogLevel(LogLevel.WARNING)
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
        final Routine<String, String> routine2 =
                JRoutine.on(contextFrom(getActivity()), factoryOf(token2))
                        .invocations()
                        .withSyncRunner(Runners.queuedRunner())
                        .withLog(Logs.androidLog())
                        .withLogLevel(LogLevel.WARNING)
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
        final RoutineContext context = contextFrom(getActivity());

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
                builder().withAsyncRunner(Runners.taskRunner())
                         .withInputMaxSize(3)
                         .withInputTimeout(seconds(10))
                         .withOutputMaxSize(3)
                         .withOutputTimeout(seconds(10))
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(countLog)
                         .set();
        JRoutine.on(contextFrom(getActivity()), factoryOf(ToUpperCase.class))
                .invocations()
                .with(configuration)
                .set()
                .loaders()
                .withId(0)
                .withInputClashResolution(ClashResolutionType.MERGE)
                .set()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.on(contextFrom(fragment), factoryOf(ToUpperCase.class))
                .invocations()
                .with(configuration)
                .set()
                .loaders()
                .withId(0)
                .withInputClashResolution(ClashResolutionType.MERGE)
                .set()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        final LoaderConfiguration configuration = LoaderConfiguration.DEFAULT_CONFIGURATION;
        final RoutineContext context = contextFrom(getActivity());

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

    private static class Abort extends TemplateInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

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

        public void dbg(@Nonnull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mDgbCount;
        }

        public void err(@Nonnull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mErrCount;
        }

        public void wrn(@Nonnull final List<Object> contexts, @Nullable final String message,
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

    private static class Delay extends TemplateContextInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(d);
        }
    }

    @SuppressWarnings("unused")
    private static class ErrorInvocation extends TemplateContextInvocation<String, String> {

        private ErrorInvocation(final int ignored) {

        }
    }

    private static class GetContextInvocation<DATA>
            extends TemplateContextInvocation<DATA, Context> {

        @Override
        public void onResult(@Nonnull final ResultChannel<Context> result) {

            result.pass(getContext());
        }
    }

    private static class PurgeContextInvocation extends TemplateContextInvocation<String, String> {

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
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.pass(s);
        }
    }

    private static class StringFunctionInvocation
            extends FunctionContextInvocation<String, String> {

        @Override
        public void onCall(@Nonnull final List<? extends String> strings,
                @Nonnull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class ToUpperCase extends TemplateContextInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(500)).pass(s.toUpperCase());
        }
    }
}
