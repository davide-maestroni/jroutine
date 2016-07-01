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

package com.github.dm.jrt.android.core.invocation;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.ChannelContextInvocation;
import com.github.dm.jrt.android.core.TestActivity;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryFrom;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.fromFactory;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation factory unit tests.
 * <p>
 * Created by davide-maestroni on 03/10/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ContextInvocationFactoryTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ContextInvocationFactoryTest() {

        super(TestActivity.class);
    }

    public void testClass() {

        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryOf(Case.class)))
                               .syncCall("TEST")
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryOf(Case.class, true)))
                               .syncCall("test")
                               .all()).containsExactly("TEST");
    }

    @SuppressWarnings("ConstantConditions")
    public void testClassError() {

        try {
            factoryOf((Class<Case>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((Class<Case>) null, true);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFromFactory() {

        final InvocationFactory<String, String> factory =
                fromFactory(getActivity(), factoryOf(tokenOf(Case.class)));
        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryFrom(factory)))
                               .syncCall("TEST")
                               .all()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testFromFactoryError() {

        try {
            factoryFrom(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testTemplateInvocation() {
        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryOf(ContextTest.class)))
                               .syncCall()
                               .close()
                               .getError()).isNull();
        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryOf(ChannelContextTest.class)))
                               .syncCall()
                               .close()
                               .getError()).isNull();
    }

    public void testToken() {

        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryOf(tokenOf(Case.class))))
                               .syncCall("TEST")
                               .all()).containsExactly("test");
        assertThat(
                JRoutineCore.with(fromFactory(getActivity(), factoryOf(tokenOf(Case.class), true)))
                            .syncCall("test")
                            .all()).containsExactly("TEST");
    }

    @SuppressWarnings("ConstantConditions")
    public void testTokenError() {

        try {
            factoryOf((ClassToken<Case>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((ClassToken<Case>) null, true);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testWrapper() {

        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryOf(CaseWrapper.class)))
                               .syncCall("TEST")
                               .all()).containsExactly("test");
        assertThat(JRoutineCore.with(fromFactory(getActivity(), factoryOf(CaseWrapper.class, true)))
                               .syncCall("test")
                               .all()).containsExactly("TEST");
    }

    @SuppressWarnings("unused")
    public static class Case extends TemplateContextInvocation<String, String> {

        private final boolean mIsUpper;

        public Case() {

            this(false);
        }

        public Case(final boolean isUpper) {

            mIsUpper = isUpper;
        }

        @Override
        public void onInput(final String input, @NotNull final Channel<String, ?> result) throws
                Exception {

            result.pass(mIsUpper ? input.toUpperCase() : input.toLowerCase());
        }
    }

    @SuppressWarnings("unused")
    public static class CaseWrapper extends ContextInvocationWrapper<String, String> {

        public CaseWrapper() {

            super(new Case());
        }

        public CaseWrapper(final boolean isUpper) {

            super(new Case(isUpper));
        }
    }

    public static class ContextTest extends TemplateContextInvocation<Object, Object> {

        @Override
        public void onComplete(@NotNull final Channel<Object, ?> result) {
            assertThat(getContext()).isExactlyInstanceOf(TestActivity.class);
        }
    }

    public static class ChannelContextTest extends ChannelContextInvocation<Object, Object> {

        @NotNull
        @Override
        protected Channel<?, Object> onChannel(@NotNull final Channel<?, Object> channel) {
            assertThat(getContext()).isExactlyInstanceOf(TestActivity.class);
            return channel;
        }
    }
}
