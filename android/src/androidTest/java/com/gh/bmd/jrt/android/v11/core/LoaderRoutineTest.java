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
package com.gh.bmd.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.R;
import com.gh.bmd.jrt.android.builder.InvocationClashException;
import com.gh.bmd.jrt.android.builder.InvocationMissingException;
import com.gh.bmd.jrt.android.builder.InvocationTypeException;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.ContextInvocations;
import com.gh.bmd.jrt.android.invocation.DelegatingContextInvocation;
import com.gh.bmd.jrt.android.invocation.PassingContextInvocation;
import com.gh.bmd.jrt.android.invocation.ProcedureContextInvocation;
import com.gh.bmd.jrt.android.invocation.TemplateContextInvocation;
import com.gh.bmd.jrt.android.log.Logs;
import com.gh.bmd.jrt.android.routine.LoaderRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.common.Reflection;
import com.gh.bmd.jrt.invocation.Invocations.Function;
import com.gh.bmd.jrt.invocation.TemplateInvocation;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.invocation.ContextInvocations.factoryOf;
import static com.gh.bmd.jrt.builder.InvocationConfiguration.builder;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine unit tests.
 * <p/>
 * Created by davide-maestroni on 12/10/14.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class LoaderRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderRoutineTest() {

        super(TestActivity.class);
    }

    public void testActivityAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withInputClashResolution(ClashResolutionType.ABORT_THIS)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test1").afterMax(timeout);

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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THIS)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

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

        final LoaderRoutine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .with(builder().withInputOrder(OrderType.PASS_ORDER)
                                       .withOutputOrder(OrderType.PASS_ORDER)
                                       .set())
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel4 = routine.callAsync("test").eventually();
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityBuilderPurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderRoutine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .with(builder().withInputOrder(OrderType.PASS_ORDER)
                                       .withOutputOrder(OrderType.PASS_ORDER)
                                       .set())
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel5 = routine.callAsync("test").eventually();
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityClearError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.onActivity(getActivity(),
                                                                ContextInvocations.contextFactoryOf(
                                                                        ClassToken.tokenOf(
                                                                                Abort.class)))
                                                    .withLoader()
                                                    .withId(0)
                                                    .withCacheStrategy(
                                                            CacheStrategyType.CACHE_IF_SUCCESS)
                                                    .set()
                                                    .callAsync(data1)
                                                    .afterMax(timeout);

        try {

            result1.next();

            fail();

        } catch (final AbortException ignored) {

        }

        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE_IF_SUCCESS)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.checkComplete();

        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withLoader()
                        .withId(0)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result3.next()).isSameAs(data1);
        result3.checkComplete();
    }

    public void testActivityClearResult() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        AbortException error = null;
        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ContextInvocations.contextFactoryOf(Abort.class))
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result2.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result2.checkComplete();

        final OutputChannel<Data> result3 = JRoutine.onActivity(getActivity(),
                                                                ContextInvocations.contextFactoryOf(
                                                                        ClassToken.tokenOf(
                                                                                Abort.class)))
                                                    .withLoader()
                                                    .withId(0)
                                                    .set()
                                                    .callAsync(data1)
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        assertThat(JRoutine.on(ContextInvocations.invocationFactoryOf(
                new ClassToken<GetContextInvocation<String>>() {}, getActivity()))
                           .callSync()
                           .next()).isSameAs(getActivity());
        assertThat(JRoutine.onActivity(getActivity(), ContextInvocations.factoryOf(
                new ClassToken<GetContextInvocation<String>>() {})).callSync().next()).isSameAs(
                getActivity().getApplicationContext());
    }

    public void testActivityDelegation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutine.onActivity(getActivity(), PassingContextInvocation.factoryOf())
                        .buildRoutine();
        final ContextInvocationFactory<Object, Object> factory =
                DelegatingContextInvocation.factoryWith(routine1, "test_routine");
        final Routine<Object, Object> routine2 =
                JRoutine.onActivity(getActivity(), factory).buildRoutine();

        assertThat(routine2.callAsync("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.invokeAsync().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testActivityFunction() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<Object, String> routine =
                JRoutine.onActivity(getActivity(), factoryOf(new TestFunction())).buildRoutine();
        assertThat(routine.callAsync("test").afterMax(timeout).next()).isEqualTo("TEST");
    }

    public void testActivityInputs() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST2");
    }

    public void testActivityInvalidIdError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), LoaderConfiguration.AUTO);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testActivityInvalidTokenError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(new TestActivity(), ClassToken.tokenOf(ErrorInvocation.class))
                    .callAsync()
                    .eventually()
                    .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testActivityKeep() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.MERGE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final OutputChannel<String> channel = JRoutine.onActivity(getActivity(), 0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final InvocationMissingException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testActivityNullPointerErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(null, ClassToken.tokenOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(),
                                (ClassToken<ContextInvocation<Object, Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(null, 0);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testActivityRestart() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withInputClashResolution(ClashResolutionType.ABORT_THAT)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test1").afterMax(timeout);

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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THAT)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withLoader()
                        .withId(0)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result2.next()).isSameAs(data1);
        result2.checkComplete();

        AbortException error = null;
        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ContextInvocations.contextFactoryOf(Abort.class))
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result3.next();

            fail();

        } catch (final AbortException e) {

            error = e;
        }

        result3.checkComplete();

        final OutputChannel<Data> result4 =
                JRoutine.onActivity(getActivity(), ContextInvocations.contextFactoryOf(Abort.class))
                        .withLoader()
                        .withId(0)
                        .set()
                        .callAsync(data1)
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderRoutine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .with(builder().withInputOrder(OrderType.PASS_ORDER)
                                       .withOutputOrder(OrderType.PASS_ORDER)
                                       .set())
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel = routine.callAsync("test").eventually();
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.checkComplete());
        routine.purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityRoutinePurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderRoutine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withOutputOrder(OrderType.PASS_ORDER)
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.callAsync("test").eventually();
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivitySame() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final Routine<Data, Data> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.callAsync(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.callAsync(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testChannelBuilderWarnings() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

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
        JRoutine.onActivity(getActivity(), 0)
                .withInvocation()
                .with(configuration)
                .set()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(5);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.onFragment(fragment, 0).withInvocation().with(configuration).set().buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(10);
    }

    public void testClash() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ContextInvocations.contextFactoryOf(Abort.class))
                        .withLoader()
                        .withId(0)
                        .set()
                        .callAsync(data1)
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ContextInvocationFactory<Object, Object> factory =
                PassingContextInvocation.factoryOf();

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(getActivity(),
                                                            factory).setConfiguration(
                    (InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutineBuilder<Object, Object>(getActivity(),
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THIS)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

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
        final LoaderRoutine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withOutputOrder(OrderType.PASS_ORDER)
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel4 = routine.callAsync("test").eventually();
        assertThat(channel4.next()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.onFragment(fragment, 0).purge();
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentBuilderPurgeInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderRoutine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withOutputOrder(OrderType.PASS_ORDER)
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel5 = routine.callAsync("test").eventually();
        assertThat(channel5.next()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.onFragment(fragment, 0).purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel6.all()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.onFragment(fragment, 0).purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel7.all()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.onFragment(fragment, 0).purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentChannel() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .set()
                        .withInvocation()
                        .withOutputOrder(OrderType.PASS_ORDER)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.callAsync("test1", "test2");
        final OutputChannel<String> channel2 = JRoutine.onFragment(fragment, 0).buildChannel();

        assertThat(channel1.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.afterMax(timeout).all()).containsExactly("TEST1", "TEST2");
    }

    public void testFragmentDelegation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(1);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine1 =
                JRoutine.onFragment(fragment, PassingContextInvocation.factoryOf()).buildRoutine();
        final ContextInvocationFactory<Object, Object> factory =
                DelegatingContextInvocation.factoryWith(routine1, "test_routine");
        final Routine<Object, Object> routine2 =
                JRoutine.onFragment(fragment, factory).buildRoutine();

        assertThat(routine2.callAsync("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.invokeAsync().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testFragmentFunction() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, String> routine =
                JRoutine.onFragment(fragment, factoryOf(new TestFunction())).buildRoutine();
        assertThat(routine.callAsync("test").afterMax(timeout).next()).isEqualTo("TEST");
    }

    public void testFragmentInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class)).buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

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
            JRoutine.onFragment(fragment, LoaderConfiguration.AUTO);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testFragmentInvalidTokenError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onFragment(new TestFragment(), ClassToken.tokenOf(ErrorInvocation.class))
                    .callAsync()
                    .eventually()
                    .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testFragmentKeep() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.MERGE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        assertThat(result1.next()).isEqualTo("TEST1");
        assertThat(result2.next()).isEqualTo("TEST1");
    }

    public void testFragmentMissingRoutine() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> channel = JRoutine.onFragment(fragment, 0).buildChannel();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final InvocationMissingException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testFragmentNullPointerErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onFragment(null, ClassToken.tokenOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFragment(null, 0);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                      .findFragmentById(
                                                                              R.id.test_fragment);
            JRoutine.onFragment(fragment, (ClassToken<ContextInvocation<Object, Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentReset() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withInputClashResolution(ClashResolutionType.ABORT_THAT)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test1").afterMax(timeout);

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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withLoader()
                        .withId(0)
                        .withClashResolution(ClashResolutionType.ABORT_THAT)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

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
        final LoaderRoutine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withOutputOrder(OrderType.PASS_ORDER)
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel = routine.callAsync("test").eventually();
        assertThat(channel.next()).isEqualTo("test");
        assertThat(channel.checkComplete());
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
        final LoaderRoutine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(PurgeContextInvocation.class))
                        .withInvocation()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withOutputOrder(OrderType.PASS_ORDER)
                        .set()
                        .withLoader()
                        .withId(0)
                        .withCacheStrategy(CacheStrategyType.CACHE)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.callAsync("test").eventually();
        assertThat(channel1.next()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel2.all()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel3.all()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeContextInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testFragmentSame() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Data, Data> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.callAsync(data1).afterMax(timeout);
        final OutputChannel<Data> result2 = routine.callAsync(data1).afterMax(timeout);

        assertThat(result1.next()).isSameAs(data1);
        assertThat(result2.next()).isSameAs(data1);
    }

    public void testInvocations() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine1 =
                JRoutine.onActivity(getActivity(), PassingContextInvocation.<String>factoryOf())
                        .withInvocation()
                        .withSyncRunner(Runners.queuedRunner())
                        .withLog(Logs.androidLog())
                        .withLogLevel(LogLevel.WARNING)
                        .set()
                        .buildRoutine();
        assertThat(routine1.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine1.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");

        final ClassToken<StringProcedureInvocation> token2 =
                ClassToken.tokenOf(StringProcedureInvocation.class);
        final Routine<String, String> routine2 = JRoutine.onActivity(getActivity(), token2)
                                                         .withInvocation()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withLog(Logs.androidLog())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .set()
                                                         .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine2.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5")
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
        final WeakReference<Object> reference = new WeakReference<Object>(getActivity());

        try {

            new LoaderInvocation<String, String>(null, factoryOf(ToUpperCase.class),
                                                 Reflection.NO_ARGS, configuration, null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, null, Reflection.NO_ARGS, configuration,
                                                 null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, factoryOf(ToUpperCase.class), null,
                                                 configuration, null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, factoryOf(ToUpperCase.class),
                                                 Reflection.NO_ARGS, null, null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, factoryOf(ToUpperCase.class),
                                                 Reflection.NO_ARGS, configuration, null, null);

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
                builder().withAsyncRunner(Runners.taskRunner())
                         .withInputMaxSize(3)
                         .withInputTimeout(seconds(10))
                         .withOutputMaxSize(3)
                         .withOutputTimeout(seconds(10))
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(countLog)
                         .set();
        JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                .withInvocation()
                .with(configuration)
                .set()
                .withLoader()
                .withId(0)
                .withInputClashResolution(ClashResolutionType.MERGE)
                .set()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(5);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                .withInvocation()
                .with(configuration)
                .set()
                .withLoader()
                .withId(0)
                .withInputClashResolution(ClashResolutionType.MERGE)
                .set()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(10);
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderConfiguration configuration = LoaderConfiguration.DEFAULT_CONFIGURATION;
        final WeakReference<Object> reference = new WeakReference<Object>(getActivity());

        try {

            new DefaultLoaderRoutine<String, String>(null, factoryOf(ToUpperCase.class),
                                                     InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     configuration);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutine<String, String>(reference, null,
                                                     InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     configuration);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutine<String, String>(reference, factoryOf(ToUpperCase.class), null,
                                                     configuration);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderRoutine<String, String>(reference, factoryOf(ToUpperCase.class),
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

    private static class StringProcedureInvocation
            extends ProcedureContextInvocation<String, String> {

        @Override
        public void onCall(@Nonnull final List<? extends String> strings,
                @Nonnull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class TestFunction implements Function<String> {

        public String call(@Nonnull final Object... params) {

            return params[0].toString().toUpperCase();
        }
    }

    private static class ToUpperCase extends TemplateContextInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(500)).pass(s.toUpperCase());
        }
    }
}
