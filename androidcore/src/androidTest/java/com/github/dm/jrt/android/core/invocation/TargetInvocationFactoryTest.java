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
import android.os.Parcel;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.DecoratingService;
import com.github.dm.jrt.android.core.DecoratingService.StringInvocation;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.RemoteDecoratingService;
import com.github.dm.jrt.android.core.TestActivity;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Target invocation factories unit test.
 * <p>
 * Created by davide-maestroni on 10/06/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class TargetInvocationFactoryTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public TargetInvocationFactoryTest() {

        super(TestActivity.class);
    }

    public void testEquals() {

        final TargetInvocationFactory<String, String> factory =
                factoryOf(PassingStringInvocation.class);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(factoryOf(PassingStringInvocation.class, 3));
        assertThat(factory).isEqualTo(factoryOf(PassingStringInvocation.class));
        assertThat(factory.hashCode()).isEqualTo(
                factoryOf(PassingStringInvocation.class).hashCode());
    }

    public void testEquals2() {

        final TargetInvocationFactory<String, String> factory =
                factoryOf(PassingStringInvocation2.class);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(factoryOf(PassingStringInvocation2.class, 3));
        assertThat(factory).isEqualTo(factoryOf(PassingStringInvocation2.class));
        assertThat(factory.hashCode()).isEqualTo(
                factoryOf(PassingStringInvocation2.class).hashCode());
    }

    public void testInvocationDecoratorAbort() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), DecoratingService.class))
                               .with(factoryOf(PassingStringInvocation.class))
                               .buildRoutine();
        assertThat(routine.async().after(millis(100)).pass("test").close().abort()).isTrue();
        routine.clear();
    }

    public void testInvocationDecoratorAbort2() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), DecoratingService.class))
                               .with(factoryOf(PassingStringInvocation2.class))
                               .buildRoutine();
        assertThat(routine.async().after(millis(100)).pass("test").close().abort()).isTrue();
        routine.clear();
    }

    public void testInvocationDecoratorLifecycle() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), DecoratingService.class))
                               .with(factoryOf(PassingStringInvocation.class))
                               .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
    }

    public void testInvocationDecoratorLifecycle2() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), DecoratingService.class))
                               .with(factoryOf(PassingStringInvocation2.class))
                               .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
    }

    public void testInvocationFactory() {

        Routine<String, String> routine = JRoutineService.on(serviceFrom(getActivity()))
                                                         .with(factoryOf(
                                                                 PassingStringInvocation.class))
                                                         .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
        routine = JRoutineService.on(serviceFrom(getActivity()))
                                 .with(factoryOf(PassingStringInvocation.class, 3))
                                 .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test", "test",
                "test");
        routine.clear();
        routine = JRoutineService.on(serviceFrom(getActivity()))
                                 .with(factoryOf(tokenOf(PassingStringInvocation.class)))
                                 .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
        routine = JRoutineService.on(serviceFrom(getActivity()))
                                 .with(factoryOf(tokenOf(PassingStringInvocation.class), 3))
                                 .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test", "test",
                "test");
        routine.clear();
    }

    public void testInvocationFactory2() {

        Routine<String, String> routine = JRoutineService.on(serviceFrom(getActivity()))
                                                         .with(factoryOf(
                                                                 PassingStringInvocation2.class))
                                                         .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
        routine = JRoutineService.on(serviceFrom(getActivity()))
                                 .with(factoryOf(PassingStringInvocation2.class, 3))
                                 .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test", "test",
                "test");
        routine.clear();
        routine = JRoutineService.on(serviceFrom(getActivity()))
                                 .with(factoryOf(tokenOf(PassingStringInvocation2.class)))
                                 .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
        routine = JRoutineService.on(serviceFrom(getActivity()))
                                 .with(factoryOf(tokenOf(PassingStringInvocation2.class), 3))
                                 .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test", "test",
                "test");
        routine.clear();
    }

    @SuppressWarnings("ConstantConditions")
    public void testInvocationFactoryError() {

        try {
            factoryOf((Class<ContextInvocation<Object, Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((Class<ContextInvocation<Object, Object>>) null, "test");
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((ClassToken<ContextInvocation<Object, Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((ClassToken<ContextInvocation<Object, Object>>) null, "test");
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((ContextInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((ContextInvocation<Object, Object>) null, "test");
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testInvocationFactoryError2() {

        try {
            factoryOf((Class<Invocation<Object, Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((Class<Invocation<Object, Object>>) null, "test");
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((ClassToken<Invocation<Object, Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((ClassToken<Invocation<Object, Object>>) null, "test");
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((Invocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            factoryOf((Invocation<Object, Object>) null, "test");
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testParcelable() {

        final Parcel parcel = Parcel.obtain();
        final TargetInvocationFactory<String, String> factory =
                factoryOf(PassingStringInvocation.class);
        parcel.writeParcelable(factory, 0);
        parcel.setDataPosition(0);
        final Parcelable parcelable =
                parcel.readParcelable(TargetInvocationFactory.class.getClassLoader());
        assertThat(parcelable).isEqualTo(factory);
        parcel.recycle();
    }

    public void testParcelable2() {

        final Parcel parcel = Parcel.obtain();
        final TargetInvocationFactory<String, String> factory =
                factoryOf(PassingStringInvocation2.class);
        parcel.writeParcelable(factory, 0);
        parcel.setDataPosition(0);
        final Parcelable parcelable =
                parcel.readParcelable(TargetInvocationFactory.class.getClassLoader());
        assertThat(parcelable).isEqualTo(factory);
        parcel.recycle();
    }

    public void testRemoteInvocationDecoratorAbort() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), RemoteDecoratingService.class))
                               .with(factoryOf(PassingStringInvocation.class))
                               .buildRoutine();
        assertThat(routine.async().after(millis(100)).pass("test").close().abort()).isTrue();
        routine.clear();
    }

    public void testRemoteInvocationDecoratorAbort2() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), RemoteDecoratingService.class))
                               .with(factoryOf(PassingStringInvocation2.class))
                               .buildRoutine();
        assertThat(routine.async().after(millis(100)).pass("test").close().abort()).isTrue();
        routine.clear();
    }

    public void testRemoteInvocationDecoratorLifecycle() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), RemoteDecoratingService.class))
                               .with(factoryOf(PassingStringInvocation.class))
                               .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
    }

    public void testRemoteInvocationDecoratorLifecycle2() {

        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), RemoteDecoratingService.class))
                               .with(factoryOf(PassingStringInvocation2.class))
                               .buildRoutine();
        assertThat(routine.async("test").after(seconds(10)).all()).containsExactly("test");
        routine.clear();
    }

    @SuppressWarnings("unused")
    private static class PassingStringInvocation extends TemplateContextInvocation<String, String>
            implements StringInvocation {

        private final int mCount;

        public PassingStringInvocation() {

            this(1);
        }

        public PassingStringInvocation(final int count) {

            mCount = count;
        }

        public void onInput(final String input, @NotNull final Channel<String, ?> result) {

            for (int i = 0; i < mCount; i++) {
                result.pass(input);
            }
        }
    }

    @SuppressWarnings("unused")
    private static class PassingStringInvocation2 extends TemplateInvocation<String, String> {

        private final int mCount;

        public PassingStringInvocation2() {

            this(1);
        }

        public PassingStringInvocation2(final int count) {

            mCount = count;
        }

        public void onInput(final String input, @NotNull final Channel<String, ?> result) {

            for (int i = 0; i < mCount; i++) {
                result.pass(input);
            }
        }
    }
}
