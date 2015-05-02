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
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.android.invocation.ContextInvocationDecorator;
import com.gh.bmd.jrt.android.invocation.FilterContextInvocation;
import com.gh.bmd.jrt.android.invocation.PassingContextInvocation;
import com.gh.bmd.jrt.android.invocation.SingleCallContextInvocation;
import com.gh.bmd.jrt.android.invocation.TemplateContextInvocation;
import com.gh.bmd.jrt.android.log.AndroidLog;
import com.gh.bmd.jrt.android.runner.MainRunner;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ReadDeadlockException;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.time.TimeDuration.millis;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invocation service routine unit tests.
 * <p/>
 * Created by davide on 12/1/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ServiceRoutineBuilderTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ServiceRoutineBuilderTest() {

        super(TestActivity.class);
    }

    public void testAbort() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Data data = new Data();
        final OutputChannel<Data> channel =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(Delay.class))
                        .withServiceConfiguration()
                        .withRunnerClass(MainRunner.class)
                        .set()
                        .callAsync(data);
        assertThat(channel.abort(new IllegalArgumentException("test"))).isTrue();

        try {

            channel.afterMax(timeout).readNext();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        try {

            JRoutine.onService(getActivity(), ClassToken.tokenOf(Abort.class))
                    .callAsync(data)
                    .afterMax(timeout)
                    .readNext();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuilderError() {

        final ClassToken<PassingContextInvocation<String>> classToken =
                new ClassToken<PassingContextInvocation<String>>() {};

        try {

            JRoutine.onService(null, classToken);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onService(getActivity(), (ClassToken<PassingContextInvocation<String>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        final ClassToken<PassingContextInvocation<Object>> classToken =
                new ClassToken<PassingContextInvocation<Object>>() {};

        try {

            new DefaultServiceRoutineBuilder<Object, Object>(getActivity(),
                                                             classToken).setConfiguration(
                    (RoutineConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultServiceRoutineBuilder<Object, Object>(getActivity(),
                                                             classToken).setConfiguration(
                    (ServiceConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testDecorator() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final ClassToken<PassingDecorator<String>> token =
                ClassToken.tokenOf(new PassingDecorator<String>());
        final Routine<String, String> routine = JRoutine.onService(getActivity(), token)
                                                        .withRoutineConfiguration()
                                                        .withSyncRunner(Runners.queuedRunner())
                                                        .withInputOrder(OrderType.NONE)
                                                        .withLogLevel(LogLevel.DEBUG)
                                                        .set()
                                                        .withServiceConfiguration()
                                                        .withLogClass(AndroidLog.class)
                                                        .set()
                                                        .buildRoutine();
        assertThat(
                routine.callSync("1", "2", "3", "4", "5").afterMax(timeout).readAll()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    public void testInvocations() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine1 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringPassingInvocation.class))
                        .withRoutineConfiguration()
                        .withSyncRunner(Runners.queuedRunner())
                        .withInputOrder(OrderType.NONE)
                        .withLogLevel(LogLevel.DEBUG)
                        .set()
                        .withServiceConfiguration()
                        .withLogClass(AndroidLog.class)
                        .set()
                        .buildRoutine();
        assertThat(routine1.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
    }

    public void testInvocations2() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final ClassToken<StringSingleCallInvocation> token =
                ClassToken.tokenOf(StringSingleCallInvocation.class);
        final Routine<String, String> routine2 = JRoutine.onService(getActivity(), token)
                                                         .withRoutineConfiguration()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withOutputOrder(OrderType.NONE)
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .set()
                                                         .withServiceConfiguration()
                                                         .withLogClass(AndroidLog.class)
                                                         .set()
                                                         .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsExactly("1", "2", "3", "4", "5");
        assertThat(routine2.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsExactly("1", "2", "3", "4", "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
    }

    public void testInvocations3() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final ClassToken<StringSingleCallInvocation> token =
                ClassToken.tokenOf(StringSingleCallInvocation.class);
        final Routine<String, String> routine3 = JRoutine.onService(getActivity(), token)
                                                         .withRoutineConfiguration()
                                                         .withInputOrder(OrderType.PASSING_ORDER)
                                                         .withOutputOrder(OrderType.PASSING_ORDER)
                                                         .set()
                                                         .buildRoutine();
        assertThat(routine3.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsExactly("1", "2", "3", "4", "5");
        assertThat(routine3.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsExactly("1", "2", "3", "4", "5");
        assertThat(routine3.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsExactly("1", "2", "3", "4", "5");
    }

    public void testInvocations4() throws InterruptedException {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final ClassToken<StringSingleCallInvocation> token =
                ClassToken.tokenOf(StringSingleCallInvocation.class);
        final Routine<String, String> routine4 = JRoutine.onService(getActivity(), token)
                                                         .withRoutineConfiguration()
                                                         .withCoreInvocations(0)
                                                         .withMaxInvocations(2)
                                                         .withAvailableInvocationTimeout(1, SECONDS)
                                                         .withAvailableInvocationTimeout(
                                                                 TimeDuration.millis(200))
                                                         .set()
                                                         .buildRoutine();
        assertThat(routine4.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine4.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine4.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
    }

    public void testParcelable() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final MyParcelable p = new MyParcelable(33, -17);
        assertThat(
                JRoutine.onService(getActivity(), ClassToken.tokenOf(MyParcelableInvocation.class))
                        .callAsync(p)
                        .afterMax(timeout)
                        .readNext()).isEqualTo(p);
    }

    public void testReadTimeout() {

        final ClassToken<PassingContextInvocation<String>> classToken =
                new ClassToken<PassingContextInvocation<String>>() {};
        assertThat(JRoutine.onService(getActivity(), classToken)
                           .withRoutineConfiguration()
                           .withReadTimeout(millis(10))
                           .withReadTimeoutAction(TimeoutActionType.EXIT)
                           .set()
                           .withServiceConfiguration()
                           .withReceivingLooper(Looper.myLooper())
                           .set()
                           .callAsync("test1")
                           .readAll()).isEmpty();
    }

    public void testReadTimeout2() {

        final ClassToken<PassingContextInvocation<String>> classToken =
                new ClassToken<PassingContextInvocation<String>>() {};

        try {

            JRoutine.onService(getActivity(), classToken)
                    .withRoutineConfiguration()
                    .withReadTimeout(millis(10))
                    .withReadTimeoutAction(TimeoutActionType.ABORT)
                    .set()
                    .withServiceConfiguration()
                    .withReceivingLooper(Looper.myLooper())
                    .set()
                    .callAsync("test2")
                    .readAll();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testReadTimeout3() {

        final ClassToken<PassingContextInvocation<String>> classToken =
                new ClassToken<PassingContextInvocation<String>>() {};

        try {

            JRoutine.onService(getActivity(), classToken)
                    .withRoutineConfiguration()
                    .withReadTimeout(millis(10))
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .withServiceConfiguration()
                    .withReceivingLooper(Looper.myLooper())
                    .set()
                    .callAsync("test3")
                    .readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }
    }

    public void testService() {

        final TimeDuration timeout = TimeDuration.seconds(10);
        final Routine<String, String> routine =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringPassingInvocation.class))
                        .withServiceConfiguration()
                        .withServiceClass(TestService.class)
                        .set()
                        .buildRoutine();
        assertThat(
                routine.callSync("1", "2", "3", "4", "5").afterMax(timeout).readAll()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine.callAsync("1", "2", "3", "4", "5")
                          .afterMax(timeout)
                          .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine.callParallel("1", "2", "3", "4", "5")
                          .afterMax(timeout)
                          .readAll()).containsOnly("1", "2", "3", "4", "5");
    }

    public void testServiceRoutineBuilderWarnings() {

        final CountLog countLog = new CountLog();
        JRoutine.onService(getActivity(), ClassToken.tokenOf(StringPassingInvocation.class))
                .withRoutineConfiguration()
                .withInputMaxSize(3)
                .withInputTimeout(seconds(1))
                .withOutputMaxSize(3)
                .withOutputTimeout(seconds(1))
                .withLogLevel(LogLevel.DEBUG)
                .withLog(countLog)
                .set()
                .withServiceConfiguration()
                .withServiceClass(TestService.class)
                .set()
                .buildRoutine();
        assertThat(countLog.getWrnCount()).isEqualTo(4);
    }

    private static class Abort extends TemplateContextInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

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

    private static class PassingDecorator<DATA> extends ContextInvocationDecorator<DATA, DATA> {

        public PassingDecorator() {

            super(PassingInvocation.<DATA>factoryOf().newInvocation());
        }
    }

    private static class StringPassingInvocation extends FilterContextInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.pass(s);
        }
    }

    private static class StringSingleCallInvocation
            extends SingleCallContextInvocation<String, String> {

        @Override
        public void onCall(@Nonnull final List<? extends String> strings,
                @Nonnull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }
}
