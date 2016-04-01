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
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.log.AndroidLog;
import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxy;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.v11.TestActivity;
import com.github.dm.jrt.android.v11.TestFragment;
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
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android facade unit tests.
 * <p>
 * Created by davide-maestroni on 02/29/2016.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class JRoutineAndroidTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public JRoutineAndroidTest() {

        super(TestActivity.class);
    }

    public void testLoader() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ClassToken<Join<String>> token = new ClassToken<Join<String>>() {};
        assertThat(JRoutineAndroid.with(loaderFrom(getActivity()))
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity(), getActivity())
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        assertThat(JRoutineAndroid.with(fragment)
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(fragment, getActivity())
                                  .on(factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
    }

    public void testLoaderClass() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        new TestClass("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .classOfType(TestClass.class)
                                  .method("getStringUp")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .classOfType(TestClass.class)
                                  .method(TestClass.class.getMethod("getStringUp"))
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .on(classOfType(TestClass.class))
                                  .method("TEST")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
    }

    public void testLoaderId() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        new TestClass("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .loaderConfiguration()
                                  .withLoaderId(33)
                                  .withCacheStrategy(CacheStrategyType.CACHE)
                                  .setConfiguration()
                                  .method("getStringLow")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .onId(33)
                                  .buildChannel()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
    }

    public void testLoaderInstance() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class, "TEST")
                                  .method(TestClass.class.getMethod("getStringLow"))
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .method("getStringLow")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .on(instanceOf(TestClass.class))
                                  .method("test")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
    }

    public void testLoaderInvocation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ClassToken<JoinString> token = new ClassToken<JoinString>() {};
        assertThat(JRoutineAndroid.with(loaderFrom(getActivity()))
                                  .on(token)
                                  .asyncCall("test1", "test2")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test1,test2");
        assertThat(JRoutineAndroid.with(loaderFrom(getActivity()))
                                  .on(token, ";")
                                  .asyncCall("test1", "test2")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test1;test2");
        assertThat(JRoutineAndroid.with(loaderFrom(getActivity()))
                                  .on(JoinString.class)
                                  .asyncCall("test1", "test2")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test1,test2");
        assertThat(JRoutineAndroid.with(loaderFrom(getActivity()))
                                  .on(JoinString.class, " ")
                                  .asyncCall("test1", "test2")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test1 test2");
        assertThat(JRoutineAndroid.with(loaderFrom(getActivity()))
                                  .on(new JoinString())
                                  .asyncCall("test1", "test2")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test1,test2");
        assertThat(JRoutineAndroid.with(loaderFrom(getActivity()))
                                  .on(new JoinString(), " ")
                                  .asyncCall("test1", "test2")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test1 test2");
    }

    public void testLoaderProxy() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        new TestClass("TEST");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .buildProxy(TestAnnotatedProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(LoaderTargetRoutineBuilder.BuilderType.OBJECT)
                                  .buildProxy(TestProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(LoaderTargetRoutineBuilder.BuilderType.OBJECT)
                                  .buildProxy(tokenOf(TestProxy.class))
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(LoaderTargetRoutineBuilder.BuilderType.PROXY)
                                  .buildProxy(TestAnnotatedProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(LoaderTargetRoutineBuilder.BuilderType.PROXY)
                                  .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                  .getStringLow()
                                  .all()).containsExactly("test");
    }

    public void testLoaderProxyConfiguration() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        new TestClass("TEST");
        assertThat(JRoutineAndroid.with(getActivity())
                                  .instanceOf(TestClass.class)
                                  .invocationConfiguration()
                                  .withLog(AndroidLogs.androidLog())
                                  .setConfiguration()
                                  .proxyConfiguration()
                                  .withSharedFields()
                                  .setConfiguration()
                                  .loaderConfiguration()
                                  .withFactoryId(11)
                                  .setConfiguration()
                                  .buildProxy(TestAnnotatedProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLoaderProxyError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineAndroid.with(getActivity()).on((ContextInvocationTarget<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testService() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(TargetInvocationFactory.factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .on(TargetInvocationFactory.factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity(), InvocationService.class)
                                  .on(TargetInvocationFactory.factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(getActivity(),
                new Intent(getActivity(), InvocationService.class))
                                  .on(TargetInvocationFactory.factoryOf(token))
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
    }

    public void testServiceClass() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        new TestClass("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .classOfType(TestClass.class)
                                  .method("getStringUp")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .classOfType(TestClass.class)
                                  .method(TestClass.class.getMethod("getStringUp"))
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .on(classOfType(TestClass.class))
                                  .method("TEST")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("TEST");
    }

    public void testServiceInstance() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class, "TEST")
                                  .method(TestClass.class.getMethod("getStringLow"))
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .method("getStringLow")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .on(instanceOf(TestClass.class))
                                  .method("test")
                                  .asyncCall()
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
    }

    public void testServiceInvocation() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

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
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(new Pass<String>())
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with(serviceFrom(getActivity()))
                                  .on(new Pass<String>(), 2)
                                  .asyncCall("test")
                                  .afterMax(seconds(10))
                                  .all()).containsExactly("test", "test");
    }

    public void testServiceProxy() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        new TestClass("TEST");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .buildProxy(TestAnnotatedProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(ServiceTargetRoutineBuilder.BuilderType.OBJECT)
                                  .buildProxy(TestProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(ServiceTargetRoutineBuilder.BuilderType.OBJECT)
                                  .buildProxy(tokenOf(TestProxy.class))
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(ServiceTargetRoutineBuilder.BuilderType.PROXY)
                                  .buildProxy(TestAnnotatedProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .withType(ServiceTargetRoutineBuilder.BuilderType.PROXY)
                                  .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                  .getStringLow()
                                  .all()).containsExactly("test");
    }

    public void testServiceProxyConfiguration() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        new TestClass("TEST");
        assertThat(JRoutineAndroid.with((Context) getActivity())
                                  .instanceOf(TestClass.class)
                                  .invocationConfiguration()
                                  .withLog(AndroidLogs.androidLog())
                                  .setConfiguration()
                                  .proxyConfiguration()
                                  .withSharedFields()
                                  .setConfiguration()
                                  .serviceConfiguration()
                                  .withLogClass(AndroidLog.class)
                                  .setConfiguration()
                                  .buildProxy(TestAnnotatedProxy.class)
                                  .getStringLow()
                                  .all()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testServiceProxyError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineAndroid.with((Context) getActivity()).on((ContextInvocationTarget<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @ServiceProxy(TestClass.class)
    @LoaderProxy(TestClass.class)
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
