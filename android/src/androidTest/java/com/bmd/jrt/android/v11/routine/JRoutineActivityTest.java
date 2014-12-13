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
package com.bmd.jrt.android.v11.routine;

import android.annotation.TargetApi;
import android.content.pm.ActivityInfo;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.TemplateInvocation;

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

    public void testLoader() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

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

    public void testRotation() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestActivity activity = getActivity();

        JRoutine.in(activity).invoke(ClassToken.tokenOf(ToUpperCase.class)).pass("test1").result();
        JRoutine.in(activity).invoke(ClassToken.tokenOf(ToUpperCase.class)).pass("test2").result();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getInstrumentation().waitForIdleSync();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        Thread.sleep(1000);

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

    private static class ToUpperCase extends TemplateInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.pass(s.toUpperCase());
        }
    }
}
