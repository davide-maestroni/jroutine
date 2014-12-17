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
import android.content.pm.ActivityInfo;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.TemplateInvocation;
import com.bmd.jrt.time.TimeDuration;

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

    public void testInputs() {

        final TestActivity activity = getActivity();

        final OutputChannel<String> result1 = JRoutine.in(activity)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test1")
                                                      .result();
        final OutputChannel<String> result2 = JRoutine.in(activity)
                                                      .invoke(ClassToken.tokenOf(ToUpperCase.class))
                                                      .pass("test2")
                                                      .result();

        assertThat(result1.readFirst()).isEqualTo("TEST1");
        assertThat(result2.readFirst()).isEqualTo("TEST2");
    }

    public void testRotationInputs() throws InterruptedException {

        JRoutine.in(getActivity())
                .invoke(ClassToken.tokenOf(ToUpperCase.class))
                .pass("test1")
                .result();
        JRoutine.in(getActivity())
                .invoke(ClassToken.tokenOf(ToUpperCase.class))
                .pass("test2")
                .result();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getInstrumentation().waitForIdleSync();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        Thread.sleep(1000);

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

    public void testRotationSame() throws InterruptedException {

        final Data data1 = new Data();
        JRoutine.in(getActivity()).invoke(ClassToken.tokenOf(Delay.class)).pass(data1).result();
        JRoutine.in(getActivity()).invoke(ClassToken.tokenOf(Delay.class)).pass(data1).result();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getInstrumentation().waitForIdleSync();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        Thread.sleep(1000);

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

    public void testSame() {

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

            result.pass(s.toUpperCase());
        }
    }
}
