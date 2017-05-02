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
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.method.annotation.Input;
import com.github.dm.jrt.method.annotation.Output;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.reflect.InvocationTarget.classOfType;
import static com.github.dm.jrt.reflect.InvocationTarget.instance;
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
    final Channel<String, String> inputStrings = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputLengths = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      void length(@Input final Channel<?, String> input, @Output final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          output.pass(input.next().length());
        }
      }
    }.call(inputStrings, outputLengths);
    inputStrings.pass("test", "test1", "test22");
    assertThat(outputLengths.in(seconds(1)).next(3)).containsOnly(4, 5, 6);
  }

  private static void testStaticInternal2() {
    final Locale locale = Locale.getDefault();
    final RoutineMethod method = new RoutineMethod(defaultExecutor(), locale) {

      String switchCase(@Input final Channel<?, String> input, final boolean isUpper) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
      }
    };
    Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType().pass("test");
    assertThat(method.call(inputChannel, true).in(seconds(1)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.channel().ofType().pass("TEST");
    assertThat(method.call(inputChannel, false).in(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testAbort() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      private int mSum;

      void sum(@Input final Channel<?, Integer> input, @Output final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(Integer.MIN_VALUE);
        }
      }
    }.call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4).abort();
    assertThat(outputChannel.in(seconds(1)).getError()).isExactlyInstanceOf(AbortException.class);
  }

  @Test
  public void testAbort2() {
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

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
    assertThat(outputChannel.in(seconds(1)).getError()).isExactlyInstanceOf(AbortException.class);
  }

  @Test
  public void testAbort3() {
    final Channel<Integer, Integer> inputChannel1 = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> inputChannel2 = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

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
    assertThat(outputChannel.in(seconds(1)).getError()).isExactlyInstanceOf(AbortException.class);
  }

  @Test
  public void testBind() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      public void square(@Input final Channel<?, Integer> input,
          @Output final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          final int i = input.next();
          output.pass(i * i);
        }
      }
    }.call(inputChannel, outputChannel);
    final Channel<Integer, Integer> resultChannel = JRoutineCore.channel().ofType();
    new SumRoutine().call(outputChannel, resultChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(resultChannel.in(seconds(1)).next()).isEqualTo(55);
  }

  @Test
  public void testCall() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      private int mSum;

      public void sum(@Input final Channel<?, Integer> input,
          @Output final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(mSum);
        }
      }
    }.call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(15);
  }

  @Test(expected = IllegalStateException.class)
  public void testCallError() {
    final RoutineMethod method = new RoutineMethod(defaultExecutor()) {

      int zero() {
        return 0;
      }
    };
    assertThat(method.call().in(seconds(1)).next()).isEqualTo(0);
    method.call();
  }

  @Test
  public void testCallSync() {
    final Channel<Integer, Integer> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine(syncExecutor()).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.next()).isEqualTo(15);
  }

  @Test
  public void testFromClass() throws NoSuchMethodException {
    assertThat(RoutineMethod.methodOn(defaultExecutor(),
        RoutineMethodTest.class.getMethod("length", String.class))
                            .call("test")
                            .in(seconds(1))
                            .next()).isEqualTo(4);
    assertThat(RoutineMethod.methodOn(defaultExecutor(),
        RoutineMethodTest.class.getMethod("length", String.class))
                            .call(JRoutineCore.channel().of("test"))
                            .in(seconds(1))
                            .next()).isEqualTo(4);
  }

  @Test
  public void testFromClass2() throws NoSuchMethodException {
    assertThat(
        RoutineMethod.methodOn(defaultExecutor(), classOfType(RoutineMethodTest.class), "length",
            String.class).call("test").in(seconds(1)).next()).isEqualTo(4);
    assertThat(
        RoutineMethod.methodOn(defaultExecutor(), classOfType(RoutineMethodTest.class), "length",
            String.class).call(JRoutineCore.channel().of("test")).in(seconds(1)).next()).isEqualTo(
        4);
    final Channel<String, String> inputChannel = JRoutineCore.channel().ofType();
    final Channel<?, Object> outputChannel =
        RoutineMethod.methodOn(defaultExecutor(), classOfType(RoutineMethodTest.class), "length",
            String.class).call(inputChannel);
    inputChannel.pass("test").close();
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(4);
  }

  @Test
  public void testFromError() throws NoSuchMethodException {
    try {
      RoutineMethod.methodOn(defaultExecutor(), String.class.getMethod("toString"));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      RoutineMethod.methodOn(defaultExecutor(), instance("test"),
          RoutineMethodTest.class.getMethod("length", String.class));
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testFromInstance() throws NoSuchMethodException {
    final String test = "test";
    assertThat(RoutineMethod.methodOn(defaultExecutor(), instance(test),
        String.class.getMethod("toString")).call().in(seconds(1)).next()).isEqualTo("test");
    assertThat(
        RoutineMethod.methodOn(syncExecutor(), instance(test), String.class.getMethod("toString"))
                     .withWrapper()
                     .withSharedFields()
                     .configuration()
                     .call()
                     .next()).isEqualTo("test");
  }

  @Test
  public void testFromInstance2() throws NoSuchMethodException {
    final String test = "test";
    assertThat(RoutineMethod.methodOn(defaultExecutor(), instance(test), "toString")
                            .call()
                            .in(seconds(1))
                            .next()).isEqualTo("test");
    assertThat(RoutineMethod.methodOn(syncExecutor(), instance(test), "toString")
                            .withWrapper()
                            .withSharedFields()
                            .configuration()
                            .call()
                            .next()).isEqualTo("test");
  }

  @Test
  public void testInputFrom2() {
    final Channel<String, Integer> channel =
        JRoutineCore.routine().of(new MappingInvocation<String, Integer>(null) {

          public void onInput(final String input, @NotNull final Channel<Integer, ?> result) {
            result.pass(Integer.parseInt(input));
          }
        }).invoke();
    final Channel<Object, Object> outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine().call(channel, outputChannel);
    channel.pass("1", "2", "3", "4").close();
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(10);
  }

  @Test
  public void testInputs() {
    Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine().call(JRoutineCore.channel().of(), outputChannel);
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(0);
    outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine().call(JRoutineCore.channel().of(1), outputChannel);
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(1);
    outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine().call(JRoutineCore.channel().of(1, 2, 3, 4, 5), outputChannel);
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(15);
    outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine().call(JRoutineCore.channel().of(Arrays.asList(1, 2, 3, 4, 5)), outputChannel);
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(15);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidAnnotationIn() {
    new RoutineMethod(defaultExecutor()) {

      void test(@Input final int ignored) {
      }
    }.call(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidAnnotationInOut() {
    new RoutineMethod(defaultExecutor()) {

      void test(@Input @Output final Channel<?, ?> ignored) {
      }
    }.call(JRoutineCore.channel().of());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidAnnotationOut() {
    new RoutineMethod(defaultExecutor()) {

      void test(@Output final int ignored) {
      }
    }.call(1);
  }

  @Test
  public void testLocal() {
    class SumRoutine extends RoutineMethod {

      private int mSum;

      private SumRoutine(final int i) {
        super(syncExecutor(), RoutineMethodTest.this, i);
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

    final Channel<Integer, Integer> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine(0).call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.all()).containsOnly(15);
  }

  @Test
  public void testLocal2() {
    final int[] i = new int[1];
    class SumRoutine extends RoutineMethod {

      private int mSum;

      private SumRoutine() {
        super(syncExecutor(), RoutineMethodTest.this, i);
      }

      public void sum(@Input final Channel<?, Integer> input,
          @Output final Channel<Integer, ?> output) {
        if (input.hasNext()) {
          mSum += input.next();

        } else {
          output.pass(mSum + i[0]);
        }
      }
    }

    i[0] = 1;
    final Channel<Integer, Integer> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new SumRoutine().call(inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.all()).containsOnly(16);
  }

  @Test
  public void testNoInputs() {
    assertThat(new RoutineMethod(defaultExecutor()) {

      String get() {
        return "test";
      }
    }.call().in(seconds(1)).next()).isEqualTo("test");
    final Channel<String, String> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      void get(@Output final Channel<String, ?> outputChannel) {
        outputChannel.pass("test");
      }
    }.call(outputChannel);
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testParams() {
    final RoutineMethod method = new RoutineMethod(defaultExecutor(), this) {

      String switchCase(@Input final Channel<?, String> input, final boolean isUpper) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase() : str.toLowerCase();
      }
    };
    Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType().pass("test");
    assertThat(method.call(inputChannel, true).in(seconds(1)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.channel().ofType().pass("TEST");
    assertThat(method.call(inputChannel, false).in(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testParams2() {
    final Locale locale = Locale.getDefault();
    final RoutineMethod method = new RoutineMethod(defaultExecutor(), this, locale) {

      String switchCase(@Input final Channel<?, String> input, final boolean isUpper) {
        final String str = input.next();
        return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
      }
    };
    Channel<Object, Object> inputChannel = JRoutineCore.channel().ofType().pass("test");
    assertThat(method.call(inputChannel, true).in(seconds(1)).next()).isEqualTo("TEST");
    inputChannel = JRoutineCore.channel().ofType().pass("TEST");
    assertThat(method.call(inputChannel, false).in(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testReturnValue() {
    final Channel<String, String> inputStrings = JRoutineCore.channel().ofType();
    final Channel<?, Object> outputChannel = new RoutineMethod(defaultExecutor()) {

      int length(@Input final Channel<?, String> input) {
        if (input.hasNext()) {
          return input.next().length();
        }
        return ignoreReturnValue();
      }
    }.call(inputStrings);
    inputStrings.pass("test").close();
    assertThat(outputChannel.in(seconds(1)).all()).containsExactly(4);
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
    final Channel<Integer, Integer> inputInts = JRoutineCore.channel().ofType();
    final Channel<String, String> inputStrings = JRoutineCore.channel().ofType();
    final Channel<String, String> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      void run(@Input final Channel<?, Integer> inputInts,
          @Input final Channel<?, String> inputStrings, @Output final Channel<String, ?> output) {
        output.pass(switchInput().next().toString());
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2");
    inputInts.pass(1, 2, 3);
    assertThat(outputChannel.in(seconds(1)).next(4)).containsExactly("test1", "test2", "1", "2");
  }

  @Test
  public void testSwitchInput2() {
    final Channel<Integer, Integer> inputInts = JRoutineCore.channel().ofType();
    final Channel<String, String> inputStrings = JRoutineCore.channel().ofType();
    final Channel<String, String> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      void run(@Input final Channel<?, Integer> inputInts,
          @Input final Channel<?, String> inputStrings, @Output final Channel<String, ?> output) {
        final Channel<?, ?> inputChannel = switchInput();
        if (inputChannel == inputStrings) {
          output.pass(inputStrings.next());
        }
      }
    }.call(inputInts, inputStrings, outputChannel);
    inputStrings.pass("test1", "test2");
    inputInts.pass(1, 2, 3);
    assertThat(outputChannel.in(seconds(1)).next(2)).containsExactly("test1", "test2");
  }

  @Test
  public void testWrap() {
    final SumRoutineInner routine = new SumRoutineInner(0);
    final Channel<Integer, Integer> inputChannel = JRoutineCore.channel().ofType();
    final Channel<Integer, Integer> outputChannel = JRoutineCore.channel().ofType();
    new RoutineMethod(defaultExecutor()) {

      void run(final SumRoutineInner routine, @Input final Channel<?, Integer> input,
          @Output final Channel<Integer, ?> output) {
        routine.sum(input, output);
      }
    }.call(routine, inputChannel, outputChannel);
    inputChannel.pass(1, 2, 3, 4, 5).close();
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo(15);
  }

  private static class SumRoutine extends RoutineMethod {

    private int mSum;

    private SumRoutine() {
      this(defaultExecutor());
    }

    private SumRoutine(@NotNull final ScheduledExecutor executor) {
      super(executor);
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

  private class SumRoutineInner extends RoutineMethod {

    private int mSum;

    private SumRoutineInner(final int i) {
      this(defaultExecutor(), i);
    }

    private SumRoutineInner(@NotNull final ScheduledExecutor executor, final int i) {
      super(executor, RoutineMethodTest.this, i);
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
}
