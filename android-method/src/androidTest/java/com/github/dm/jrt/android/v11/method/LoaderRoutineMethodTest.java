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
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.method.annotation.Input;
import com.github.dm.jrt.method.annotation.Output;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
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
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      private int mSum;

      void sum(@Input final Channel<?, Integer> input1, @Input final Channel<?, Integer> input2,
          @Output final Channel<Integer, ?> output) {
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
    assertThat(outputChannel.in(seconds(10)).getError()).isExactlyInstanceOf(AbortException.class);
  }

  private static void testAbort3(@NotNull final Activity activity) {
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      private int mSum;

      void sum(@Input final Channel<?, Integer> input1, @Input final Channel<?, Integer> input2,
          @Output final Channel<Integer, ?> output) {
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
    assertThat(outputChannel.in(seconds(10)).getError()).isExactlyInstanceOf(AbortException.class);
  }

  private static void testContext(@NotNull final Activity activity) {
    final Channel<Boolean, Boolean> outputChannel = JRoutineCore.<Boolean>ofInputs().buildChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      void test(@Output final Channel<Boolean, ?> output) {
        output.pass(getContext() instanceof TestApp);
      }
    }.call(outputChannel);
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly(true);
  }

  private static void testNoInputs(@NotNull final Activity activity) {
    final LoaderContext context = loaderFrom(activity);
    assertThat(new LoaderRoutineMethod(context) {

      String get() {
        return "test";
      }
    }.call().in(seconds(10)).all()).containsExactly("test");
    final Channel<String, String> outputChannel = JRoutineCore.<String>ofInputs().buildChannel();
    new LoaderRoutineMethod(context) {

      void get(@Output final Channel<String, ?> outputChannel) {
        outputChannel.pass("test");
      }
    }.call(outputChannel);
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly("test");
  }

  private static void testParams2(@NotNull final Activity activity) {
    final Locale locale = Locale.getDefault();
    final LoaderRoutineMethod method = new LoaderRoutineMethod(loaderFrom(activity), locale) {

      String switchCase(@Input final Channel<?, String> input, final boolean isUpper) {
        if (input.hasNext()) {
          final String str = input.next();
          return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
        }
        return null;
      }
    };
    Channel<Object, Object> inputChannel =
        JRoutineCore.ofInputs().buildChannel().pass("test").close();
    Channel<?, String> outputChannel = method.call(inputChannel, true);
    assertThat(outputChannel.in(seconds(10)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.ofInputs().buildChannel().pass("TEST").close();
    outputChannel = method.call(inputChannel, false);
    assertThat(outputChannel.in(seconds(10)).next()).isEqualTo("test");
  }

  private static void testParams3(@NotNull final Activity activity) {
    final Locale locale = Locale.getDefault();
    final LoaderRoutineMethod method = new LoaderRoutineMethod(loaderFrom(activity)) {

      String switchCase(@Input final Channel<?, String> input, final boolean isUpper) {
        if (input.hasNext()) {
          final String str = input.next();
          return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
        }
        return null;
      }
    };
    final Channel<?, Object> outputChannel =
        method.call(JRoutineCore.of("test").buildChannel(), true);
    assertThat(outputChannel.in(seconds(10)).next()).isEqualTo("TEST");
    final Channel<?, String> inputChannel = JRoutineCore.of("test").buildChannel();
    try {
      method.call(inputChannel, false);
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  private static void testReturnValue(@NotNull final Activity activity) {
    final Channel<String, String> inputStrings = JRoutineCore.<String>ofInputs().buildChannel();
    final Channel<?, Object> outputChannel = new LoaderRoutineMethod(loaderFrom(activity)) {

      int length(@Input final Channel<?, String> input) {
        if (input.hasNext()) {
          return input.next().length();
        }
        return ignoreReturnValue();
      }
    }.call(inputStrings);
    inputStrings.pass("test").close();
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly(4);
  }

  private static void testSwitchInput(@NotNull final Activity activity) {
    final Channel<Integer, Integer> inputInts = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<String, String> inputStrings = JRoutineCore.<String>ofInputs().buildChannel();
    final Channel<String, String> outputChannel = JRoutineCore.<String>ofInputs().buildChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      void run(@Input final Channel<?, Integer> inputInts,
          @Input final Channel<?, String> inputStrings, @Output final Channel<String, ?> output) {
        final Channel<?, Object> inputChannel = switchInput();
        if (inputChannel.hasNext()) {
          output.pass(inputChannel.next().toString());
        }
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2").close();
    inputInts.pass(1, 2, 3).close();
    assertThat(outputChannel.in(seconds(10)).next(4)).containsExactly("test1", "test2", "1", "2");
  }

  private static void testSwitchInput2(@NotNull final Activity activity) {
    final Channel<Integer, Integer> inputInts = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<String, String> inputStrings = JRoutineCore.<String>ofInputs().buildChannel();
    final Channel<String, String> outputChannel = JRoutineCore.<String>ofInputs().buildChannel();
    new LoaderRoutineMethod(loaderFrom(activity)) {

      void run(@Input final Channel<?, Integer> inputInts,
          @Input final Channel<?, String> inputStrings, @Output final Channel<String, ?> output) {
        final Channel<?, ?> inputChannel = switchInput();
        if (inputChannel == inputStrings) {
          output.pass(inputStrings.next());
        }
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2").close();
    inputInts.pass(1, 2, 3).close();
    assertThat(outputChannel.in(seconds(10)).next(2)).containsExactly("test1", "test2");
  }

  public void testAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<Integer, Integer> inputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    new SumRoutine(loaderFrom(getActivity())).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4).abort();
    assertThat(outputChannel.in(seconds(10)).getError()).isExactlyInstanceOf(AbortException.class);
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
    final Channel<Integer, Integer> inputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    new SquareRoutine(context).call(inputChannel, outputChannel);
    final Channel<Integer, Integer> resultChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    new SumRoutine(context).call(outputChannel, resultChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(resultChannel.in(seconds(10)).all()).containsExactly(55);
  }

  public void testCall() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final Channel<Integer, Integer> inputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    new SumRoutine(loaderFrom(getActivity())).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsExactly(15);
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
                                  .in(seconds(10))
                                  .next()).isEqualTo(4);
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()),
        LoaderRoutineMethodTest.class.getMethod("length", String.class))
                                  .call(JRoutineCore.of("test").buildChannel())
                                  .in(seconds(10))
                                  .next()).isEqualTo(4);
    final Channel<String, String> inputChannel = JRoutineCore.<String>ofInputs().buildChannel();
    final Channel<?, Object> outputChannel = LoaderRoutineMethod.from(loaderFrom(getActivity()),
        LoaderRoutineMethodTest.class.getMethod("length", String.class)).callParallel(inputChannel);
    inputChannel.pass("test", "test1", "test22").close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(4, 5, 6);
  }

  public void testFromClass2() throws NoSuchMethodException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()),
        classOfType(LoaderRoutineMethodTest.class), "length", String.class)
                                  .call("test")
                                  .in(seconds(10))
                                  .next()).isEqualTo(4);
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()),
        classOfType(LoaderRoutineMethodTest.class), "length", String.class)
                                  .call(JRoutineCore.of("test").buildChannel())
                                  .in(seconds(10))
                                  .next()).isEqualTo(4);
    final Channel<String, String> inputChannel = JRoutineCore.<String>ofInputs().buildChannel();
    final Channel<?, Object> outputChannel = LoaderRoutineMethod.from(loaderFrom(getActivity()),
        classOfType(LoaderRoutineMethodTest.class), "length", String.class).call(inputChannel);
    inputChannel.pass("test").close();
    assertThat(outputChannel.in(seconds(10)).next()).isEqualTo(4);
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
        String.class.getMethod("toString")).call().in(seconds(10)).next()).isEqualTo("test");
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, test),
        String.class.getMethod("toString"))
                                  .applyObjectConfiguration()
                                  .withSharedFields()
                                  .configured()
                                  .call()
                                  .in(seconds(10))
                                  .next()).isEqualTo("test");
  }

  public void testFromInstance2() throws NoSuchMethodException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final String test = "test";
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, test),
        "toString").call().in(seconds(10)).next()).isEqualTo("test");
    assertThat(LoaderRoutineMethod.from(loaderFrom(getActivity()), instanceOf(String.class, test),
        "toString")
                                  .applyObjectConfiguration()
                                  .withSharedFields()
                                  .configured()
                                  .call()
                                  .in(seconds(10))
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

    final Channel<Integer, Integer> inputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.<Integer>ofInputs().buildChannel();
    new SumRoutine(loaderFrom(getActivity())).applyInvocationConfiguration()
                                             .withOutputOrder(OrderType.SORTED)
                                             .configured()
                                             .callParallel(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.in(seconds(10)).all()).containsOnly(1, 2, 3, 4, 5);
  }

  public void testParams() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final SwitchCase method = new SwitchCase(loaderFrom(getActivity()));
    Channel<Object, Object> inputChannel =
        JRoutineCore.ofInputs().buildChannel().pass("test").close();
    Channel<?, String> outputChannel = method.call(inputChannel, true);
    assertThat(outputChannel.in(seconds(10)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.ofInputs().buildChannel().pass("TEST").close();
    outputChannel = method.call(inputChannel, false);
    assertThat(outputChannel.in(seconds(10)).next()).isEqualTo("test");
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

    public void square(@Input final Channel<?, Integer> input,
        @Output final Channel<Integer, ?> output) {
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

    public void sum(@Input final Channel<?, Integer> input,
        @Output final Channel<Integer, ?> output) {
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

    String switchCase(@Input final Channel<?, String> input, final boolean isUpper) {
      if (input.hasNext()) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase() : str.toLowerCase();
      }
      return null;
    }
  }
}
