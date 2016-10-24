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

package com.github.dm.jrt.android.method;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.method.annotation.In;
import com.github.dm.jrt.method.annotation.Out;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service routine method unit tests.
 * <p>
 * Created by davide-maestroni on 08/18/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
@SuppressWarnings("unused")
public class ServiceRoutineMethodTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public ServiceRoutineMethodTest() {
    super(TestActivity.class);
  }

  public static int length(final String str) {
    return str.length();
  }

  private static void testAbort2(@NotNull final Activity activity) {
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new ServiceRoutineMethod(serviceFrom(activity)) {

      private int mSum;

      void sum(@In final Channel<?, Integer> input1, @In final Channel<?, Integer> input2,
          @Out final Channel<Integer, ?> output) {
        final Channel<?, Integer> input = switchInput();
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(mSum);
        }
      }
    }.call(inputChannel1, inputChannel2, outputChannel);
    inputChannel1.pass(1, 2, 3, 4);
    inputChannel2.abort();
    assertThat(outputChannel.after(seconds(10)).getError()).isExactlyInstanceOf(
        AbortException.class);
  }

  private static void testAbort3(@NotNull final Activity activity) {
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new ServiceRoutineMethod(serviceFrom(activity)) {

      private int mSum;

      void sum(@In final Channel<?, Integer> input1, @In final Channel<?, Integer> input2,
          @Out final Channel<Integer, ?> output) {
        if (input1.equals(switchInput())) {
          if (input1.hasNext()) {
            mSum += input1.next();

          } else {
            output.pass(mSum);
          }
        }
      }
    }.call(inputChannel1, inputChannel2, outputChannel);
    inputChannel1.pass(1, 2, 3, 4);
    inputChannel2.abort();
    assertThat(outputChannel.after(seconds(10)).getError()).isExactlyInstanceOf(
        AbortException.class);
  }

  private static void testContext(@NotNull final Activity activity) {
    final Channel<Boolean, Boolean> outputChannel = JRoutineCore.io().buildChannel();
    new ServiceRoutineMethod(serviceFrom(activity)) {

      void test(@Out final Channel<Boolean, ?> output) {
        output.pass(getContext() instanceof InvocationService);
      }
    }.call(outputChannel);
    assertThat(outputChannel.after(seconds(10)).all()).containsExactly(true);
  }

  private static void testNoInputs(@NotNull final Activity activity) {
    final ServiceContext context = serviceFrom(activity);
    assertThat(new ServiceRoutineMethod(context) {

      String get() {
        return "test";
      }
    }.call().after(seconds(10)).all()).containsExactly("test");
    final Channel<String, String> outputChannel = JRoutineCore.io().buildChannel();
    new ServiceRoutineMethod(context) {

      void get(@Out final Channel<String, ?> outputChannel) {
        outputChannel.pass("test");
      }
    }.call(outputChannel);
    assertThat(outputChannel.after(seconds(10)).all()).containsExactly("test");
  }

  private static void testParams2(@NotNull final Activity activity) {
    final Locale locale = Locale.getDefault();
    final ServiceRoutineMethod method = new ServiceRoutineMethod(serviceFrom(activity), locale) {

      String switchCase(@In final Channel<?, String> input, final boolean isUpper) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
      }
    };
    Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel().pass("test");
    Channel<?, String> outputChannel = method.call(inputChannel, true);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("TEST");
    inputChannel.close();
    outputChannel.after(seconds(10)).getComplete();
    inputChannel = JRoutineCore.io().buildChannel().pass("TEST");
    outputChannel = method.call(inputChannel, false);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("test");
    inputChannel.close();
    outputChannel.after(seconds(10)).getComplete();
  }

  private static void testReturnValue(@NotNull final Activity activity) {
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<?, Object> outputChannel = new ServiceRoutineMethod(serviceFrom(activity)) {

      int length(@In final Channel<?, String> input) {
        if (input.hasNext()) {
          return input.next().length();
        }
        return ignoreReturnValue();
      }
    }.call(inputStrings);
    inputStrings.pass("test").close();
    assertThat(outputChannel.after(seconds(10)).all()).containsExactly(4);
  }

  private static void testSwitchInput(@NotNull final Activity activity) {
    final Channel<Integer, Integer> inputInts = JRoutineCore.io().buildChannel();
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<String, String> outputChannel = JRoutineCore.io().buildChannel();
    new ServiceRoutineMethod(serviceFrom(activity)) {

      void run(@In final Channel<?, Integer> inputInts, @In final Channel<?, String> inputStrings,
          @Out final Channel<String, ?> output) {
        output.pass(switchInput().next().toString());
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2");
    inputInts.pass(1, 2, 3);
    assertThat(outputChannel.after(seconds(10)).next(4)).containsExactly("test1", "test2", "1",
        "2");
    inputStrings.abort();
    outputChannel.after(seconds(10)).getComplete();
  }

  private static void testSwitchInput2(@NotNull final Activity activity) {
    final Channel<Integer, Integer> inputInts = JRoutineCore.io().buildChannel();
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<String, String> outputChannel = JRoutineCore.io().buildChannel();
    new ServiceRoutineMethod(serviceFrom(activity)) {

      void run(@In final Channel<?, Integer> inputInts, @In final Channel<?, String> inputStrings,
          @Out final Channel<String, ?> output) {
        final Channel<?, ?> inputChannel = switchInput();
        if (inputChannel == inputStrings) {
          output.pass(inputStrings.next());
        }
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2");
    inputInts.pass(1, 2, 3);
    assertThat(outputChannel.after(seconds(10)).next(2)).containsExactly("test1", "test2");
    inputStrings.abort();
    outputChannel.after(seconds(10)).getComplete();
  }

  public void testAbort() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine(serviceFrom(getActivity())).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4).abort();
    assertThat(outputChannel.after(seconds(10)).getError()).isExactlyInstanceOf(
        AbortException.class);
  }

  public void testAbort2() {
    testAbort2(getActivity());
  }

  public void testAbort3() {
    testAbort3(getActivity());
  }

  public void testBind() {
    final ServiceContext context = serviceFrom(getActivity());
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SquareRoutine(context).call(inputChannel, outputChannel);
    final Channel<Integer, Integer> resultChannel = JRoutineCore.io().buildChannel();
    new SumRoutine(context).call(outputChannel, resultChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(resultChannel.after(seconds(10)).all()).containsExactly(55);
  }

  public void testCall() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine(serviceFrom(getActivity())).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.after(seconds(10)).all()).containsExactly(15);
  }

  public void testContext() {
    testContext(getActivity());
  }

  public void testFromClass() throws NoSuchMethodException {
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()),
        ServiceRoutineMethodTest.class.getMethod("length", String.class))
                                   .call("test")
                                   .after(seconds(10))
                                   .next()).isEqualTo(4);
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()),
        ServiceRoutineMethodTest.class.getMethod("length", String.class))
                                   .call(JRoutineCore.io().of("test"))
                                   .after(seconds(10))
                                   .next()).isEqualTo(4);
    final Channel<String, String> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<?, Object> outputChannel = ServiceRoutineMethod.from(serviceFrom(getActivity()),
        ServiceRoutineMethodTest.class.getMethod("length", String.class))
                                                                 .callParallel(inputChannel);
    inputChannel.pass("test", "test1", "test22").close();
    assertThat(outputChannel.after(seconds(10)).all()).containsOnly(4, 5, 6);
  }

  public void testFromClass2() throws NoSuchMethodException {
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()),
        classOfType(ServiceRoutineMethodTest.class), "length", String.class)
                                   .call("test")
                                   .after(seconds(10))
                                   .next()).isEqualTo(4);
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()),
        classOfType(ServiceRoutineMethodTest.class), "length", String.class)
                                   .call(JRoutineCore.io().of("test"))
                                   .after(seconds(10))
                                   .next()).isEqualTo(4);
    final Channel<String, String> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<?, Object> outputChannel = ServiceRoutineMethod.from(serviceFrom(getActivity()),
        classOfType(ServiceRoutineMethodTest.class), "length", String.class).call(inputChannel);
    inputChannel.pass("test").close();
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo(4);
  }

  public void testFromError() throws NoSuchMethodException {
    try {
      ServiceRoutineMethod.from(serviceFrom(getActivity()), String.class.getMethod("toString"));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      ServiceRoutineMethod.from(serviceFrom(getActivity()), instanceOf(String.class, "test"),
          ServiceRoutineMethodTest.class.getMethod("length", String.class));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  public void testFromInstance() throws NoSuchMethodException {
    final String test = "test";
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()), instanceOf(String.class, test),
        String.class.getMethod("toString")).call().after(seconds(10)).next()).isEqualTo("test");
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()), instanceOf(String.class, test),
        String.class.getMethod("toString"))
                                   .applyObjectConfiguration()
                                   .withSharedFields()
                                   .configured()
                                   .call()
                                   .after(seconds(10))
                                   .next()).isEqualTo("test");
  }

  public void testFromInstance2() throws NoSuchMethodException {
    final String test = "test";
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()), instanceOf(String.class, test),
        "toString").call().after(seconds(10)).next()).isEqualTo("test");
    assertThat(ServiceRoutineMethod.from(serviceFrom(getActivity()), instanceOf(String.class, test),
        "toString")
                                   .applyObjectConfiguration()
                                   .withSharedFields()
                                   .configured()
                                   .call()
                                   .after(seconds(10))
                                   .next()).isEqualTo("test");
  }

  public void testNoInputs() {
    testNoInputs(getActivity());
  }

  public void testParallel() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine(serviceFrom(getActivity())).applyInvocationConfiguration()
                                              .withOutputOrder(OrderType.SORTED)
                                              .configured()
                                              .callParallel(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.after(seconds(10)).all()).containsOnly(1, 2, 3, 4, 5);
  }

  public void testParams() {
    final SwitchCase method = new SwitchCase(serviceFrom(getActivity()));
    Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel().pass("test");
    Channel<?, String> outputChannel = method.call(inputChannel, true);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("TEST");
    inputChannel.close();
    outputChannel.after(seconds(10)).getComplete();
    inputChannel = JRoutineCore.io().buildChannel().pass("TEST");
    outputChannel = method.call(inputChannel, false);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("test");
    inputChannel.close();
    outputChannel.after(seconds(10)).getComplete();
  }

  public void testParams2() {
    testParams2(getActivity());
  }

  public void testReturnValue() {
    testReturnValue(getActivity());
  }

  public void testStaticScopeError() {
    try {
      new ServiceRoutineMethod(serviceFrom(getActivity())) {};
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  public void testSwitchInput() {
    testSwitchInput(getActivity());
  }

  public void testSwitchInput2() {
    testSwitchInput2(getActivity());
  }

  private static class SquareRoutine extends ServiceRoutineMethod {

    public SquareRoutine(@NotNull final ServiceContext context) {
      super(context);
    }

    public void square(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
      if (input.hasNext()) {
        final int i = input.next();
        output.pass(i * i);
      }
    }
  }

  private static class SumRoutine extends ServiceRoutineMethod {

    private int mSum;

    public SumRoutine(@NotNull final ServiceContext context) {
      super(context);
    }

    public void sum(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
      if (input.hasNext()) {
        mSum += input.next();

      } else {
        output.pass(mSum);
      }
    }
  }

  private static class SwitchCase extends ServiceRoutineMethod {

    public SwitchCase(@NotNull final ServiceContext context) {
      super(context);
    }

    String switchCase(@In final Channel<?, String> input, final boolean isUpper) {
      final String str = input.next();
      return (isUpper) ? str.toUpperCase() : str.toLowerCase();
    }
  }
}
