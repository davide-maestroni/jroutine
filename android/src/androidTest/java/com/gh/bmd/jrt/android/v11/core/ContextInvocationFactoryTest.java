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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationDecorator;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.DecoratingContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.PassingContextInvocation;
import com.gh.bmd.jrt.android.routine.LoaderRoutine;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.android.v11.core.LoaderContext.contextFrom;
import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation factories unit test.
 * <p/>
 * Created by davide-maestroni on 19/08/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ContextInvocationFactoryTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ContextInvocationFactoryTest() {

        super(TestActivity.class);
    }

    public void testDecoratingInvocationFactory() {

        final ContextInvocationFactory<String, String> factory =
                PassingContextInvocation.factoryOf();
        assertThat(factory.newInvocation()).isExactlyInstanceOf(PassingContextInvocation.class);
        final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
        assertThat(decoratedFactory.newInvocation()).isExactlyInstanceOf(
                TestInvocationDecorator.class);
    }

    public void testInvocationDecoratorAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ContextInvocationFactory<String, String> factory =
                PassingContextInvocation.factoryOf();
        final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), decoratedFactory).buildRoutine();
        assertThat(routine.asyncInvoke().after(millis(100)).pass("test").result().abort()).isTrue();
        routine.purge();
    }

    public void testInvocationDecoratorLifecycle() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ContextInvocationFactory<String, String> factory =
                PassingContextInvocation.factoryOf();
        final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
        final LoaderRoutine<String, String> routine =
                JRoutine.on(contextFrom(getActivity()), decoratedFactory).buildRoutine();
        assertThat(routine.asyncCall("test").eventually().all()).containsExactly("test");
        routine.purge();
    }

    private static class TestInvocationDecorator
            extends ContextInvocationDecorator<String, String> {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped invocation instance.
         */
        public TestInvocationDecorator(@Nonnull final ContextInvocation<String, String> wrapped) {

            super(wrapped);
        }
    }

    private static class TestInvocationFactory
            extends DecoratingContextInvocationFactory<String, String> {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped factory instance.
         */
        public TestInvocationFactory(
                @Nonnull final ContextInvocationFactory<String, String> wrapped) {

            super(wrapped);
        }

        @Nonnull
        @Override
        protected ContextInvocation<String, String> decorate(
                @Nonnull final ContextInvocation<String, String> invocation) {

            return new TestInvocationDecorator(invocation);
        }
    }
}
