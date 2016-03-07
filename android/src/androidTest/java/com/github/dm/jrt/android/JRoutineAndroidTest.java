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

package com.github.dm.jrt.android;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.v11.TestActivity;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.object.annotation.Alias;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 02/29/2016.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class JRoutineAndroidTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public JRoutineAndroidTest() {

        super(TestActivity.class);
    }

    public void testService() {

        final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.withService(getActivity())
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.withService(getActivity(), InvocationService.class)
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.withService(getActivity(),
                                               new Intent(getActivity(), InvocationService.class))
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
    }

    public void testServiceClass() throws NoSuchMethodException {

        assertThat(JRoutineAndroid.withService(getActivity())
                                  .onInstance(TestClass.class, "test")
                                  .method("getStringUp")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
        assertThat(JRoutineAndroid.withService(getActivity())
                                  .onInstance(TestClass.class)
                                  .method(TestClass.class.getMethod("getStringUp"))
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
        assertThat(JRoutineAndroid.withService(getActivity())
                                  .on(classOfType(TestClass.class))
                                  .alias("TEST")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
    }

    public void testServiceInstance() throws NoSuchMethodException {

        assertThat(JRoutineAndroid.withService(getActivity())
                                  .onInstance(TestClass.class, "test")
                                  .method(TestClass.class.getMethod("getStringLow"))
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.withService(getActivity())
                                  .onInstance(TestClass.class)
                                  .method("getStringLow")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.withService(getActivity())
                                  .on(instanceOf(TestClass.class))
                                  .alias("test")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
    }

    public void testServiceInvocation() {

        final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(token)
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(token, 2)
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test", "test");
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(PassString.class)
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(PassString.class, 3)
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test", "test", "test");
    }

    public static class Pass<DATA> extends TemplateContextInvocation<DATA, DATA> {

        private final int mCount;

        public Pass() {

            this(1);
        }

        public Pass(final int count) {

            mCount = count;
        }

        @Override
        public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) throws
                Exception {

            final int count = mCount;
            for (int i = 0; i < count; i++) {
                result.pass(input);
            }
        }
    }

    public static class PassString extends Pass<String> {

        public PassString() {

        }

        public PassString(final int count) {

            super(count);
        }
    }

    public static class TestClass {

        private static String sText;

        public TestClass() {

            this("test");
        }

        public TestClass(final String text) {

            sText = text;
        }

        @Alias("TEST")
        public static String getStringUp() {

            return sText.toUpperCase();
        }

        @Alias("test")
        public String getStringLow() {

            return sText.toLowerCase();
        }
    }
}
