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
import com.bmd.jrt.android.invocator.InputClashException;
import com.bmd.jrt.android.invocator.RoutineClashException;
import com.bmd.jrt.android.invocator.RoutineInvocator.ClashResolution;
import com.bmd.jrt.android.invocator.RoutineInvocator.ResultCache;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.invocation.TemplateInvocation;
import com.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
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

        final OutputChannel<String> result1 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .onClash(ClashResolution.ABORT)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .onClash(ClashResolution.ABORT)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");

        try {

            result2.readFirst();

            fail();

        } catch (final InputClashException ignored) {

        }
    }

    public void testActivityClearError() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .onComplete(ResultCache.CLEAR_IF_ERROR)
                                                    .invoke(ClassToken.tokenOf(Abort.class))
                                                    .pass(data1)
                                                    .result();

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        final OutputChannel<Data> result2 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .onComplete(ResultCache.CLEAR_IF_ERROR)
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result2.readFirst()).isSameAs(data1);

        final OutputChannel<Data> result3 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result3.readFirst()).isSameAs(data1);
    }

    public void testActivityClearResult() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .onComplete(ResultCache.CLEAR_IF_RESULT)
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result1.readFirst()).isSameAs(data1);

        RoutineException error = null;
        final OutputChannel<Data> result2 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .onComplete(ResultCache.CLEAR_IF_RESULT)
                                                    .invoke(ClassToken.tokenOf(Abort.class))
                                                    .pass(data1)
                                                    .result();

        try {

            result2.readFirst();

            fail();

        } catch (final RoutineException e) {

            error = e;
        }

        final OutputChannel<Data> result3 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .invoke(ClassToken.tokenOf(Abort.class))
                                                    .pass(data1)
                                                    .result();

        try {

            result3.readFirst();

            fail();

        } catch (final RoutineException e) {

            assertThat(e).isSameAs(error);
        }
    }

    public void testActivityInputs() {

        final OutputChannel<String> result1 = JRoutine.in(getActivity())
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(getActivity())
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testActivityKeep() {

        final OutputChannel<String> result1 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .onClash(ClashResolution.KEEP)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .onClash(ClashResolution.KEEP)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST1");
    }

    public void testActivityReset() {

        final OutputChannel<String> result1 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .onClash(ClashResolution.RESET)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .onClash(ClashResolution.RESET)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        assertThat(result2.readFirst()).isEqualTo("TEST1");
    }

    public void testActivityRestart() {

        final OutputChannel<String> result1 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(getActivity())
                                                      .withId(0)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        try {

            result1.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testActivityRetain() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .onComplete(ResultCache.RETAIN)
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result1.readFirst()).isSameAs(data1);

        final OutputChannel<Data> result2 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result2.readFirst()).isSameAs(data1);

        RoutineException error = null;
        final OutputChannel<Data> result3 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .onComplete(ResultCache.RETAIN)
                                                    .invoke(ClassToken.tokenOf(Abort.class))
                                                    .pass(data1)
                                                    .result();

        try {

            result3.readFirst();

            fail();

        } catch (final RoutineException e) {

            error = e;
        }

        final OutputChannel<Data> result4 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .invoke(ClassToken.tokenOf(Abort.class))
                                                    .pass(data1)
                                                    .result();

        try {

            result4.readFirst();

            fail();

        } catch (final RoutineException e) {

            assertThat(e).isSameAs(error);
        }
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testActivityRotationInputs() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        JRoutine.in(getActivity())
                .invoke(ClassToken.tokenOf(ToUpperCase.class))
                .pass("test1")
                .result();
        JRoutine.in(getActivity())
                .invoke(ClassToken.tokenOf(ToUpperCase.class))
                .pass("test2")
                .result();

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

        final OutputChannel<String> result1 =
                JRoutine.in(getActivity()).invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 =
                JRoutine.in(getActivity()).invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testActivityRotationSame() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Data data1 = new Data();
        JRoutine.in(getActivity()).invoke(ClassToken.tokenOf(Delay.class)).pass(data1).result();
        JRoutine.in(getActivity()).invoke(ClassToken.tokenOf(Delay.class)).pass(data1).result();

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

        final OutputChannel<Data> result1 = JRoutine.in(getActivity())
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();
        final OutputChannel<Data> result2 = JRoutine.in(getActivity())
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result1.readFirst()).isSameAs(data1);
        assertThat(result2.readFirst()).isSameAs(data1);
    }

    public void testActivitySame() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.in(getActivity())
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();
        final OutputChannel<Data> result2 = JRoutine.in(getActivity())
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result1.readFirst()).isSameAs(data1);
        assertThat(result2.readFirst()).isSameAs(data1);
    }

    public void testClash() {

        final Data data1 = new Data();
        final OutputChannel<Data> result1 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .onComplete(ResultCache.RETAIN)
                                                    .invoke(ClassToken.tokenOf(Delay.class))
                                                    .pass(data1)
                                                    .result();

        assertThat(result1.readFirst()).isSameAs(data1);

        final OutputChannel<Data> result2 = JRoutine.in(getActivity())
                                                    .withId(0)
                                                    .invoke(ClassToken.tokenOf(Abort.class))
                                                    .pass(data1)
                                                    .result();

        try {

            result2.readFirst();

            fail();

        } catch (final RoutineClashException ignored) {

        }
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    public void testErrors() {

        try {

            JRoutine.enable((FragmentActivity) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.enable((Fragment) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.in((FragmentActivity) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.in((Fragment) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.in(new TestActivity());

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            JRoutine.in(new TestFragment());

            fail();

        } catch (final IllegalStateException ignored) {

        }
    }

    public void testFragmentAbort() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> result1 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .onClash(ClashResolution.ABORT)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .onClash(ClashResolution.ABORT)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");

        try {

            result2.readFirst();

            fail();

        } catch (final InputClashException ignored) {

        }
    }

    public void testFragmentInputs() throws InterruptedException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> result1 = JRoutine.in(fragment)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(fragment)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testFragmentKeep() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> result1 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .onClash(ClashResolution.KEEP)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .onClash(ClashResolution.KEEP)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST1");
    }

    public void testFragmentReset() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final OutputChannel<String> result1 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .onClash(ClashResolution.RESET)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .onClash(ClashResolution.RESET)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();

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
        final OutputChannel<String> result1 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(fragment)
                                                      .withId(0)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

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
        final OutputChannel<Data> result1 =
                JRoutine.in(fragment).invoke(ClassToken.tokenOf(Delay.class)).pass(data1).result();
        final OutputChannel<Data> result2 =
                JRoutine.in(fragment).invoke(ClassToken.tokenOf(Delay.class)).pass(data1).result();

        assertThat(result1.readFirst()).isSameAs(data1);
        assertThat(result2.readFirst()).isSameAs(data1);
    }

    @SuppressWarnings("ConstantConditions")
    public void testLoaderError() throws NoSuchMethodException {

        try {

            new LoaderInvocation<String, String>(null, 0, ClashResolution.KEEP, ResultCache.RETAIN,
                                                 ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(new WeakReference<Object>(getActivity()), 0, null,
                                                 ResultCache.RETAIN,
                                                 ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(new WeakReference<Object>(getActivity()), 0,
                                                 ClashResolution.KEEP, null,
                                                 ToUpperCase.class.getDeclaredConstructor());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new LoaderInvocation<String, String>(new WeakReference<Object>(getActivity()), 0,
                                                 ClashResolution.KEEP, ResultCache.RETAIN, null);

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

                RoutineInterruptedException.interrupt(e);
            }

            result.abort();
        }
    }

    private static class Data {

    }

    private static class Delay extends TemplateInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(d);
        }
    }

    private static class ToUpperCase extends TemplateInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(500)).pass(s.toUpperCase());
        }
    }
}
