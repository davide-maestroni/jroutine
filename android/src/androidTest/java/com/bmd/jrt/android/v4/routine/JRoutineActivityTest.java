/**
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
package com.bmd.jrt.android.v4.routine;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.bmd.jrt.android.R;
import com.bmd.jrt.android.builder.AndroidRoutineBuilder;
import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ClashResolution;
import com.bmd.jrt.android.builder.AndroidRoutineBuilder.ResultCache;
import com.bmd.jrt.android.builder.InputClashException;
import com.bmd.jrt.android.builder.RoutineClashException;
import com.bmd.jrt.android.builder.RoutineMissingException;
import com.bmd.jrt.android.invocation.AndroidSimpleInvocation;
import com.bmd.jrt.android.invocation.AndroidTemplateInvocation;
import com.bmd.jrt.android.invocation.AndroidTunnelInvocation;
import com.bmd.jrt.android.log.Logs;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JRoutine activity unit tests.
 * <p/>
 * Created by davide on 12/10/14.
 */
@TargetApi(VERSION_CODES.FROYO)
public class JRoutineActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public JRoutineActivityTest() {

        super(TestActivity.class);
    }

    public void testActivityAbort() {

        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.ABORT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test1");

        assertThat(result1.readFirst()).isEqualTo("TEST1");

        try {

            result2.readFirst();

            fail();

        } catch (final InputClashException ignored) {

        }
    }

    public void testActivityAbortInput() {

        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.ABORT_ON_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        assertThat(result1.readFirst()).isEqualTo("TEST1");

        try {

            result2.readFirst();

            fail();

        } catch (final InputClashException ignored) {

        }
    }

    public void testActivityClearError() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .onComplete(ResultCache.RETAIN_RESULT)
                        .buildRoutine()
                        .callAsync(data1);

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .onComplete(ResultCache.RETAIN_RESULT)
                        .buildRoutine()
                        .callAsync(data1);

        assertThat(result2.readFirst()).isSameAs(data1);
        result2.checkComplete();

        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .buildRoutine()
                        .callAsync(data1);

        assertThat(result3.readFirst()).isSameAs(data1);
        result3.checkComplete();
    }

    public void testActivityClearResult() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .onComplete(ResultCache.RETAIN_ERROR)
                        .buildRoutine()
                        .callAsync(data1);

        assertThat(result1.readFirst()).isSameAs(data1);
        result1.checkComplete();

        RoutineException error = null;
        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .onComplete(ResultCache.RETAIN_ERROR)
                        .buildRoutine()
                        .callAsync(data1);

        try {

            result2.readFirst();

            fail();

        } catch (final RoutineException e) {

            error = e;
        }

        result2.checkComplete();

        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .buildRoutine()
                        .callAsync(data1);

        try {

            result3.readFirst();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result3.checkComplete();
    }

    public void testActivityInputs() {

        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testActivityKeep() {

        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.KEEP)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST1");
    }

    public void testActivityMissingRoutine() throws InterruptedException {

        final OutputChannel<String> channel = JRoutine.onActivity(getActivity(), 0).buildChannel();

        try {

            channel.readAll();

            fail();

        } catch (final RoutineMissingException ignored) {

        }
    }

    public void testActivityRestart() {

        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.RESTART)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test1");

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        assertThat(result2.readFirst()).isEqualTo("TEST1");
    }

    public void testActivityRestartOnInput() {

        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.RESTART_ON_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testActivityRetain() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .onComplete(ResultCache.RETAIN)
                        .buildRoutine()
                        .callAsync(data1);

        assertThat(result1.readFirst()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .buildRoutine()
                        .callAsync(data1);

        assertThat(result2.readFirst()).isSameAs(data1);
        result2.checkComplete();

        RoutineException error = null;
        final OutputChannel<Data> result3 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .onComplete(ResultCache.RETAIN)
                        .buildRoutine()
                        .callAsync(data1);

        try {

            result3.readFirst();

            fail();

        } catch (final RoutineException e) {

            error = e;
        }

        result3.checkComplete();

        final OutputChannel<Data> result4 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .buildRoutine()
                        .callAsync(data1);

        try {

            result4.readFirst();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isSameAs(error.getCause());
        }

        result4.checkComplete();
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testActivityRotationChannel() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<String, String> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .outputOrder(DataOrder.INSERTION)
                        .buildRoutine();
        routine.callAsync("test1", "test2");

        final Semaphore semaphore = new Semaphore(0);

        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {

                getActivity().recreate();
                semaphore.release();
            }
        });

        semaphore.acquire();
        getInstrumentation().waitForIdleSync();

        final OutputChannel<String> channel = JRoutine.onActivity(getActivity(), 0).buildChannel();

        assertThat(channel.readAll()).containsExactly("TEST1", "TEST2");
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testActivityRotationInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<String, String> routine1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .buildRoutine();
        routine1.callAsync("test1");
        routine1.callAsync("test2");

        final Semaphore semaphore = new Semaphore(0);

        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {

                getActivity().recreate();
                semaphore.release();
            }
        });

        semaphore.acquire();
        getInstrumentation().waitForIdleSync();

        final Routine<String, String> routine2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                        .buildRoutine();
        final OutputChannel<String> result1 = routine2.callAsync("test1");
        final OutputChannel<String> result2 = routine2.callAsync("test2");

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testActivityRotationSame() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Data data1 = new Data();
        final Routine<Data, Data> routine1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class)).buildRoutine();
        routine1.callAsync(data1);
        routine1.callAsync(data1);

        final Semaphore semaphore = new Semaphore(0);

        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {

                getActivity().recreate();
                semaphore.release();
            }
        });

        semaphore.acquire();
        getInstrumentation().waitForIdleSync();

        final Routine<Data, Data> routine2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine2.callAsync(data1);
        final OutputChannel<Data> result2 = routine2.callAsync(data1);

        assertThat(result1.readFirst()).isSameAs(data1);
        assertThat(result2.readFirst()).isSameAs(data1);
    }

    public void testActivitySame() {

        final Data data1 = new Data();
        final Routine<Data, Data> routine =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.callAsync(data1);
        final OutputChannel<Data> result2 = routine.callAsync(data1);

        assertThat(result1.readFirst()).isSameAs(data1);
        assertThat(result2.readFirst()).isSameAs(data1);
    }

    public void testClash() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withId(0)
                        .onComplete(ResultCache.RETAIN)
                        .buildRoutine()
                        .callAsync(data1);

        assertThat(result1.readFirst()).isSameAs(data1);
        result1.checkComplete();

        final OutputChannel<Data> result2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(Abort.class))
                        .withId(0)
                        .buildRoutine()
                        .callAsync(data1);

        try {

            result2.readFirst();

            fail();

        } catch (final RoutineClashException ignored) {

        }

        result2.checkComplete();
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testErrors() {

        try {

            JRoutine.initActivity((FragmentActivity) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.initFragment((Fragment) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity((FragmentActivity) null, ClassToken.tokenOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity((FragmentActivity) null, 0);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), AndroidRoutineBuilder.GENERATED_ID);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment((Fragment) null, ClassToken.tokenOf(ToUpperCase.class));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFragment((Fragment) null, 0);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                      .findFragmentById(
                                                                              R.id.test_fragment);
            JRoutine.onFragment(fragment, AndroidRoutineBuilder.GENERATED_ID);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(new TestActivity(), ClassToken.tokenOf(ToUpperCase.class));

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            JRoutine.onFragment(new TestFragment(), ClassToken.tokenOf(ToUpperCase.class));

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                    .logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class)).onClash(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), ClassToken.tokenOf(ToUpperCase.class))
                    .onComplete(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFragmentAbort() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.ABORT_ON_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        assertThat(result1.readFirst()).isEqualTo("TEST1");

        try {

            result2.readFirst();

            fail();

        } catch (final InputClashException ignored) {

        }
    }

    public void testFragmentChannel() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .outputOrder(DataOrder.INSERTION)
                        .buildRoutine();
        final OutputChannel<String> channel1 = routine.callAsync("test1", "test2");
        final OutputChannel<String> channel2 = JRoutine.onFragment(fragment, 0).buildChannel();

        assertThat(channel1.readAll()).containsExactly("TEST1", "TEST2");
        assertThat(channel2.readAll()).containsExactly("TEST1", "TEST2");
    }

    public void testFragmentInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class)).buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testFragmentKeep() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.KEEP)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST1");
    }

    public void testFragmentMissingRoutine() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> channel = JRoutine.onFragment(fragment, 0).buildChannel();

        try {

            channel.readAll();

            fail();

        } catch (final RoutineMissingException ignored) {

        }
    }

    public void testFragmentReset() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.RESTART)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test1");

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        assertThat(result2.readFirst()).isEqualTo("TEST1");
    }

    public void testFragmentRestart() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<String, String> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(ToUpperCase.class))
                        .withId(0)
                        .onClash(ClashResolution.RESTART_ON_INPUT)
                        .buildRoutine();
        final OutputChannel<String> result1 = routine.callAsync("test1");
        final OutputChannel<String> result2 = routine.callAsync("test2");

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testFragmentSame() throws InterruptedException {

        final Data data1 = new Data();
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Data, Data> routine =
                JRoutine.onFragment(fragment, ClassToken.tokenOf(Delay.class)).buildRoutine();
        final OutputChannel<Data> result1 = routine.callAsync(data1);
        final OutputChannel<Data> result2 = routine.callAsync(data1);

        assertThat(result1.readFirst()).isSameAs(data1);
        assertThat(result2.readFirst()).isSameAs(data1);
    }

    public void testInvocations() {

        final Routine<String, String> routine1 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(StringTunnelInvocation.class))
                        .syncRunner(RunnerType.QUEUED)
                        .loggedWith(Logs.androidLog())
                        .logLevel(LogLevel.WARNING)
                        .buildRoutine();
        assertThat(routine1.callSync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2", "3",
                                                                                      "4", "5");
        assertThat(routine1.callAsync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                       "3", "4",
                                                                                       "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                          "3", "4",
                                                                                          "5");

        final Routine<String, String> routine2 =
                JRoutine.onActivity(getActivity(), ClassToken.tokenOf(StringSimpleInvocation.class))
                        .syncRunner(RunnerType.SEQUENTIAL)
                        .loggedWith(Logs.androidLog())
                        .logLevel(LogLevel.WARNING)
                        .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2", "3",
                                                                                      "4", "5");
        assertThat(routine2.callAsync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                       "3", "4",
                                                                                       "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                          "3", "4",
                                                                                          "5");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLoaderError() throws NoSuchMethodException {

        final Logger logger = Logger.createLogger(null, LogLevel.DEFAULT, this);
        final WeakReference<Object> reference = new WeakReference<Object>(getActivity());

        try {

            new LoaderInvocation<String, String>(null, 0, ClashResolution.KEEP, ResultCache.RETAIN,
                                                 ToUpperCase.class.getDeclaredConstructor(),
                                                 DataOrder.DEFAULT, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, 0, null, ResultCache.RETAIN,
                                                 ToUpperCase.class.getDeclaredConstructor(),
                                                 DataOrder.DEFAULT, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, 0, ClashResolution.KEEP, null,
                                                 ToUpperCase.class.getDeclaredConstructor(),
                                                 DataOrder.DEFAULT, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, 0, ClashResolution.KEEP,
                                                 ResultCache.RETAIN, null, DataOrder.DEFAULT,
                                                 logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, 0, ClashResolution.KEEP,
                                                 ResultCache.RETAIN,
                                                 ToUpperCase.class.getDeclaredConstructor(), null,
                                                 logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(reference, 0, ClashResolution.KEEP,
                                                 ResultCache.RETAIN,
                                                 ToUpperCase.class.getDeclaredConstructor(),
                                                 DataOrder.DEFAULT, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineError() throws NoSuchMethodException {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().buildConfiguration();
        final WeakReference<Object> reference = new WeakReference<Object>(getActivity());

        try {

            new AndroidRoutine<String, String>(null, reference, 0, ClashResolution.KEEP,
                                               ResultCache.RETAIN,
                                               ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AndroidRoutine<String, String>(configuration, null, 0, ClashResolution.KEEP,
                                               ResultCache.RETAIN,
                                               ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AndroidRoutine<String, String>(configuration, reference, 0, null,
                                               ResultCache.RETAIN,
                                               ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AndroidRoutine<String, String>(configuration, reference, 0, ClashResolution.KEEP,
                                               null, ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AndroidRoutine<String, String>(configuration, reference, 0, ClashResolution.KEEP,
                                               ResultCache.RETAIN, null);

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

                RoutineInterruptedException.interrupt(e);
            }

            result.abort(new IllegalStateException());
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

    private static class StringSimpleInvocation extends AndroidSimpleInvocation<String, String> {

        @Override
        public void onCall(@Nonnull final List<? extends String> strings,
                @Nonnull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class StringTunnelInvocation extends AndroidTunnelInvocation<String> {

    }

    private static class ToUpperCase extends AndroidTemplateInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(500)).pass(s.toUpperCase());
        }
    }
}
