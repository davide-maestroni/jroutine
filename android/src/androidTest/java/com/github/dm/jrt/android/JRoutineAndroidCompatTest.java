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

import com.github.dm.jrt.TargetRoutineBuilder.BuilderType;
import com.github.dm.jrt.android.core.TargetInvocationFactory;
import com.github.dm.jrt.android.core.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.CallContextInvocationFactories;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.v4.TestActivity;
import com.github.dm.jrt.android.v4.TestFragment;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.object.annotation.Alias;
import com.github.dm.jrt.object.annotation.AsyncOut;
import com.github.dm.jrt.object.annotation.ReadTimeout;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 02/29/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class JRoutineAndroidCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public JRoutineAndroidCompatTest() {

        super(TestActivity.class);
    }

    public void testLoader() {

        final ClassToken<Join<String>> token = new ClassToken<Join<String>>() {};
        assertThat(JRoutineAndroidCompat.with(loaderFrom(getActivity()))
                                        .on(CallContextInvocationFactories.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .on(CallContextInvocationFactories.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity(), getActivity())
                                        .on(CallContextInvocationFactories.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        assertThat(JRoutineAndroidCompat.withLoader(fragment)
                                        .on(CallContextInvocationFactories.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(fragment, getActivity())
                                        .on(CallContextInvocationFactories.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
    }

    public void testLoaderClass() throws NoSuchMethodException {

        new TestClass("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onClass(TestClass.class)
                                        .method("getStringUp")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("TEST");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onClass(TestClass.class)
                                        .method(TestClass.class.getMethod("getStringUp"))
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("TEST");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .on(classOfType(TestClass.class))
                                        .alias("TEST")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("TEST");
    }

    public void testLoaderId() throws NoSuchMethodException {

        new TestClass("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .withLoaders()
                                        .withLoaderId(33)
                                        .withCacheStrategy(CacheStrategyType.CACHE)
                                        .getConfigured()
                                        .method("getStringLow")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onId(33)
                                        .buildChannel()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
    }

    public void testLoaderInstance() throws NoSuchMethodException {

        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class, "TEST")
                                        .method(TestClass.class.getMethod("getStringLow"))
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .method("getStringLow")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .on(instanceOf(TestClass.class))
                                        .alias("test")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
    }

    public void testLoaderInvocation() {

        final ClassToken<JoinString> token = new ClassToken<JoinString>() {};
        assertThat(JRoutineAndroidCompat.with(loaderFrom(getActivity()))
                                        .on(token)
                                        .asyncCall("test1", "test2")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test1,test2");
        assertThat(JRoutineAndroidCompat.with(loaderFrom(getActivity()))
                                        .on(token, ";")
                                        .asyncCall("test1", "test2")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test1;test2");
        assertThat(JRoutineAndroidCompat.with(loaderFrom(getActivity()))
                                        .on(JoinString.class)
                                        .asyncCall("test1", "test2")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test1,test2");
        assertThat(JRoutineAndroidCompat.with(loaderFrom(getActivity()))
                                        .on(JoinString.class, " ")
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test1 test2");
    }

    public void testLoaderProxy() {

        new TestClass("TEST");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .buildProxy(TestAnnotatedProxy.class)
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.OBJECT)
                                        .buildProxy(TestProxy.class)
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.OBJECT)
                                        .buildProxy(tokenOf(TestProxy.class))
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.PROXY)
                                        .buildProxy(TestAnnotatedProxy.class)
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withLoader(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.PROXY)
                                        .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                        .getStringLow()
                                        .all()).containsExactly("test");
    }

    public void testService() {

        final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
        assertThat(JRoutineAndroidCompat.with(serviceFrom(getActivity()))
                                        .on(TargetInvocationFactory.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .on(TargetInvocationFactory.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity(), InvocationService.class)
                                        .on(TargetInvocationFactory.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity(), new Intent(getActivity(),
                                                                               InvocationService
                                                                                       .class))
                                        .on(TargetInvocationFactory.factoryOf(token))
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
    }

    public void testServiceClass() throws NoSuchMethodException {

        new TestClass("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onClass(TestClass.class)
                                        .method("getStringUp")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("TEST");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onClass(TestClass.class)
                                        .method(TestClass.class.getMethod("getStringUp"))
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("TEST");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .on(classOfType(TestClass.class))
                                        .alias("TEST")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("TEST");
    }

    public void testServiceInstance() throws NoSuchMethodException {

        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class, "TEST")
                                        .method(TestClass.class.getMethod("getStringLow"))
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class)
                                        .method("getStringLow")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .on(instanceOf(TestClass.class))
                                        .alias("test")
                                        .asyncCall()
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
    }

    public void testServiceInvocation() {

        final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
        assertThat(JRoutineAndroidCompat.with(serviceFrom(getActivity()))
                                        .on(token)
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.with(serviceFrom(getActivity()))
                                        .on(token, 2)
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test", "test");
        assertThat(JRoutineAndroidCompat.with(serviceFrom(getActivity()))
                                        .on(PassString.class)
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.with(serviceFrom(getActivity()))
                                        .on(PassString.class, 3)
                                        .asyncCall("test")
                                        .afterMax(seconds(10))
                                        .all()).containsExactly("test", "test", "test");
    }

    public void testServiceProxy() {

        new TestClass("TEST");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class)
                                        .buildProxy(TestAnnotatedProxy.class)
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class)
                                        .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.OBJECT)
                                        .buildProxy(TestProxy.class)
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.OBJECT)
                                        .buildProxy(tokenOf(TestProxy.class))
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.PROXY)
                                        .buildProxy(TestAnnotatedProxy.class)
                                        .getStringLow()
                                        .all()).containsExactly("test");
        assertThat(JRoutineAndroidCompat.withService(getActivity())
                                        .onInstance(TestClass.class)
                                        .withBuilder(BuilderType.PROXY)
                                        .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                        .getStringLow()
                                        .all()).containsExactly("test");
    }

    @ServiceProxy(TestClass.class)
    @LoaderProxyCompat(TestClass.class)
    public interface TestAnnotatedProxy extends TestProxy {

    }

    public interface TestProxy {

        @AsyncOut
        @ReadTimeout(value = 10, unit = TimeUnit.SECONDS)
        OutputChannel<String> getStringLow();
    }

    public static class Join<DATA> extends CallContextInvocation<DATA, String> {

        private final String mSeparator;

        public Join() {

            this(",");
        }

        public Join(final String separator) {

            mSeparator = separator;
        }

        @Override
        protected void onCall(@NotNull final List<? extends DATA> inputs,
                @NotNull final ResultChannel<String> result) throws Exception {

            final String separator = mSeparator;
            final StringBuilder builder = new StringBuilder();
            for (final DATA input : inputs) {
                if (builder.length() > 0) {
                    builder.append(separator);
                }

                builder.append(input.toString());
            }

            result.pass(builder.toString());
        }
    }

    @SuppressWarnings("unused")
    public static class JoinString extends Join<String> {

        public JoinString() {

        }

        public JoinString(final String separator) {

            super(separator);
        }
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

    @SuppressWarnings("unused")
    public static class PassString extends Pass<String> {

        public PassString() {

        }

        public PassString(final int count) {

            super(count);
        }
    }

    @SuppressWarnings("unused")
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
