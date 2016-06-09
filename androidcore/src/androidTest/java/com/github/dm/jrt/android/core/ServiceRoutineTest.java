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

package com.github.dm.jrt.android.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationWrapper;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.log.AndroidLog;
import com.github.dm.jrt.android.core.runner.MainRunner;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ExecutionTimeoutException;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service routine unit tests.
 * <p>
 * Created by davide-maestroni on 12/01/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ServiceRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ServiceRoutineTest() {

        super(TestActivity.class);
    }

    public void testAbort() {

        final UnitDuration timeout = seconds(10);
        final Data data = new Data();
        final OutputChannel<Data> channel = JRoutineService.with(serviceFrom(getActivity()))
                                                           .on(factoryOf(Delay.class))
                                                           .serviceConfiguration()
                                                           .withRunnerClass(MainRunner.class)
                                                           .apply()
                                                           .asyncCall(data);
        assertThat(channel.abort(new IllegalArgumentException("test"))).isTrue();

        try {

            channel.afterMax(timeout).next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        try {

            JRoutineService.with(serviceFrom(getActivity()))
                           .on(factoryOf(Abort.class))
                           .asyncCall()
                           .afterMax(timeout)
                           .next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuilderError() {

        final ClassToken<StringPassingInvocation> classToken =
                new ClassToken<StringPassingInvocation>() {};
        final ServiceContext context = null;

        try {

            JRoutineService.with(context).on(factoryOf(classToken));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineService.with(serviceFrom(getActivity())).on(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineService.with(serviceFrom(getActivity()))
                           .on(factoryOf((ClassToken<StringPassingInvocation>) null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        final ClassToken<StringPassingInvocation> classToken =
                new ClassToken<StringPassingInvocation>() {};

        try {

            new DefaultServiceRoutineBuilder<String, String>(serviceFrom(getActivity()),
                    factoryOf(classToken)).apply((InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultServiceRoutineBuilder<String, String>(serviceFrom(getActivity()),
                    factoryOf(classToken)).apply((ServiceConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConstructor() {

        boolean failed = false;
        try {
            new JRoutineService();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    public void testDecorator() {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(new PassingWrapper<String>());
        final Routine<String, String> routine = JRoutineService.with(serviceFrom(getActivity()))
                                                               .on(targetFactory)
                                                               .invocationConfiguration()
                                                               .withInputOrder(OrderType.BY_DELAY)
                                                               .withLogLevel(Level.DEBUG)
                                                               .apply()
                                                               .serviceConfiguration()
                                                               .withLogClass(AndroidLog.class)
                                                               .apply()
                                                               .buildRoutine();
        assertThat(routine.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    public void testExecutionTimeout() {

        final OutputChannel<String> channel = JRoutineService.with(serviceFrom(getActivity()))
                                                             .on(factoryOf(StringDelay.class))
                                                             .invocationConfiguration()
                                                             .withOutputTimeout(millis(10))
                                                             .withOutputTimeoutAction(
                                                                     TimeoutActionType.BREAK)
                                                             .apply()
                                                             .asyncCall("test1");
        assertThat(channel.all()).isEmpty();
        assertThat(channel.afterMax(seconds(10)).hasCompleted()).isTrue();
    }

    public void testExecutionTimeout2() {

        final OutputChannel<String> channel = JRoutineService.with(serviceFrom(getActivity()))
                                                             .on(factoryOf(StringDelay.class))
                                                             .invocationConfiguration()
                                                             .withOutputTimeout(millis(10))
                                                             .withOutputTimeoutAction(
                                                                     TimeoutActionType.ABORT)
                                                             .apply()
                                                             .asyncCall("test2");

        try {

            channel.all();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(channel.afterMax(seconds(10)).hasCompleted()).isTrue();
    }

    public void testExecutionTimeout3() {

        final OutputChannel<String> channel = JRoutineService.with(serviceFrom(getActivity()))
                                                             .on(factoryOf(StringDelay.class))
                                                             .invocationConfiguration()
                                                             .withOutputTimeout(millis(10))
                                                             .withOutputTimeoutAction(
                                                                     TimeoutActionType.THROW)
                                                             .apply()
                                                             .asyncCall("test3");

        try {

            channel.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(channel.afterMax(seconds(10)).hasCompleted()).isTrue();
    }

    public void testInvocations() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(StringPassingInvocation.class);
        final Routine<String, String> routine1 = JRoutineService.with(serviceFrom(getActivity()))
                                                                .on(targetFactory)
                                                                .invocationConfiguration()
                                                                .withInputOrder(OrderType.BY_DELAY)
                                                                .withLogLevel(Level.DEBUG)
                                                                .apply()
                                                                .serviceConfiguration()
                                                                .withLogClass(AndroidLog.class)
                                                                .apply()
                                                                .buildRoutine();
        assertThat(routine1.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine1.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
    }

    public void testInvocations2() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final ClassToken<StringCallInvocation> token = tokenOf(StringCallInvocation.class);
        final Routine<String, String> routine2 = JRoutineService.with(serviceFrom(getActivity()))
                                                                .on(factoryOf(token))
                                                                .invocationConfiguration()
                                                                .withOutputOrder(OrderType.BY_DELAY)
                                                                .withLogLevel(Level.DEBUG)
                                                                .apply()
                                                                .serviceConfiguration()
                                                                .withLogClass(AndroidLog.class)
                                                                .apply()
                                                                .buildRoutine();
        assertThat(
                routine2.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsExactly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsExactly("1", "2", "3", "4", "5");
        assertThat(routine2.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
    }

    public void testInvocations3() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(StringCallInvocation.class);
        final Routine<String, String> routine3 = JRoutineService.with(serviceFrom(getActivity()))
                                                                .on(targetFactory)
                                                                .invocationConfiguration()
                                                                .withInputOrder(OrderType.BY_CALL)
                                                                .withOutputOrder(OrderType.BY_CALL)
                                                                .apply()
                                                                .buildRoutine();
        assertThat(
                routine3.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsExactly(
                "1", "2", "3", "4", "5");
        assertThat(routine3.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsExactly("1", "2", "3", "4", "5");
        assertThat(routine3.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsExactly("1", "2", "3", "4", "5");
    }

    public void testInvocations4() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(StringCallInvocation.class);
        final Routine<String, String> routine4 = JRoutineService.with(serviceFrom(getActivity()))
                                                                .on(targetFactory)
                                                                .invocationConfiguration()
                                                                .withCoreInstances(0)
                                                                .withMaxInstances(2)
                                                                .apply()
                                                                .buildRoutine();
        assertThat(routine4.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine4.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine4.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
    }

    public void testInvocations5() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<Void, String> targetFactory =
                factoryOf(TextCommandInvocation.class);
        final Routine<Void, String> routine4 = JRoutineService.with(serviceFrom(getActivity()))
                                                              .on(targetFactory)
                                                              .invocationConfiguration()
                                                              .withCoreInstances(0)
                                                              .withMaxInstances(2)
                                                              .apply()
                                                              .buildRoutine();
        assertThat(routine4.syncCall().afterMax(timeout).all()).containsOnly("test1", "test2",
                "test3");
        assertThat(routine4.asyncCall().afterMax(timeout).all()).containsOnly("test1", "test2",
                "test3");
        assertThat(routine4.parallelCall().afterMax(timeout).all()).containsOnly("test1", "test2",
                "test3");
    }

    public void testParcelable() {

        final UnitDuration timeout = seconds(10);
        final MyParcelable p = new MyParcelable(33, -17);
        assertThat(JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(MyParcelableInvocation.class))
                                  .asyncCall(p)
                                  .afterMax(timeout)
                                  .next()).isEqualTo(p);
    }

    public void testService() {

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine =
                JRoutineService.with(serviceFrom(getActivity(), TestService.class))
                               .on(factoryOf(StringPassingInvocation.class))
                               .buildRoutine();
        assertThat(routine.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine.parallelCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    public void testSize() {

        final InvocationChannel<String, String> channel =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(StringPassingInvocation.class))
                               .asyncInvoke();
        assertThat(channel.size()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.size()).isEqualTo(1);
        final OutputChannel<String> result = channel.result();
        assertThat(result.afterMax(seconds(10)).hasCompleted()).isTrue();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.skipNext(1).size()).isEqualTo(0);
    }

    private static class Abort extends TemplateContextInvocation<Data, Data> {

        @Override
        public void onResult(@NotNull final ResultChannel<Data> result) {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException e) {

                throw new InvocationInterruptedException(e);
            }

            result.abort(new IllegalStateException("test"));
        }
    }

    @SuppressWarnings("unused")
    private static class CountLog implements Log {

        private int mDgbCount;

        private int mErrCount;

        private int mWrnCount;

        public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mDgbCount;
        }

        public void err(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mErrCount;
        }

        public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mWrnCount;
        }

        public int getDgbCount() {

            return mDgbCount;
        }

        public int getErrCount() {

            return mErrCount;
        }

        public int getWrnCount() {

            return mWrnCount;
        }
    }

    private static class Data implements Parcelable {

        public static final Creator<Data> CREATOR = new Creator<Data>() {

            public Data createFromParcel(final Parcel source) {

                return new Data();
            }

            public Data[] newArray(final int size) {

                return new Data[size];
            }
        };

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

        }
    }

    private static class Delay extends TemplateContextInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @NotNull final ResultChannel<Data> result) {

            result.after(UnitDuration.millis(500)).pass(d);
        }
    }

    private static class MyParcelable implements Parcelable {

        public static final Creator<MyParcelable> CREATOR = new Creator<MyParcelable>() {

            public MyParcelable createFromParcel(final Parcel source) {

                final int x = source.readInt();
                final int y = source.readInt();
                return new MyParcelable(x, y);
            }

            public MyParcelable[] newArray(final int size) {

                return new MyParcelable[0];
            }
        };

        private final int mX;

        private final int mY;

        private MyParcelable(final int x, final int y) {

            mX = x;
            mY = y;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof MyParcelable)) {

                return false;
            }

            final MyParcelable that = (MyParcelable) o;

            return mX == that.mX && mY == that.mY;
        }

        @Override
        public int hashCode() {

            int result = mX;
            result = 31 * result + mY;
            return result;
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeInt(mX);
            dest.writeInt(mY);
        }
    }

    private static class MyParcelableInvocation
            extends TemplateContextInvocation<MyParcelable, MyParcelable> {

        public void onInput(final MyParcelable myParcelable,
                @NotNull final ResultChannel<MyParcelable> result) {

            result.pass(myParcelable);
        }
    }

    private static class PassingWrapper<DATA> extends ContextInvocationWrapper<DATA, DATA> {

        public PassingWrapper() {

            super(IdentityInvocation.<DATA>factoryOf().newInvocation());
        }
    }

    private static class StringCallInvocation extends CallContextInvocation<String, String> {

        @Override
        protected void onCall(@NotNull final List<? extends String> strings,
                @NotNull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class StringDelay extends TemplateContextInvocation<String, String> {

        @Override
        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.after(UnitDuration.millis(100)).pass(s);
        }
    }

    private static class StringPassingInvocation extends TemplateContextInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.pass(s);
        }
    }

    private static class TextCommandInvocation extends TemplateContextInvocation<Void, String> {

        public void onResult(@NotNull final ResultChannel<String> result) {

            result.pass("test1", "test2", "test3");
        }
    }
}
