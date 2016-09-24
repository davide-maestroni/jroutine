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

package com.github.dm.jrt.android.v11.method;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.method.TestActivity;
import com.github.dm.jrt.android.method.app.TestApp;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.method.InputChannel;
import com.github.dm.jrt.method.OutputChannel;
import com.github.dm.jrt.method.RoutineMethod;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine method unit tests.
 * <p>
 * Created by davide-maestroni on 08/20/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
@SuppressWarnings("unused")
public class LoaderRoutineMethodTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public LoaderRoutineMethodTest() {
    super(TestActivity.class);
  }

  public static int length(final String str) {
    return str.length();
  }

  private static void testAbort2(@NotNull final Activity activity) {
    final InputChannel<Integer> inputChannel1 = RoutineMethod.inputChannel();
    final InputChannel<Integer> inputChannel2 = RoutineMethod.inputChannel();
    final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      private int mSum;

      void sum(final InputChannel<Integer> input1, final InputChannel<Integer> input2,
          final OutputChannel<Integer> output) {
        final InputChannel<Integer> input = switchInput();
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
    final InputChannel<Integer> inputChannel1 = RoutineMethod.inputChannel();
    final InputChannel<Integer> inputChannel2 = RoutineMethod.inputChannel();
    final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      private int mSum;

      void sum(final InputChannel<Integer> input1, final InputChannel<Integer> input2,
          final OutputChannel<Integer> output) {
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
    final OutputChannel<Boolean> outputChannel = RoutineMethod.outputChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      void test(final OutputChannel<Boolean> output) {
        output.pass(getContext() instanceof TestApp);
      }
    }.call(outputChannel);
    assertThat(outputChannel.after(seconds(10)).all()).containsExactly(true);
  }

  private static void testNoInputs(@NotNull final Activity activity) {
    final LoaderContext context = loaderFrom(activity);
    assertThat(new LoaderRoutineMethod(context) {

      String get() {
        return "test";
      }
    }.call().after(seconds(10)).all()).containsExactly("test");
    final OutputChannel<String> outputChannel = RoutineMethod.outputChannel();
    new LoaderRoutineMethod(context) {

      void get(final OutputChannel<String> outputChannel) {
        outputChannel.pass("test");
      }
    }.call(outputChannel);
    assertThat(outputChannel.after(seconds(10)).all()).containsExactly("test");
  }

  private static void testParams2(@NotNull final Activity activity) {
    final Locale locale = Locale.getDefault();
    final LoaderRoutineMethod method = new LoaderRoutineMethod(loaderFrom(activity), locale) {

      String switchCase(final InputChannel<String> input, final boolean isUpper) {
        if (input.hasNext()) {
          final String str = input.next();
          return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
        }
        return null;
      }
    };
    InputChannel<Object> inputChannel = RoutineMethod.inputChannel().pass("test").close();
    OutputChannel<String> outputChannel = method.call(inputChannel, true);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("TEST");
    inputChannel = RoutineMethod.inputChannel().pass("TEST").close();
    outputChannel = method.call(inputChannel, false);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("test");
  }

  private static void testParams3(@NotNull final Activity activity) {
    final Locale locale = Locale.getDefault();
    final LoaderRoutineMethod method = new LoaderRoutineMethod(loaderFrom(activity)) {

      String switchCase(final InputChannel<String> input, final boolean isUpper) {
        if (input.hasNext()) {
          final String str = input.next();
          return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
        }
        return null;
      }
    };
    final OutputChannel<String> outputChannel = method.call(RoutineMethod.inputOf("test"), true);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("TEST");
    final InputChannel<String> inputChannel = RoutineMethod.inputOf("test");
    try {
      method.call(inputChannel, false);
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  private static void testReturnValue(@NotNull final Activity activity) {
    final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
    final OutputChannel<Object> outputChannel = new LoaderRoutineMethod(loaderFrom(activity)) {

      int length(final InputChannel<String> input) {
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
    final InputChannel<Integer> inputInts = RoutineMethod.inputChannel();
    final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
    final OutputChannel<String> outputChannel = RoutineMethod.outputChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      void run(final InputChannel<Integer> inputInts, final InputChannel<String> inputStrings,
          final OutputChannel<String> output) {
        final InputChannel<Object> inputChannel = switchInput();
        if (inputChannel.hasNext()) {
          output.pass(inputChannel.next().toString());
        }
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2").close();
    inputInts.pass(1, 2, 3).close();
    assertThat(outputChannel.after(seconds(10)).next(4)).containsExactly("test1", "test2", "1",
        "2");
  }

  private static void testSwitchInput2(@NotNull final Activity activity) {
    final InputChannel<Integer> inputInts = RoutineMethod.inputChannel();
    final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
    final OutputChannel<String> outputChannel = RoutineMethod.outputChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      void run(final InputChannel<Integer> inputInts, final InputChannel<String> inputStrings,
          final OutputChannel<String> output) {
        final InputChannel<?> inputChannel = switchInput();
        if (inputChannel == inputStrings) {
          output.pass(inputStrings.next());
        }
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2").close();
    inputInts.pass(1, 2, 3).close();
    assertThat(outputChannel.after(seconds(10)).next(2)).containsExactly("test1", "test2");
  }

  public void testAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
    final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
    new SumRoutine(loaderFrom(getActivity())).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4).abort();
    assertThat(outputChannel.after(seconds(10)).getError()).isExactlyInstanceOf(
        AbortException.class);
  }

  public void testAbort2() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testAbort2(getActivity());
  }

  public void testAbort3() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testAbort3(getActivity());
  }

  public void testBind() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final LoaderContext context = loaderFrom(getActivity());
    final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
    final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
    new SquareRoutine(context).call(inputChannel, outputChannel);
    final OutputChannel<Integer> resultChannel = RoutineMethod.outputChannel();
    new SumRoutine(context).call(RoutineMethod.toInput(outputChannel), resultChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(resultChannel.after(seconds(10)).all()).containsExactly(55);
  }

  public void testCall() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
    final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
    new SumRoutine(loaderFrom(getActivity())).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.after(seconds(10)).all()).containsExactly(15);
  }

  public void testContext() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testContext(getActivity());
  }

  public void testFromClass() throws NoSuchMethodException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()),
        LoaderRoutineMethodTest.class.getMethod("length", String.class))
                                  .call("test")
                                  .after(seconds(10))
                                  .next()).isEqualTo(4);
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()),
        LoaderRoutineMethodTest.class.getMethod("length", String.class))
                                  .call(RoutineMethod.inputOf("test"))
                                  .after(seconds(10))
                                  .next()).isEqualTo(4);
    final InputChannel<String> inputChannel = RoutineMethod.inputChannel();
    final OutputChannel<Object> outputChannel = LoaderRoutineMethod.from(loaderFrom(getActivity()),
        LoaderRoutineMethodTest.class.getMethod("length", String.class)).callParallel(inputChannel);
    inputChannel.pass("test", "test1", "test22").close();
    assertThat(outputChannel.after(seconds(10)).all()).containsOnly(4, 5, 6);
  }

  public void testFromClass2() throws NoSuchMethodException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()),
        classOfType(LoaderRoutineMethodTest.class), "length", String.class)
                                  .call("test")
                                  .after(seconds(10))
                                  .next()).isEqualTo(4);
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()),
        classOfType(LoaderRoutineMethodTest.class), "length", String.class)
                                  .call(RoutineMethod.inputOf("test"))
                                  .after(seconds(10))
                                  .next()).isEqualTo(4);
    final InputChannel<String> inputChannel = RoutineMethod.inputChannel();
    final OutputChannel<Object> outputChannel = LoaderRoutineMethod.from(loaderFrom(getActivity()),
        classOfType(LoaderRoutineMethodTest.class), "length", String.class).call(inputChannel);
    inputChannel.pass("test").close();
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo(4);
  }

  public void testFromError() throws NoSuchMethodException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      LoaderRoutineMethod.from(loaderFrom(getActivity()), String.class.getMethod("toString"));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, "test"),
          LoaderRoutineMethodTest.class.getMethod("length", String.class));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  public void testFromInstance() throws NoSuchMethodException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final String test = "test";
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, test),
        String.class.getMethod("toString")).call().after(seconds(10)).next()).isEqualTo("test");
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, test),
        String.class.getMethod("toString"))
                                  .applyObjectConfiguration()
                                  .withSharedFields()
                                  .configured()
                                  .call()
                                  .after(seconds(10))
                                  .next()).isEqualTo("test");
  }

  public void testFromInstance2() throws NoSuchMethodException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final String test = "test";
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, test),
        "toString").call().after(seconds(10)).next()).isEqualTo("test");
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, test),
        "toString")
                                  .applyObjectConfiguration()
                                  .withSharedFields()
                                  .configured()
                                  .call()
                                  .after(seconds(10))
                                  .next()).isEqualTo("test");
  }

  public void testNoInputs() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testNoInputs(getActivity());
  }

  public void testParallel() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
    final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
    new SumRoutine(loaderFrom(getActivity())).applyInvocationConfiguration()
                                             .withOutputOrder(OrderType.SORTED)
                                             .configured()
                                             .callParallel(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.after(seconds(10)).all()).containsOnly(1, 2, 3, 4, 5);
  }

  public void testParams() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final SwitchCase method = new SwitchCase(loaderFrom(getActivity()));
    InputChannel<Object> inputChannel = RoutineMethod.inputChannel().pass("test").close();
    OutputChannel<String> outputChannel = method.call(inputChannel, true);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("TEST");
    inputChannel = RoutineMethod.inputChannel().pass("TEST").close();
    outputChannel = method.call(inputChannel, false);
    assertThat(outputChannel.after(seconds(10)).next()).isEqualTo("test");
  }

  public void testParams2() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testParams2(getActivity());
  }

  public void testParams3() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testParams3(getActivity());
  }

  public void testReturnValue() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testReturnValue(getActivity());
  }

  public void testStaticScopeError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      new LoaderRoutineMethod(loaderFrom(getActivity())) {};
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  public void testSwitchInput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSwitchInput(getActivity());
  }

  public void testSwitchInput2() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSwitchInput2(getActivity());
  }

  private static class SquareRoutine extends LoaderRoutineMethod {

    public SquareRoutine(@NotNull final LoaderContext context) {
      super(context);
    }

    public void square(final InputChannel<Integer> input, final OutputChannel<Integer> output) {
      if (input.hasNext()) {
        final int i = input.next();
        output.pass(i * i);
      }
    }
  }

  private static class SumRoutine extends LoaderRoutineMethod {

    private int mSum;

    public SumRoutine(@NotNull final LoaderContext context) {
      super(context);
    }

    public void sum(final InputChannel<Integer> input, final OutputChannel<Integer> output) {
      if (input.hasNext()) {
        mSum += input.next();

      } else {
        output.pass(mSum);
      }
    }
  }

  private static class SwitchCase extends LoaderRoutineMethod {

    public SwitchCase(@NotNull final LoaderContext context) {
      super(context);
    }

    String switchCase(final InputChannel<String> input, final boolean isUpper) {
      if (input.hasNext()) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase() : str.toLowerCase();
      }
      return null;
    }
  }
}
