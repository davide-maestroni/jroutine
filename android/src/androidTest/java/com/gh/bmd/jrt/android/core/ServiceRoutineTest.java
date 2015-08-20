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
package com.gh.bmd.jrt.android.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.android.core.ServiceTarget.InvocationServiceTarget;
import com.gh.bmd.jrt.android.invocation.ContextInvocationWrapper;
import com.gh.bmd.jrt.android.invocation.FilterContextInvocation;
import com.gh.bmd.jrt.android.invocation.FunctionContextInvocation;
import com.gh.bmd.jrt.android.invocation.PassingContextInvocation;
import com.gh.bmd.jrt.android.invocation.TemplateContextInvocation;
import com.gh.bmd.jrt.android.log.AndroidLog;
import com.gh.bmd.jrt.android.runner.MainRunner;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.ExecutionTimeoutException;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.core.ServiceContext.serviceFrom;
import static com.gh.bmd.jrt.android.core.ServiceTarget.targetInvocation;
import static com.gh.bmd.jrt.util.ClassToken.tokenOf;
import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service routine unit tests.
 * <p/>
 * Created by davide-maestroni on 12/1/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ServiceRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ServiceRoutineTest() {

        super(TestActivity.class);
    }

    public void testAbort() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data = new Data();
        final OutputChannel<Data> channel =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation(Delay.class))
                        .service()
                        .withRunnerClass(MainRunner.class)
                        .set()
                        .asyncCall(data);
        assertThat(channel.abort(new IllegalArgumentException("test"))).isTrue();

        try {

            channel.afterMax(timeout).next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        try {

            JRoutine.on(serviceFrom(getActivity()), targetInvocation(Abort.class))
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

        final ClassToken<PassingContextInvocation<String>> classToken =
                new ClassToken<PassingContextInvocation<String>>() {};

        try {

            JRoutine.on(null, targetInvocation(classToken));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(serviceFrom(getActivity()), (InvocationServiceTarget) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(serviceFrom(getActivity()),
                        targetInvocation((ClassToken<PassingContextInvocation<String>>) null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        final ClassToken<PassingContextInvocation<Object>> classToken =
                new ClassToken<PassingContextInvocation<Object>>() {};

        try {

            new DefaultServiceRoutineBuilder<Object, Object>(serviceFrom(getActivity()),
                                                             targetInvocation(
                                                                     classToken)).setConfiguration(
                    (InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultServiceRoutineBuilder<Object, Object>(serviceFrom(getActivity()),
                                                             targetInvocation(
                                                                     classToken)).setConfiguration(
                    (ServiceConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testDecorator() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final InvocationServiceTarget<String, String> targetInvocation =
                targetInvocation(new PassingWrapper<String>());
        final Routine<String, String> routine =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation)
                        .invocations()
                        .withSyncRunner(Runners.queuedRunner())
                        .withInputOrder(OrderType.BY_CHANCE)
                        .withLogLevel(LogLevel.DEBUG)
                        .set()
                        .service()
                        .withLogClass(AndroidLog.class)
                        .set()
                        .buildRoutine();
        assertThat(routine.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    public void testExecutionTimeout() {

        final OutputChannel<String> channel =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation(StringDelay.class))
                        .invocations()
                        .withExecutionTimeout(millis(10))
                        .withExecutionTimeoutAction(TimeoutActionType.EXIT)
                        .set()
                        .asyncCall("test1");
        assertThat(channel.all()).isEmpty();
        assertThat(channel.eventually().checkComplete()).isTrue();
    }

    public void testExecutionTimeout2() {

        final OutputChannel<String> channel =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation(StringDelay.class))
                        .invocations()
                        .withExecutionTimeout(millis(10))
                        .withExecutionTimeoutAction(TimeoutActionType.ABORT)
                        .set()
                        .asyncCall("test2");

        try {

            channel.all();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(channel.eventually().checkComplete()).isTrue();
    }

    public void testExecutionTimeout3() {

        final OutputChannel<String> channel =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation(StringDelay.class))
                        .invocations()
                        .withExecutionTimeout(millis(10))
                        .withExecutionTimeoutAction(TimeoutActionType.THROW)
                        .set()
                        .asyncCall("test3");

        try {

            channel.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(channel.eventually().checkComplete()).isTrue();
    }

    public void testInvocations() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final InvocationServiceTarget<String, String> targetInvocation =
                targetInvocation(StringPassingInvocation.class);
        final Routine<String, String> routine1 =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation)
                        .invocations()
                        .withSyncRunner(Runners.queuedRunner())
                        .withInputOrder(OrderType.BY_CHANCE)
                        .withLogLevel(LogLevel.DEBUG)
                        .set()
                        .service()
                        .withLogClass(AndroidLog.class)
                        .set()
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final ClassToken<StringFunctionInvocation> token = tokenOf(StringFunctionInvocation.class);
        final Routine<String, String> routine2 =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation(token))
                        .invocations()
                        .withSyncRunner(Runners.queuedRunner())
                        .withOutputOrder(OrderType.BY_CHANCE)
                        .withLogLevel(LogLevel.DEBUG)
                        .set()
                        .service()
                        .withLogClass(AndroidLog.class)
                        .set()
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final InvocationServiceTarget<String, String> targetInvocation =
                targetInvocation(StringFunctionInvocation.class);
        final Routine<String, String> routine3 =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation)
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
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

        final TimeDuration timeout = TimeDuration.seconds(10);
        final InvocationServiceTarget<String, String> targetInvocation =
                targetInvocation(StringFunctionInvocation.class);
        final Routine<String, String> routine4 =
                JRoutine.on(serviceFrom(getActivity()), targetInvocation)
                        .invocations()
                        .withCoreInstances(0)
                        .withMaxInstances(2)
                        .set()
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

    public void testParcelable() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final MyParcelable p = new MyParcelable(33, -17);
        assertThat(JRoutine.on(serviceFrom(getActivity()),
                               targetInvocation(MyParcelableInvocation.class))
                           .asyncCall(p)
                           .afterMax(timeout)
                           .next()).isEqualTo(p);
    }

    public void testService() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.on(serviceFrom(getActivity(), TestService.class),
                            targetInvocation(StringPassingInvocation.class)).buildRoutine();
        assertThat(routine.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine.parallelCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    private static class Abort extends TemplateContextInvocation<Data, Data> {

        @Override
        public void onResult(@Nonnull final ResultChannel<Data> result) {

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

        public void dbg(@Nonnull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mDgbCount;
        }

        public void err(@Nonnull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mErrCount;
        }

        public void wrn(@Nonnull final List<Object> contexts, @Nullable final String message,
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

            public Data createFromParcel(@Nonnull final Parcel source) {

                return new Data();
            }

            @Nonnull
            public Data[] newArray(final int size) {

                return new Data[size];
            }
        };

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

        }
    }

    private static class Delay extends TemplateContextInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(d);
        }
    }

    private static class MyParcelable implements Parcelable {

        public static final Creator<MyParcelable> CREATOR = new Creator<MyParcelable>() {

            public MyParcelable createFromParcel(@Nonnull final Parcel source) {

                final int x = source.readInt();
                final int y = source.readInt();
                return new MyParcelable(x, y);
            }

            @Nonnull
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

        public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

            dest.writeInt(mX);
            dest.writeInt(mY);
        }
    }

    private static class MyParcelableInvocation
            extends FilterContextInvocation<MyParcelable, MyParcelable> {

        public void onInput(final MyParcelable myParcelable,
                @Nonnull final ResultChannel<MyParcelable> result) {

            result.pass(myParcelable);
        }
    }

    private static class PassingWrapper<DATA> extends ContextInvocationWrapper<DATA, DATA> {

        public PassingWrapper() {

            super(PassingInvocation.<DATA>factoryOf().newInvocation());
        }
    }

    private static class StringDelay extends TemplateContextInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(100)).pass(s);
        }
    }

    private static class StringFunctionInvocation
            extends FunctionContextInvocation<String, String> {

        @Override
        protected void onCall(@Nonnull final List<? extends String> strings,
                @Nonnull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class StringPassingInvocation extends FilterContextInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.pass(s);
        }
    }
}
