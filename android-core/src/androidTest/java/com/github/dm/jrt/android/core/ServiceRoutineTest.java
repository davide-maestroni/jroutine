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
import com.github.dm.jrt.android.core.executor.MainExecutor;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationWrapper;
import com.github.dm.jrt.android.core.invocation.InvocationFactoryReference;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.log.AndroidLog;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.android.core.invocation.InvocationFactoryReference.factoryOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
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

    final DurationMeasure timeout = seconds(10);
    final Data data = new Data();
    final Channel<?, Data> channel =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withService()
                       .withExecutorClass(MainExecutor.class)
                       .configuration()
                       .of(factoryOf(Delay.class))
                       .invoke()
                       .pass(data)
                       .close();
    assertThat(channel.abort(new IllegalArgumentException("test"))).isTrue();

    try {

      channel.in(timeout).next();

      fail();

    } catch (final AbortException e) {

      assertThat(e.getCause().getMessage()).isEqualTo("test");
    }

    try {

      JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                     .of(factoryOf(Abort.class))
                     .invoke()
                     .close()
                     .in(timeout)
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
    final ServiceSource context = null;

    try {

      JRoutineService.routineOn(context).of(factoryOf(classToken));

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineService.routineOn(ServiceSource.serviceOf(getActivity())).of(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                     .of(factoryOf((ClassToken<StringPassingInvocation>) null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testConfigurationErrors() {

    try {

      new DefaultServiceRoutineBuilder(ServiceSource.serviceOf(getActivity())).withConfiguration(
          (InvocationConfiguration) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      new DefaultServiceRoutineBuilder(ServiceSource.serviceOf(getActivity())).withConfiguration(
          (ServiceConfiguration) null);

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

    final DurationMeasure timeout = seconds(10);
    final InvocationFactoryReference<String, String> targetFactory =
        factoryOf(new PassingWrapper<String>());
    final Routine<String, String> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withInputOrder(OrderType.UNSORTED)
                       .withLogLevel(Level.DEBUG)
                       .configuration()
                       .withService()
                       .withLogClass(AndroidLog.class)
                       .configuration()
                       .of(targetFactory);
    assertThat(
        routine.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
  }

  public void testExecutionTimeout() {

    final Channel<?, String> channel =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withOutputTimeout(millis(10))
                       .withOutputTimeoutAction(TimeoutActionType.CONTINUE)
                       .configuration()
                       .of(factoryOf(StringDelay.class))
                       .invoke()
                       .pass("test1")
                       .close();
    assertThat(channel.all()).isEmpty();
    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
  }

  public void testExecutionTimeout2() {

    final Channel<?, String> channel =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withOutputTimeout(millis(10))
                       .withOutputTimeoutAction(TimeoutActionType.ABORT)
                       .configuration()
                       .of(factoryOf(StringDelay.class))
                       .invoke()
                       .pass("test2")
                       .close();

    try {

      channel.all();

      fail();

    } catch (final AbortException ignored) {

    }

    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
  }

  public void testExecutionTimeout3() {

    final Channel<?, String> channel =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withOutputTimeout(millis(10))
                       .withOutputTimeoutAction(TimeoutActionType.FAIL)
                       .configuration()
                       .of(factoryOf(StringDelay.class))
                       .invoke()
                       .pass("test3")
                       .close();

    try {

      channel.all();

      fail();

    } catch (final OutputTimeoutException ignored) {

    }

    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
  }

  public void testInvocations() throws InterruptedException {

    final DurationMeasure timeout = seconds(10);
    final InvocationFactoryReference<String, String> targetFactory =
        factoryOf(StringPassingInvocation.class);
    final Routine<String, String> routine1 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withInputOrder(OrderType.UNSORTED)
                       .withLogLevel(Level.DEBUG)
                       .configuration()
                       .withService()
                       .withLogClass(AndroidLog.class)
                       .configuration()
                       .of(targetFactory);
    assertThat(
        routine1.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
    assertThat(JRoutineCore.routine()
                           .of(InvocationFactory.factoryOf(routine1))
                           .invoke()
                           .pass("1", "2", "3", "4", "5")
                           .close()
                           .in(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
  }

  public void testInvocations2() throws InterruptedException {

    final DurationMeasure timeout = seconds(10);
    final ClassToken<StringCallInvocation> token = tokenOf(StringCallInvocation.class);
    final Routine<String, String> routine2 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withOutputOrder(OrderType.UNSORTED)
                       .withLogLevel(Level.DEBUG)
                       .configuration()
                       .withService()
                       .withLogClass(AndroidLog.class)
                       .configuration()
                       .of(factoryOf(token));
    assertThat(
        routine2.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsExactly(
        "1", "2", "3", "4", "5");
    assertThat(JRoutineCore.routine()
                           .of(InvocationFactory.factoryOf(routine2))
                           .invoke()
                           .pass("1", "2", "3", "4", "5")
                           .close()
                           .in(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
  }

  public void testInvocations3() throws InterruptedException {

    final DurationMeasure timeout = seconds(10);
    final InvocationFactoryReference<String, String> targetFactory =
        factoryOf(StringCallInvocation.class);
    final Routine<String, String> routine3 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withInputOrder(OrderType.SORTED)
                       .withOutputOrder(OrderType.SORTED)
                       .configuration()
                       .of(targetFactory);
    assertThat(
        routine3.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsExactly(
        "1", "2", "3", "4", "5");
    assertThat(JRoutineCore.routine()
                           .of(InvocationFactory.factoryOf(routine3))
                           .invoke()
                           .pass("1", "2", "3", "4", "5")
                           .close()
                           .in(timeout)
                           .all()).containsExactly("1", "2", "3", "4", "5");
  }

  public void testInvocations4() throws InterruptedException {

    final DurationMeasure timeout = seconds(10);
    final InvocationFactoryReference<String, String> targetFactory =
        factoryOf(StringCallInvocation.class);
    final Routine<String, String> routine4 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withCoreInvocations(0)
                       .withMaxInvocations(2)
                       .configuration()
                       .of(targetFactory);
    assertThat(
        routine4.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
    assertThat(JRoutineCore.routine()
                           .of(InvocationFactory.factoryOf(routine4))
                           .invoke()
                           .pass("1", "2", "3", "4", "5")
                           .close()
                           .in(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
  }

  public void testInvocations5() throws InterruptedException {

    final DurationMeasure timeout = seconds(10);
    final InvocationFactoryReference<Void, String> targetFactory =
        factoryOf(TextCommandInvocation.class);
    final Routine<Void, String> routine5 =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .withInvocation()
                       .withCoreInvocations(0)
                       .withMaxInvocations(2)
                       .configuration()
                       .of(targetFactory);
    assertThat(routine5.invoke().close().in(timeout).all()).containsOnly("test1", "test2", "test3");
    assertThat(JRoutineCore.routine()
                           .of(InvocationFactory.factoryOf(routine5))
                           .invoke()
                           .close()
                           .in(timeout)
                           .all()).containsOnly("test1", "test2", "test3");
  }

  public void testParcelable() {

    final DurationMeasure timeout = seconds(10);
    final MyParcelable p = new MyParcelable(33, -17);
    assertThat(JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                              .of(factoryOf(MyParcelableInvocation.class))
                              .invoke()
                              .pass(p)
                              .close()
                              .in(timeout)
                              .next()).isEqualTo(p);
  }

  public void testService() {

    final DurationMeasure timeout = seconds(10);
    final Routine<String, String> routine =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity(), TestService.class))
                       .of(factoryOf(StringPassingInvocation.class));
    assertThat(
        routine.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
    assertThat(JRoutineCore.routine()
                           .of(InvocationFactory.factoryOf(routine))
                           .invoke()
                           .pass("1", "2", "3", "4", "5")
                           .close()
                           .in(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
  }

  public void testSize() {

    final Channel<String, String> channel =
        JRoutineService.routineOn(ServiceSource.serviceOf(getActivity()))
                       .of(factoryOf(StringPassingInvocation.class))
                       .invoke();
    assertThat(channel.inputSize()).isEqualTo(0);
    channel.after(millis(500)).pass("test");
    assertThat(channel.inputSize()).isEqualTo(1);
    final Channel<?, String> result = channel.afterNoDelay().close();
    assertThat(result.in(seconds(10)).getComplete()).isTrue();
    assertThat(result.outputSize()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.skipNext(1).outputSize()).isEqualTo(0);
  }

  private static class Abort extends TemplateContextInvocation<Data, Data> {

    @Override
    public void onComplete(@NotNull final Channel<Data, ?> result) {
      try {
        Thread.sleep(500);

      } catch (final InterruptedException e) {
        throw new InterruptedInvocationException(e);
      }

      result.abort(new IllegalStateException("test"));
    }

    @Override
    public boolean onRecycle() {
      return true;
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
      result.after(DurationMeasure.millis(500)).pass(d);
    }

    @Override
    public boolean onRecycle() {
      return true;
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

    @Override
    public void onInput(final MyParcelable myParcelable,
        @NotNull final Channel<MyParcelable, ?> result) {
      result.pass(myParcelable);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class PassingWrapper<DATA> extends ContextInvocationWrapper<DATA, DATA> {

    public PassingWrapper() {

      super(IdentityInvocation.<DATA>factory().newInvocation());
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
      result.after(DurationMeasure.millis(100)).pass(s);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class StringPassingInvocation extends TemplateContextInvocation<String, String> {

    @Override
    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      result.pass(s);
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class TextCommandInvocation extends TemplateContextInvocation<Void, String> {

    @Override
    public void onComplete(@NotNull final Channel<String, ?> result) {
      result.pass("test1", "test2", "test3");
    }

    @Override
    public boolean onRecycle() {
      return true;
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
