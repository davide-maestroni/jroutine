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

package com.github.dm.jrt.method;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.method.annotation.In;
import com.github.dm.jrt.method.annotation.Out;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.object.InvocationTarget.classOfType;
import static com.github.dm.jrt.object.InvocationTarget.instance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine method unit tests.
 * <p>
 * Created by davide-maestroni on 08/10/2016.
 */
@SuppressWarnings("unused")
public class RoutineMethodTest {

  public static int length(final String str) {
    return str.length();
  }

  private static void testStaticInternal() {
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputLengths = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

      void length(@In final Channel<?, String> input, @Out final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          output.pass(input.next().length());
        }
      }
    }.callParallel(inputStrings, outputLengths);
    inputStrings.pass("test", "test1", "test22");
    assertThat(outputLengths.after(seconds(1)).next(3)).containsOnly(4, 5, 6);
  }

  private static void testStaticInternal2() {
    final Locale locale = Locale.getDefault();
    final RoutineMethod method = new RoutineMethod(locale) {

      String switchCase(@In final Channel<?, String> input, final boolean isUpper) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
      }
    };
    Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel().pass("test");
    assertThat(method.call(inputChannel, true).after(seconds(1)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.io().buildChannel().pass("TEST");
    assertThat(method.call(inputChannel, false).after(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testAbort() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

      private int mSum;

      void sum(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(Integer.MIN_VALUE);
        }
      }
    }.call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4).abort();
    assertThat(outputChannel.after(seconds(1)).getError()).isExactlyInstanceOf(
        AbortException.class);
  }

  @Test
  public void testAbort2() {
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

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
    assertThat(outputChannel.after(seconds(1)).getError()).isExactlyInstanceOf(
        AbortException.class);
  }

  @Test
  public void testAbort3() {
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

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
    assertThat(outputChannel.after(seconds(1)).getError()).isExactlyInstanceOf(
        AbortException.class);
  }

  @Test
  public void testBind() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

      public void square(@In final Channel<?, Integer> input,
          @Out final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          final int i = input.next();
          output.pass(i * i);
        }
      }
    }.call(inputChannel, outputChannel);
    final Channel<Integer, Integer> resultChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().call(outputChannel, resultChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(resultChannel.after(seconds(1)).next()).isEqualTo(55);
  }

  @Test
  public void testCall() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

      private int mSum;

      public void sum(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(mSum);
        }
      }
    }.call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(15);
  }

  @Test(expected = IllegalStateException.class)
  public void testCallError() {
    final RoutineMethod method = new RoutineMethod() {

      int zero() {
        return 0;
      }
    };
    assertThat(method.call().after(seconds(1)).next()).isEqualTo(0);
    method.call();
  }

  @Test
  public void testCallSync() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().applyInvocationConfiguration()
                    .withRunner(Runners.syncRunner())
                    .configured()
                    .call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.next()).isEqualTo(15);
  }

  @Test
  public void testFromClass() throws NoSuchMethodException {
    assertThat(RoutineMethod.from(RoutineMethodTest.class.getMethod("length", String.class))
                            .call("test")
                            .after(seconds(1))
                            .next()).isEqualTo(4);
    assertThat(RoutineMethod.from(RoutineMethodTest.class.getMethod("length", String.class))
                            .call(JRoutineCore.io().of("test"))
                            .after(seconds(1))
                            .next()).isEqualTo(4);
    final Channel<String, String> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<?, Object> outputChannel =
        RoutineMethod.from(RoutineMethodTest.class.getMethod("length", String.class))
                     .callParallel(inputChannel);
    inputChannel.pass("test", "test1", "test22").close();
    assertThat(outputChannel.after(seconds(1)).all()).containsOnly(4, 5, 6);
  }

  @Test
  public void testFromClass2() throws NoSuchMethodException {
    assertThat(RoutineMethod.from(classOfType(RoutineMethodTest.class), "length", String.class)
                            .call("test")
                            .after(seconds(1))
                            .next()).isEqualTo(4);
    assertThat(RoutineMethod.from(classOfType(RoutineMethodTest.class), "length", String.class)
                            .call(JRoutineCore.io().of("test"))
                            .after(seconds(1))
                            .next()).isEqualTo(4);
    final Channel<String, String> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<?, Object> outputChannel =
        RoutineMethod.from(classOfType(RoutineMethodTest.class), "length", String.class)
                     .call(inputChannel);
    inputChannel.pass("test").close();
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(4);
  }

  @Test
  public void testFromError() throws NoSuchMethodException {
    try {
      RoutineMethod.from(String.class.getMethod("toString"));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      RoutineMethod.from(instance("test"),
          RoutineMethodTest.class.getMethod("length", String.class));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testFromInstance() throws NoSuchMethodException {
    final String test = "test";
    assertThat(RoutineMethod.from(instance(test), String.class.getMethod("toString"))
                            .call()
                            .after(seconds(1))
                            .next()).isEqualTo("test");
    assertThat(RoutineMethod.from(instance(test), String.class.getMethod("toString"))
                            .applyInvocationConfiguration()
                            .withRunner(Runners.syncRunner())
                            .configured()
                            .applyObjectConfiguration()
                            .withSharedFields()
                            .configured()
                            .call()
                            .next()).isEqualTo("test");
  }

  @Test
  public void testFromInstance2() throws NoSuchMethodException {
    final String test = "test";
    assertThat(
        RoutineMethod.from(instance(test), "toString").call().after(seconds(1)).next()).isEqualTo(
        "test");
    assertThat(RoutineMethod.from(instance(test), "toString")
                            .applyInvocationConfiguration()
                            .withRunner(Runners.syncRunner())
                            .configured()
                            .applyObjectConfiguration()
                            .withSharedFields()
                            .configured()
                            .call()
                            .next()).isEqualTo("test");
  }

  @Test
  public void testInputFrom2() {
    final Channel<String, Integer> channel =
        JRoutineCore.with(new MappingInvocation<String, Integer>(null) {

          public void onInput(final String input, @NotNull final Channel<Integer, ?> result) {
            result.pass(Integer.parseInt(input));
          }
        }).call();
    final Channel<Object, Object> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().call(channel, outputChannel);
    channel.pass("1", "2", "3", "4").close();
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(10);
  }

  @Test
  public void testInputs() {
    Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().call(JRoutineCore.io().of(), outputChannel);
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(0);
    outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().call(JRoutineCore.io().of(1), outputChannel);
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(1);
    outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().call(JRoutineCore.io().of(1, 2, 3, 4, 5), outputChannel);
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(15);
    outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().call(JRoutineCore.io().of(Arrays.asList(1, 2, 3, 4, 5)), outputChannel);
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(15);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidAnnotationIn() {
    new RoutineMethod() {

      void test(@In final int ignored) {
      }
    }.call(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidAnnotationInOut() {
    new RoutineMethod() {

      void test(@In @Out final Channel<?, ?> ignored) {
      }
    }.call(JRoutineCore.io().of());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidAnnotationOut() {
    new RoutineMethod() {

      void test(@Out final int ignored) {
      }
    }.call(1);
  }

  @Test
  public void testLocal() {
    class SumRoutine extends RoutineMethod {

      private int mSum;

      private SumRoutine(final int i) {
        super(RoutineMethodTest.this, i);
      }

      public void sum(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(mSum);
        }
      }
    }

    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine(0).applyInvocationConfiguration()
                     .withRunner(Runners.syncRunner())
                     .configured()
                     .callParallel(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.all()).containsOnly(1, 2, 3, 4, 5);
  }

  @Test
  public void testLocal2() {
    final int[] i = new int[1];
    class SumRoutine extends RoutineMethod {

      private int mSum;

      private SumRoutine() {
        super(RoutineMethodTest.this, i);
      }

      public void sum(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(mSum + i[0]);
        }
      }
    }

    i[0] = 1;
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().applyInvocationConfiguration()
                    .withRunner(Runners.syncRunner())
                    .configured()
                    .callParallel(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.all()).containsOnly(2, 3, 4, 5, 6);
  }

  @Test
  public void testNoInputs() {
    assertThat(new RoutineMethod() {

      String get() {
        return "test";
      }
    }.call().after(seconds(1)).next()).isEqualTo("test");
    final Channel<String, String> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

      void get(@Out final Channel<String, ?> outputChannel) {
        outputChannel.pass("test");
      }
    }.call(outputChannel);
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testParallel() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutine().applyInvocationConfiguration()
                    .withRunner(Runners.syncRunner())
                    .configured()
                    .callParallel(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.all()).containsOnly(1, 2, 3, 4, 5);
  }

  @Test
  public void testParallel2() {
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<?, Object> outputChannel = new RoutineMethod(this) {

      int length(@In final Channel<String, String> input) {
        if (input.hasNext()) {
          return input.next().length();
        }
        return 0;
      }
    }.callParallel(inputStrings);
    inputStrings.pass("test");
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(4);
  }

  @Test
  public void testParallel3() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new SumRoutineInner(0).applyInvocationConfiguration()
                          .withRunner(Runners.syncRunner())
                          .configured()
                          .callParallel(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.all()).containsOnly(1, 2, 3, 4, 5);
  }

  @Test(expected = IllegalStateException.class)
  public void testParallelError() {
    new RoutineMethod() {

      int zero() {
        return 0;
      }
    }.callParallel();
  }

  @Test
  public void testParams() {
    final RoutineMethod method = new RoutineMethod(this) {

      String switchCase(@In final Channel<?, String> input, final boolean isUpper) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase() : str.toLowerCase();
      }
    };
    Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel().pass("test");
    assertThat(method.call(inputChannel, true).after(seconds(1)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.io().buildChannel().pass("TEST");
    assertThat(method.call(inputChannel, false).after(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testParams2() {
    final Locale locale = Locale.getDefault();
    final RoutineMethod method = new RoutineMethod(this, locale) {

      String switchCase(@In final Channel<?, String> input, final boolean isUpper) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
      }
    };
    Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel().pass("test");
    assertThat(method.call(inputChannel, true).after(seconds(1)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.io().buildChannel().pass("TEST");
    assertThat(method.call(inputChannel, false).after(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testReturnValue() {
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<?, Object> outputChannel = new RoutineMethod() {

      int length(@In final Channel<?, String> input) {
        if (input.hasNext()) {
          return input.next().length();
        }
        return ignoreReturnValue();
      }
    }.call(inputStrings);
    inputStrings.pass("test").close();
    assertThat(outputChannel.after(seconds(1)).all()).containsExactly(4);
  }

  @Test
  public void testStatic() {
    testStaticInternal();
  }

  @Test
  public void testStatic2() {
    testStaticInternal2();
  }

  @Test
  public void testSwitchInput() {
    final Channel<Integer, Integer> inputInts = JRoutineCore.io().buildChannel();
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<String, String> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

      void run(@In final Channel<?, Integer> inputInts, @In final Channel<?, String> inputStrings,
          @Out final Channel<String, ?> output) {
        output.pass(switchInput().next().toString());
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2");
    inputInts.pass(1, 2, 3);
    assertThat(outputChannel.after(seconds(1)).next(4)).containsExactly("test1", "test2", "1", "2");
  }

  @Test
  public void testSwitchInput2() {
    final Channel<Integer, Integer> inputInts = JRoutineCore.io().buildChannel();
    final Channel<String, String> inputStrings = JRoutineCore.io().buildChannel();
    final Channel<String, String> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

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
    assertThat(outputChannel.after(seconds(1)).next(2)).containsExactly("test1", "test2");
  }

  @Test
  public void testWrap() {
    final SumRoutineInner routine = new SumRoutineInner(0);
    final Channel<Integer, Integer> inputChannel = JRoutineCore.io().buildChannel();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.io().buildChannel();
    new RoutineMethod() {

      void run(final SumRoutineInner routine, @In final Channel<?, Integer> input,
          @Out final Channel<Integer, ?> output) {
        routine.sum(input, output);
      }
    }.call(routine, inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(15);
  }

  private static class SumRoutine extends RoutineMethod {

    private int mSum;

    public void sum(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
      if (input.hasNext()) {
        mSum += input.next();

      } else {
        output.pass(mSum);
      }
    }
  }

  private class SumRoutineInner extends RoutineMethod {

    private int mSum;

    private SumRoutineInner(final int i) {
      super(RoutineMethodTest.this, i);
    }

    public void sum(@In final Channel<?, Integer> input, @Out final Channel<Integer, ?> output) {
      if (input.hasNext()) {
        mSum += input.next();

      } else {
        output.pass(mSum);
      }
    }
  }
}
