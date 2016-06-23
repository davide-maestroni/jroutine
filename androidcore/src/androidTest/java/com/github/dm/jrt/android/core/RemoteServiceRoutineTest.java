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
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
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
 * Remote service routine unit tests.
 * <p>
 * Created by davide-maestroni on 12/01/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class RemoteServiceRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public RemoteServiceRoutineTest() {

        super(TestActivity.class);
    }

    public void testAbort() {

        final UnitDuration timeout = seconds(10);
        final Data data = new Data();
        final Channel<?, Data> channel =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(factoryOf(Delay.class))
                               .serviceConfiguration()
                               .withRunnerClass(MainRunner.class)
                               .applied()
                               .async(data);
        assertThat(channel.abort(new IllegalArgumentException("test"))).isTrue();

        try {

            channel.after(timeout).next();

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        try {

            JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                           .with(factoryOf(Abort.class))
                           .async()
                           .close()
                           .after(timeout)
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

            JRoutineService.on(context).with(factoryOf(classToken));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                           .with(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                           .with(factoryOf((ClassToken<StringPassingInvocation>) null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        final ClassToken<StringPassingInvocation> classToken =
                new ClassToken<StringPassingInvocation>() {};

        try {

            new DefaultServiceRoutineBuilder<String, String>(
                    serviceFrom(getActivity(), RemoteInvocationService.class),
                    factoryOf(classToken)).apply((InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultServiceRoutineBuilder<String, String>(
                    serviceFrom(getActivity(), RemoteInvocationService.class),
                    factoryOf(classToken)).apply((ServiceConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testDecorator() {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(new PassingWrapper<String>());
        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(targetFactory)
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_DELAY)
                               .withLogLevel(Level.DEBUG)
                               .applied()
                               .serviceConfiguration()
                               .withLogClass(AndroidLog.class)
                               .applied()
                               .buildRoutine();
        assertThat(routine.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
    }

    public void testExecutionTimeout() {

        final Channel<?, String> channel =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(factoryOf(StringDelay.class))
                               .invocationConfiguration()
                               .withOutputTimeout(millis(10))
                               .withOutputTimeoutAction(TimeoutActionType.BREAK)
                               .applied()
                               .async("test1");
        assertThat(channel.all()).isEmpty();
        assertThat(channel.after(seconds(10)).hasCompleted()).isTrue();
    }

    public void testExecutionTimeout2() {

        final Channel<?, String> channel =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(factoryOf(StringDelay.class))
                               .invocationConfiguration()
                               .withOutputTimeout(millis(10))
                               .withOutputTimeoutAction(TimeoutActionType.ABORT)
                               .applied()
                               .async("test2");

        try {

            channel.all();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(channel.after(seconds(10)).hasCompleted()).isTrue();
    }

    public void testExecutionTimeout3() {

        final Channel<?, String> channel =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(factoryOf(StringDelay.class))
                               .invocationConfiguration()
                               .withOutputTimeout(millis(10))
                               .withOutputTimeoutAction(TimeoutActionType.FAIL)
                               .applied()
                               .async("test3");

        try {

            channel.all();

            fail();

        } catch (final OutputTimeoutException ignored) {

        }

        assertThat(channel.after(seconds(10)).hasCompleted()).isTrue();
    }

    public void testInvocations() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(StringPassingInvocation.class);
        final Routine<String, String> routine1 =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(targetFactory)
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_DELAY)
                               .withLogLevel(Level.DEBUG)
                               .applied()
                               .serviceConfiguration()
                               .withLogClass(AndroidLog.class)
                               .applied()
                               .buildRoutine();
        assertThat(routine1.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine1.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine1.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    public void testInvocations2() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final ClassToken<StringCallInvocation> token = tokenOf(StringCallInvocation.class);
        final Routine<String, String> routine2 =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(factoryOf(token))
                               .invocationConfiguration()
                               .withOutputOrder(OrderType.BY_DELAY)
                               .withLogLevel(Level.DEBUG)
                               .applied()
                               .serviceConfiguration()
                               .withLogClass(AndroidLog.class)
                               .applied()
                               .buildRoutine();
        assertThat(routine2.sync("1", "2", "3", "4", "5").after(timeout).all()).containsExactly("1",
                "2", "3", "4", "5");
        assertThat(routine2.async("1", "2", "3", "4", "5").after(timeout).all()).containsExactly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    public void testInvocations3() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(StringCallInvocation.class);
        final Routine<String, String> routine3 =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(targetFactory)
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_CALL)
                               .withOutputOrder(OrderType.BY_CALL)
                               .applied()
                               .buildRoutine();
        assertThat(routine3.sync("1", "2", "3", "4", "5").after(timeout).all()).containsExactly("1",
                "2", "3", "4", "5");
        assertThat(routine3.async("1", "2", "3", "4", "5").after(timeout).all()).containsExactly(
                "1", "2", "3", "4", "5");
        assertThat(routine3.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsExactly(
                "1", "2", "3", "4", "5");
    }

    public void testInvocations4() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<String, String> targetFactory =
                factoryOf(StringCallInvocation.class);
        final Routine<String, String> routine4 =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(targetFactory)
                               .invocationConfiguration()
                               .withCoreInstances(0)
                               .withMaxInstances(2)
                               .applied()
                               .buildRoutine();
        assertThat(routine4.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine4.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine4.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
    }

    public void testInvocations5() throws InterruptedException {

        final UnitDuration timeout = seconds(10);
        final TargetInvocationFactory<Void, String> targetFactory =
                factoryOf(TextCommandInvocation.class);
        final Routine<Void, String> routine4 =
                JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                               .with(targetFactory)
                               .invocationConfiguration()
                               .withCoreInstances(0)
                               .withMaxInstances(2)
                               .applied()
                               .buildRoutine();
        assertThat(routine4.sync().close().after(timeout).all()).containsOnly("test1", "test2",
                "test3");
        assertThat(routine4.async().close().after(timeout).all()).containsOnly("test1", "test2",
                "test3");
        assertThat(routine4.parallel().close().after(timeout).all()).containsOnly("test1", "test2",
                "test3");
    }

    public void testParcelable() {

        final UnitDuration timeout = seconds(10);
        final MyParcelable p = new MyParcelable(33, -17);
        assertThat(JRoutineService.on(serviceFrom(getActivity(), RemoteInvocationService.class))
                                  .with(factoryOf(MyParcelableInvocation.class))
                                  .async(p)
                                  .after(timeout)
                                  .next()).isEqualTo(p);
    }

    public void testService() {

        final UnitDuration timeout = seconds(10);
        final Routine<String, String> routine =
                JRoutineService.on(serviceFrom(getActivity(), RemoteTestService.class))
                               .with(factoryOf(StringPassingInvocation.class))
                               .buildRoutine();
        assertThat(routine.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
    }

    public void testSize() {

        final Channel<String, String> channel =
                JRoutineService.on(serviceFrom(getActivity(), RemoteTestService.class))
                               .with(factoryOf(StringPassingInvocation.class))
                               .async();
        assertThat(channel.inputCount()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.inputCount()).isEqualTo(1);
        final Channel<?, String> result = channel.close();
        assertThat(result.after(seconds(10)).hasCompleted()).isTrue();
        assertThat(result.outputCount()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.skipNext(1).outputCount()).isEqualTo(0);
    }

    public void testTransform() {

        assertThat(JRoutineService.on(serviceFrom(getActivity(), RemoteTestService.class))
                                  .with(factoryOf(TestTransform.class))
                                  .async("test1", "test2", "test3")
                                  .after(seconds(10))
                                  .all()).containsExactly("TEST1", "TEST2", "TEST3");
    }

    private static class Abort extends TemplateContextInvocation<Data, Data> {

        @Override
        public void onComplete(@NotNull final Channel<Data, ?> result) {

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
        public void onInput(final Data d, @NotNull final Channel<Data, ?> result) {

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
                @NotNull final Channel<MyParcelable, ?> result) {

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
                @NotNull final Channel<String, ?> result) {

            result.pass(strings);
        }
    }

    private static class StringDelay extends TemplateContextInvocation<String, String> {

        @Override
        public void onInput(final String s, @NotNull final Channel<String, ?> result) {

            result.after(UnitDuration.millis(100)).pass(s);
        }
    }

    private static class StringPassingInvocation extends TemplateContextInvocation<String, String> {

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {

            result.pass(s);
        }
    }

    private static class TestTransform extends StreamContextInvocation<String, String> {

        @NotNull
        @Override
        protected Channel<?, String> onChannel(@NotNull final Channel<?, String> channel) {

            return JRoutineCore.with(new UpperCaseInvocation()).async(channel);
        }
    }

    private static class TextCommandInvocation extends TemplateContextInvocation<Void, String> {

        @Override
        public void onComplete(@NotNull final Channel<String, ?> result) {

            result.pass("test1", "test2", "test3");
        }
    }

    private static class UpperCaseInvocation extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected UpperCaseInvocation() {

            super(null);
        }

        @Override
        public void onInput(final String input, @NotNull final Channel<String, ?> result) {

            result.pass(input.toUpperCase());
        }
    }
}
