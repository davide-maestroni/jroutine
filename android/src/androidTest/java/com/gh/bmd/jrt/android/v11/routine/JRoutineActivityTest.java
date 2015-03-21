/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.v11.routine;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.R;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.CacheStrategy;
import com.gh.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.gh.bmd.jrt.android.builder.InputClashException;
import com.gh.bmd.jrt.android.builder.InvocationClashException;
import com.gh.bmd.jrt.android.builder.InvocationMissingException;
import com.gh.bmd.jrt.android.invocation.AndroidPassingInvocation;
import com.gh.bmd.jrt.android.invocation.AndroidSingleCallInvocation;
import com.gh.bmd.jrt.android.invocation.AndroidTemplateInvocation;
import com.gh.bmd.jrt.android.log.Logs;
import com.gh.bmd.jrt.android.routine.AndroidRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.RunnerType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
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

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withOutputOrder;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JRoutine activity unit tests.
 * <p/>
 * Created by davide on 12/10/14.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class JRoutineActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public JRoutineActivityTest() {

        super(TestActivity.class);
    }

    public void testActivityAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.ABORT_THIS)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test1").afterMax(timeout);

        assertThat(result1.readNext()).isEqualTo("TEST1");

        try {

            result2.readNext();

            fail();

        } catch (final InputClashException ignored) {

        }
    }

    public void testActivityAbortInput() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.ABORT_THIS_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        assertThat(result1.readNext()).isEqualTo("TEST1");

        try {

            result2.readNext();

            fail();

        } catch (final InputClashException ignored) {

        }
    }

    public void testActivityClearError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE_IF_SUCCESS)
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result1.readNext();

            fail();

        } catch (final InvocationException ignored) {

        }

        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE_IF_SUCCESS)
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result2.readNext()).isSameAs(data1);
        result2.checkComplete();

        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result3.readNext()).isSameAs(data1);
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
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE_IF_ERROR)
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result1.readNext()).isSameAs(data1);
        result1.checkComplete();

        InvocationException error = null;
        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE_IF_ERROR)
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result2.readNext();

            fail();

        } catch (final InvocationException e) {

            error = e;
        }

        result2.checkComplete();

        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result3.readNext();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result3.checkComplete();
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

        assertThat(result1.readNext()).isEqualTo("TEST1");
        assertThat(result2.readNext()).isEqualTo("TEST2");
    }

    public void testActivityKeep() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.KEEP_THAT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        assertThat(result1.readNext()).isEqualTo("TEST1");
        assertThat(result2.readNext()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final OutputChannel<String> channel = JRoutine.onActivity(getActivity(), 0).buildChannel();

        try {

            channel.afterMax(timeout).readAll();

            fail();

        } catch (final InvocationMissingException ignored) {

        }
    }

    public void testActivityPurge() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final AndroidRoutine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(PurgeAndroidInvocation.class))
                        .withConfiguration(builder().withInputOrder(OrderType.PASSING)
                                                    .withOutputOrder(OrderType.PASSING)
                                                    .buildConfiguration())
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE)
                        .buildRoutine();
        final OutputChannel<String> channel = routine.callAsync("test").eventually();
        assertThat(channel.readNext()).isEqualTo("test");
        assertThat(channel.checkComplete());
        routine.purge();
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel1 = routine.callAsync("test").eventually();
        assertThat(channel1.readNext()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel2.readAll()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel3.readAll()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel4 = routine.callAsync("test").eventually();
        assertThat(channel4.readNext()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge();
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel5 = routine.callAsync("test").eventually();
        assertThat(channel5.readNext()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge("test");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel6.readAll()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge("test1", "test2");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel7.readAll()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.onActivity(getActivity(), 0).purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();
    }

    public void testActivityRestart() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.ABORT_THAT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test1").afterMax(timeout);

        try {

            result1.readNext();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.readNext()).isEqualTo("TEST1");
    }

    public void testActivityRestartOnInput() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.ABORT_THAT_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        try {

            result1.readNext();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.readNext()).isEqualTo("TEST2");
    }

    public void testActivityRetain() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE)
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result1.readNext()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result2.readNext()).isSameAs(data1);
        result2.checkComplete();

        InvocationException error = null;
        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE)
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result3.readNext();

            fail();

        } catch (final InvocationException e) {

            error = e;
        }

        result3.checkComplete();

        final OutputChannel<Data> result4 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .buildRoutine()
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result4.readNext();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result4.checkComplete();
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

        assertThat(result1.readNext()).isSameAs(data1);
        assertThat(result2.readNext()).isSameAs(data1);
    }

    public void testAndroidChannelBuilderWarnings() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final CountLog countLog = new CountLog();
        final RoutineConfiguration configuration = builder().withRunner(Runners.taskRunner())
                                                            .withInputSize(3)
                                                            .withInputTimeout(seconds(1))
                                                            .withOutputSize(3)
                                                            .withOutputTimeout(seconds(1))
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(countLog)
                                                            .buildConfiguration();
        JRoutine.onActivity(getActivity(), 0).withConfiguration(configuration).buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(5);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.onFragment(fragment, 0).withConfiguration(configuration).buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(10);
    }

    public void testAndroidRoutineBuilderWarnings() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final CountLog countLog = new CountLog();
        final RoutineConfiguration configuration = builder().withRunner(Runners.taskRunner())
                                                            .withInputSize(3)
                                                            .withInputTimeout(seconds(1))
                                                            .withOutputSize(3)
                                                            .withOutputTimeout(seconds(1))
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(countLog)
                                                            .buildConfiguration();
        JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                .withConfiguration(configuration)
                .withId(0)
                .onClash(ClashResolution.KEEP_THAT)
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(5);

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                .withConfiguration(configuration)
                .withId(0)
                .onClash(ClashResolution.KEEP_THAT)
                .buildRoutine();
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
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE)
                        .callAsync(data1)
                        .afterMax(timeout);

        assertThat(result1.readNext()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .callAsync(data1)
                        .afterMax(timeout);

        try {

            result2.readNext();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        result2.checkComplete();
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testErrors() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(null, ClassToken.tokenOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(null, 0);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), AndroidRoutineBuilder.AUTO);

            fail();

        } catch (final IllegalArgumentException ignored) {

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
            JRoutine.onFragment(fragment, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                      .findFragmentById(
                                                                              R.id.test_fragment);
            JRoutine.onFragment(fragment, AndroidRoutineBuilder.AUTO);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(new TestActivity(), ClassToken.tokenOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(new TestFragment(), ClassToken.tokenOf(ErrorInvocation.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

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
                        .withId(0)
                        .onClash(ClashResolution.ABORT_THIS_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        assertThat(result1.readNext()).isEqualTo("TEST1");

        try {

            result2.readNext();

            fail();

        } catch (final InputClashException ignored) {

        }
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
                        .withId(0)
                        .withConfiguration(withOutputOrder(OrderType.PASSING))
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.callAsync("test1", "test2");
        final OutputChannel<String> channel2 = JRoutine.onFragment(fragment, 0).buildChannel();

        assertThat(channel1.afterMax(timeout).readAll()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.afterMax(timeout).readAll()).containsExactly("TEST1", "TEST2");
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

        assertThat(result1.readNext()).isEqualTo("TEST1");
        assertThat(result2.readNext()).isEqualTo("TEST2");
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
                        .withId(0)
                        .onClash(ClashResolution.KEEP_THAT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        assertThat(result1.readNext()).isEqualTo("TEST1");
        assertThat(result2.readNext()).isEqualTo("TEST1");
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

            channel.afterMax(timeout).readAll();

            fail();

        } catch (final InvocationMissingException ignored) {

        }
    }

    public void testFragmentPurge() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final AndroidRoutine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(PurgeAndroidInvocation.class))
                        .withConfiguration(builder().withInputOrder(OrderType.PASSING)
                                                    .withOutputOrder(OrderType.PASSING)
                                                    .buildConfiguration())
                        .withId(0)
                        .onComplete(CacheStrategy.CACHE)
                        .buildRoutine();
        final OutputChannel<String> channel = routine.callAsync("test").eventually();
        assertThat(channel.readNext()).isEqualTo("test");
        assertThat(channel.checkComplete());
        routine.purge();
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel1 = routine.callAsync("test").eventually();
        assertThat(channel1.readNext()).isEqualTo("test");
        assertThat(channel1.checkComplete());
        routine.purge("test");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel2 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel2.readAll()).containsExactly("test1", "test2");
        assertThat(channel2.checkComplete());
        routine.purge("test1", "test2");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel3 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel3.readAll()).containsExactly("test1", "test2");
        assertThat(channel3.checkComplete());
        routine.purge(Arrays.asList("test1", "test2"));
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel4 = routine.callAsync("test").eventually();
        assertThat(channel4.readNext()).isEqualTo("test");
        assertThat(channel4.checkComplete());
        JRoutine.onFragment(fragment, 0).purge();
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel5 = routine.callAsync("test").eventually();
        assertThat(channel5.readNext()).isEqualTo("test");
        assertThat(channel5.checkComplete());
        JRoutine.onFragment(fragment, 0).purge("test");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel6 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel6.readAll()).containsExactly("test1", "test2");
        assertThat(channel6.checkComplete());
        JRoutine.onFragment(fragment, 0).purge("test1", "test2");
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();

        final OutputChannel<String> channel7 = routine.callAsync("test1", "test2").eventually();
        assertThat(channel7.readAll()).containsExactly("test1", "test2");
        assertThat(channel7.checkComplete());
        JRoutine.onFragment(fragment, 0).purge(Arrays.asList((Object) "test1", "test2"));
        assertThat(PurgeAndroidInvocation.waitDestroy(1, 1000)).isTrue();
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
                        .withId(0)
                        .onClash(ClashResolution.ABORT_THAT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test1").afterMax(timeout);

        try {

            result1.readNext();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.readNext()).isEqualTo("TEST1");
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
                        .withId(0)
                        .onClash(ClashResolution.ABORT_THAT_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1").afterMax(timeout);
        final OutputChannel<String> result2 = routine.callAsync("test2").afterMax(timeout);

        try {

            result1.readNext();

            fail();

        } catch (final InvocationClashException ignored) {

        }

        assertThat(result2.readNext()).isEqualTo("TEST2");
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

        assertThat(result1.readNext()).isSameAs(data1);
        assertThat(result2.readNext()).isSameAs(data1);
    }

    public void testInvocations() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = TimeDuration.seconds(10);
        final ClassToken<StringPassingInvocation> token1 =
                ClassToken.tokenOf(StringPassingInvocation.class);
        final RoutineConfiguration configuration1 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withLog(Logs.androidLog())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .buildConfiguration();
        final Routine<String, String> routine1 = JRoutine.onActivity(getActivity(), token1)
                                                         .withConfiguration(configuration1)
                                                         .buildRoutine();
        assertThat(routine1.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");

        final ClassToken<StringSingleCallInvocation> token2 =
                ClassToken.tokenOf(StringSingleCallInvocation.class);
        final RoutineConfiguration configuration2 = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                             .withLog(Logs.androidLog())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .buildConfiguration();
        final Routine<String, String> routine2 = JRoutine.onActivity(getActivity(), token2)
                                                         .withConfiguration(configuration2)
                                                         .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine2.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLoaderError() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Logger logger = Logger.newLogger(null, null, this);
        final WeakReference<Object> reference = new WeakReference<Object>(getActivity());

        try {

            new LoaderInvocation<String, String>(null, 0, ClashResolution.KEEP_THAT,
                                                 CacheStrategy.CACHE,
                                                 ToUpperCase.class.getDeclaredConstructor(), null,
                                                 logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, 0, ClashResolution.KEEP_THAT, null,
                                                 null, null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, 0, ClashResolution.KEEP_THAT,
                                                 CacheStrategy.CACHE,
                                                 ToUpperCase.class.getDeclaredConstructor(), null,
                                                 null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final WeakReference<Object> reference = new WeakReference<Object>(getActivity());

        try {

            new DefaultAndroidRoutine<String, String>(null, reference, 0, ClashResolution.KEEP_THAT,
                                                      CacheStrategy.CACHE,
                                                      ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultAndroidRoutine<String, String>(RoutineConfiguration.EMPTY_CONFIGURATION,
                                                      null, 0, ClashResolution.KEEP_THAT,
                                                      CacheStrategy.CACHE,
                                                      ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultAndroidRoutine<String, String>(RoutineConfiguration.EMPTY_CONFIGURATION,
                                                      reference, 0, ClashResolution.KEEP_THAT, null,
                                                      null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class Abort extends AndroidTemplateInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException e) {

                throw InvocationInterruptedException.interrupt(e);
            }

            result.abort(new IllegalStateException());
        }
    }

    @SuppressWarnings("UnusedDeclaration")
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

    private static class Delay extends AndroidTemplateInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(d);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class ErrorInvocation extends AndroidTemplateInvocation<String, String> {

        private ErrorInvocation(final int ignored) {

        }
    }

    private static class PurgeAndroidInvocation extends AndroidPassingInvocation<String> {

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
    }

    private static class StringPassingInvocation extends AndroidPassingInvocation<String> {

    }

    private static class StringSingleCallInvocation
            extends AndroidSingleCallInvocation<String, String> {

        @Override
        public void onCall(@Nonnull final List<? extends String> strings,
                @Nonnull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class ToUpperCase extends AndroidTemplateInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(500)).pass(s.toUpperCase());
        }
    }
}
