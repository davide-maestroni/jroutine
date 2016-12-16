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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.Functions.biSink;
import static com.github.dm.jrt.function.Functions.castTo;
import static com.github.dm.jrt.function.Functions.constant;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.decorate;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.identity;
import static com.github.dm.jrt.function.Functions.isEqualTo;
import static com.github.dm.jrt.function.Functions.isInstanceOf;
import static com.github.dm.jrt.function.Functions.isNotNull;
import static com.github.dm.jrt.function.Functions.isNull;
import static com.github.dm.jrt.function.Functions.isSameAs;
import static com.github.dm.jrt.function.Functions.max;
import static com.github.dm.jrt.function.Functions.maxBy;
import static com.github.dm.jrt.function.Functions.min;
import static com.github.dm.jrt.function.Functions.minBy;
import static com.github.dm.jrt.function.Functions.negative;
import static com.github.dm.jrt.function.Functions.noOp;
import static com.github.dm.jrt.function.Functions.positive;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.sink;
import static com.github.dm.jrt.function.Functions.supplierCommand;
import static com.github.dm.jrt.function.Functions.supplierFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Functions unit tests.
 * <p>
 * Created by davide-maestroni on 09/24/2015.
 */
public class FunctionsTest {

  @NotNull
  private static CommandInvocation<String> createCommand() {

    return consumerCommand(new Consumer<Channel<String, ?>>() {

      public void accept(final Channel<String, ?> result) {

        result.pass("test");
      }
    });
  }

  @NotNull
  private static CommandInvocation<String> createCommand2() {

    return supplierCommand(new Supplier<String>() {

      public String get() {

        return "test";
      }
    });
  }

  @NotNull
  private static InvocationFactory<Object, String> createFactory() {

    return supplierFactory(new Supplier<Invocation<Object, String>>() {

      public Invocation<Object, String> get() {

        return new MappingInvocation<Object, String>(null) {

          public void onInput(final Object input, @NotNull final Channel<String, ?> result) {

            result.pass(input.toString());
          }
        };
      }
    });
  }

  @NotNull
  private static InvocationFactory<Object, String> createFunction() {

    return consumerCall(new BiConsumer<List<?>, Channel<String, ?>>() {

      public void accept(final List<?> objects, final Channel<String, ?> result) {

        for (final Object object : objects) {

          result.pass(object.toString());
        }
      }
    });
  }

  @NotNull
  private static InvocationFactory<Object, String> createFunction2() {

    return functionCall(new Function<List<?>, String>() {

      public String apply(final List<?> objects) {

        final StringBuilder builder = new StringBuilder();

        for (final Object object : objects) {

          builder.append(object.toString());
        }

        return builder.toString();
      }
    });
  }

  @NotNull
  private static MappingInvocation<Object, String> createMapping() {

    return consumerMapping(new BiConsumer<Object, Channel<String, ?>>() {

      public void accept(final Object o, final Channel<String, ?> result) {

        result.pass(o.toString());
      }
    });
  }

  @NotNull
  private static MappingInvocation<Object, String> createMapping2() {

    return functionMapping(new Function<Object, String>() {

      public String apply(final Object o) {

        return o.toString();
      }
    });
  }

  @NotNull
  private static MappingInvocation<String, String> createMapping3() {

    return predicateFilter(new Predicate<String>() {

      public boolean test(final String s) {

        return s.length() > 0;
      }
    });
  }

  @Test
  public void testAction() throws Exception {

    final TestAction action1 = new TestAction();
    final ActionDecorator action2 = decorate(action1);
    assertThat(decorate(action2)).isSameAs(action2);
    action2.perform();
    assertThat(action1.isCalled()).isTrue();
    action1.reset();
    final TestAction action3 = new TestAction();
    final ActionDecorator action4 = action2.andThen(action3);
    action4.perform();
    assertThat(action1.isCalled()).isTrue();
    assertThat(action3.isCalled()).isTrue();

    final TestRunnable runnable1 = new TestRunnable();
    final ActionDecorator action5 = decorate(runnable1);
    assertThat(decorate(action5)).isSameAs(action5);
    action5.perform();
    assertThat(runnable1.isCalled()).isTrue();
    runnable1.reset();
    final TestRunnable runnable3 = new TestRunnable();
    final ActionDecorator action6 = action5.andThen(runnable3);
    action6.perform();
    assertThat(runnable1.isCalled()).isTrue();
    assertThat(runnable3.isCalled()).isTrue();
  }

  @Test
  public void testActionContext() {

    assertThat(decorate(new TestAction()).hasStaticScope()).isTrue();
    assertThat(decorate(new Action() {

      public void perform() {

      }
    }).hasStaticScope()).isFalse();

    assertThat(decorate(new TestRunnable()).hasStaticScope()).isTrue();
    assertThat(decorate(new Runnable() {

      public void run() {

      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testActionEquals() {

    final TestAction action1 = new TestAction();
    assertThat(decorate(action1)).isEqualTo(decorate(action1));
    final ActionDecorator action2 = decorate(action1);
    assertThat(action2).isEqualTo(action2);
    assertThat(action2).isNotEqualTo(null);
    assertThat(action2).isNotEqualTo("test");
    assertThat(decorate(action1).andThen(action2).hashCode()).isEqualTo(
        action2.andThen(action2).hashCode());
    assertThat(decorate(action1).andThen(action2)).isEqualTo(action2.andThen(action2));
    assertThat(action2.andThen(action2)).isEqualTo(decorate(action1).andThen(action2));
    assertThat(decorate(action1).andThen(action2).hashCode()).isEqualTo(
        action2.andThen(action1).hashCode());
    assertThat(decorate(action1).andThen(action2)).isEqualTo(action2.andThen(action1));
    assertThat(action2.andThen(action1)).isEqualTo(decorate(action1).andThen(action2));
    assertThat(decorate(action1).andThen(action2).hashCode()).isNotEqualTo(
        action2.andThen(action2.andThen(action1)).hashCode());
    assertThat(decorate(action1).andThen(action2)).isNotEqualTo(
        action2.andThen(action2.andThen(action1)));
    assertThat(action2.andThen(action2.andThen(action1))).isNotEqualTo(
        decorate(action1).andThen(action2));
    assertThat(decorate(action1).andThen(action1).hashCode()).isNotEqualTo(
        action2.andThen(action2.andThen(action1)).hashCode());
    assertThat(decorate(action1).andThen(action1)).isNotEqualTo(
        action2.andThen(action2.andThen(action1)));
    assertThat(action2.andThen(action2.andThen(action1))).isNotEqualTo(
        decorate(action1).andThen(action1));
    assertThat(action2.andThen(action1).hashCode()).isNotEqualTo(
        action2.andThen(noOp()).hashCode());
    assertThat(action2.andThen(action1)).isNotEqualTo(action2.andThen(noOp()));
    assertThat(action2.andThen(noOp())).isNotEqualTo(action2.andThen(action1));

    final TestRunnable runnable1 = new TestRunnable();
    assertThat(decorate(runnable1)).isEqualTo(decorate(runnable1));
    final ActionDecorator action3 = decorate(runnable1);
    assertThat(action3).isEqualTo(action3);
    assertThat(action3).isNotEqualTo(null);
    assertThat(action3).isNotEqualTo("test");
    assertThat(decorate(runnable1).andThen(action3).hashCode()).isEqualTo(
        action3.andThen(action3).hashCode());
    assertThat(decorate(runnable1).andThen(action3)).isEqualTo(action3.andThen(action3));
    assertThat(action3.andThen(action3)).isEqualTo(decorate(runnable1).andThen(action3));
    assertThat(decorate(runnable1).andThen(action3).hashCode()).isEqualTo(
        action3.andThen(runnable1).hashCode());
    assertThat(decorate(runnable1).andThen(action3)).isEqualTo(action3.andThen(runnable1));
    assertThat(action3.andThen(runnable1)).isEqualTo(decorate(runnable1).andThen(action3));
    assertThat(decorate(runnable1).andThen(action3).hashCode()).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)).hashCode());
    assertThat(decorate(runnable1).andThen(action3)).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)));
    assertThat(action3.andThen(action3.andThen(runnable1))).isNotEqualTo(
        decorate(runnable1).andThen(action3));
    assertThat(decorate(runnable1).andThen(runnable1).hashCode()).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)).hashCode());
    assertThat(decorate(runnable1).andThen(runnable1)).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)));
    assertThat(action3.andThen(action3.andThen(runnable1))).isNotEqualTo(
        decorate(runnable1).andThen(runnable1));
    assertThat(action3.andThen(runnable1).hashCode()).isNotEqualTo(
        action3.andThen(noOp()).hashCode());
    assertThat(action3.andThen(runnable1)).isNotEqualTo(action3.andThen(noOp()));
    assertThat(action3.andThen(noOp())).isNotEqualTo(action3.andThen(runnable1));
  }

  @Test
  public void testBiConsumer() throws Exception {

    final TestBiConsumer consumer1 = new TestBiConsumer();
    final BiConsumerDecorator<Object, Object> consumer2 = decorate(consumer1);
    assertThat(decorate(consumer2)).isSameAs(consumer2);
    consumer2.accept("test", "test");
    assertThat(consumer1.isCalled()).isTrue();
    consumer1.reset();
    final TestBiConsumer consumer3 = new TestBiConsumer();
    final BiConsumerDecorator<Object, Object> consumer4 = consumer2.andThen(consumer3);
    consumer4.accept("test", "test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(consumer3.isCalled()).isTrue();
  }

  @Test
  public void testBiConsumerContext() {

    assertThat(decorate(new TestBiConsumer()).hasStaticScope()).isTrue();
    assertThat(decorate(new BiConsumer<Object, Object>() {

      public void accept(final Object o, final Object o2) {

      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testBiConsumerEquals() {

    final TestBiConsumer consumer1 = new TestBiConsumer();
    assertThat(decorate(consumer1)).isEqualTo(decorate(consumer1));
    final BiConsumerDecorator<Object, Object> consumer2 = decorate(consumer1);
    assertThat(consumer2).isEqualTo(consumer2);
    assertThat(consumer2).isNotEqualTo(null);
    assertThat(consumer2).isNotEqualTo("test");
    assertThat(decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer2).hashCode());
    assertThat(decorate(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer2));
    assertThat(consumer2.andThen(consumer2)).isEqualTo(decorate(consumer1).andThen(consumer2));
    assertThat(decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer1).hashCode());
    assertThat(decorate(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer1));
    assertThat(consumer2.andThen(consumer1)).isEqualTo(decorate(consumer1).andThen(consumer2));
    assertThat(decorate(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(decorate(consumer1).andThen(consumer2)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        decorate(consumer1).andThen(consumer2));
    assertThat(decorate(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(decorate(consumer1).andThen(consumer1)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        decorate(consumer1).andThen(consumer1));
    assertThat(consumer2.andThen(consumer1).hashCode()).isNotEqualTo(
        consumer2.andThen(biSink()).hashCode());
    assertThat(consumer2.andThen(consumer1)).isNotEqualTo(consumer2.andThen(biSink()));
    assertThat(consumer2.andThen(biSink())).isNotEqualTo(consumer2.andThen(consumer1));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testBiConsumerError() {

    try {

      BiConsumerDecorator.decorate(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate((BiConsumer<?, ?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate(new TestBiConsumer()).andThen(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testBiFunction() throws Exception {

    final TestBiFunction function1 = new TestBiFunction();
    final BiFunctionDecorator<Object, Object, Object> function2 = decorate(function1);
    assertThat(decorate(function2)).isSameAs(function2);
    assertThat(function2.apply("test", function1)).isSameAs(function1);
    assertThat(function1.isCalled()).isTrue();
    function1.reset();
    final TestFunction function = new TestFunction();
    final BiFunctionDecorator<Object, Object, Object> function3 = function2.andThen(function);
    assertThat(function3.apply("test", function1)).isSameAs(function1);
    assertThat(function1.isCalled()).isTrue();
    assertThat(function.isCalled()).isTrue();
    assertThat(Functions.<String, String>first().andThen(new Function<String, Integer>() {

      public Integer apply(final String s) {

        return s.length();
      }
    }).andThen(new Function<Integer, Integer>() {

      public Integer apply(final Integer integer) {

        return integer * 3;
      }
    }).apply("test", "long test")).isEqualTo(12);
    assertThat(Functions.<String, Integer>second().andThen(new Function<Integer, Integer>() {

      public Integer apply(final Integer integer) {

        return integer + 2;
      }
    }).apply("test", 3)).isEqualTo(5);
  }

  @Test
  public void testBiFunctionContext() {

    assertThat(decorate(new TestBiFunction()).hasStaticScope()).isTrue();
    assertThat(decorate(new BiFunction<Object, Object, Object>() {

      public Object apply(final Object o, final Object o2) {

        return null;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testBiFunctionEquals() {

    final TestBiFunction function1 = new TestBiFunction();
    assertThat(decorate(function1)).isEqualTo(decorate(function1));
    final BiFunctionDecorator<Object, Object, Object> function2 = decorate(function1);
    assertThat(function2).isEqualTo(function2);
    assertThat(function2).isNotEqualTo(null);
    assertThat(function2).isNotEqualTo("test");
    final TestFunction function = new TestFunction();
    assertThat(decorate(function1).andThen(function).hashCode()).isEqualTo(
        function2.andThen(function).hashCode());
    assertThat(decorate(function1).andThen(function)).isEqualTo(function2.andThen(function));
    assertThat(function2.andThen(function)).isEqualTo(decorate(function1).andThen(function));
    assertThat(decorate(function1).andThen(decorate(function)).hashCode()).isEqualTo(
        function2.andThen(function).hashCode());
    assertThat(decorate(function1).andThen(decorate(function))).isEqualTo(
        function2.andThen(function));
    assertThat(function2.andThen(function)).isEqualTo(
        decorate(function1).andThen(decorate(function)));
    assertThat(decorate(function1).andThen(decorate(function)).hashCode()).isNotEqualTo(
        function2.andThen(decorate(function).andThen(function)).hashCode());
    assertThat(decorate(function1).andThen(decorate(function))).isNotEqualTo(
        function2.andThen(decorate(function).andThen(function)));
    assertThat(function2.andThen(decorate(function).andThen(function))).isNotEqualTo(
        decorate(function1).andThen(decorate(function)));
    assertThat(decorate(function1).andThen(function).hashCode()).isNotEqualTo(
        function2.andThen(decorate(function).andThen(function)).hashCode());
    assertThat(decorate(function1).andThen(function)).isNotEqualTo(
        function2.andThen(decorate(function).andThen(function)));
    assertThat(function2.andThen(decorate(function).andThen(function))).isNotEqualTo(
        decorate(function1).andThen(function));
    assertThat(function2.andThen(function).hashCode()).isNotEqualTo(
        function2.andThen(identity()).hashCode());
    assertThat(function2.andThen(function)).isNotEqualTo(function2.andThen(identity()));
    assertThat(function2.andThen(identity())).isNotEqualTo(function2.andThen(function));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testBiFunctionError() {

    try {

      BiFunctionDecorator.decorate(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate((BiFunction<?, ?, ?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate(new TestBiFunction()).andThen(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testBiSink() throws Exception {

    final TestBiConsumer consumer1 = new TestBiConsumer();
    final BiConsumerDecorator<Object, Object> consumer2 = biSink().andThen(consumer1);
    consumer2.accept("test", "test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(biSink()).isSameAs(biSink());
  }

  @Test
  public void testCastFunction() throws Exception {

    final FunctionDecorator<Object, Number> function = castTo(Number.class);
    function.apply(1);
    function.apply(3.5);

    try {

      function.apply("test");

      fail();

    } catch (final ClassCastException ignored) {

    }

    final FunctionDecorator<Object, List<String>> function1 =
        castTo(new ClassToken<List<String>>() {});
    function1.apply(new ArrayList<String>());
    function1.apply(new CopyOnWriteArrayList<String>());

    try {

      fail(function1.apply(Arrays.asList(1, 2)).get(0));

    } catch (final ClassCastException ignored) {

    }

    try {

      function1.apply("test");

      fail();

    } catch (final ClassCastException ignored) {

    }
  }

  @Test
  public void testCastFunctionEquals() {

    final FunctionDecorator<Object, Number> function = castTo(Number.class);
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(castTo(Number.class));
    assertThat(function).isNotEqualTo(castTo(String.class));
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(castTo(Number.class).hashCode());
    final FunctionDecorator<Object, List<String>> function1 =
        castTo(new ClassToken<List<String>>() {});
    assertThat(function1).isEqualTo(function1);
    assertThat(function1).isEqualTo(castTo(new ClassToken<List<String>>() {}));
    assertThat(function1).isNotEqualTo(castTo(new ClassToken<String>() {}));
    assertThat(function1).isNotEqualTo("");
    assertThat(function1.hashCode()).isEqualTo(
        castTo(new ClassToken<List<String>>() {}).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCastFunctionError() {

    try {

      castTo((Class<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      castTo((ClassToken<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testCommand() {

    final Routine<Void, String> routine = JRoutineCore.with(createCommand()).buildRoutine();
    assertThat(routine.close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testCommand2() {

    final Routine<Void, String> routine = JRoutineCore.with(createCommand2()).buildRoutine();
    assertThat(routine.close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testCommand2Equals() {

    final InvocationFactory<Void, String> factory = createCommand2();
    final SupplierDecorator<String> constant = constant("test");
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createCommand2());
    assertThat(factory).isNotEqualTo(supplierCommand(constant));
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo("");
    assertThat(supplierCommand(constant)).isEqualTo(supplierCommand(constant));
    assertThat(supplierCommand(constant).hashCode()).isEqualTo(
        supplierCommand(constant).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCommand2Error() {

    try {

      supplierCommand(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testCommandEquals() {

    final InvocationFactory<Void, String> factory = createCommand();
    final ConsumerDecorator<Channel<String, ?>> sink = sink();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createCommand());
    assertThat(factory).isNotEqualTo(consumerCommand(sink));
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo("");
    assertThat(consumerCommand(sink)).isEqualTo(consumerCommand(sink));
    assertThat(consumerCommand(sink).hashCode()).isEqualTo(consumerCommand(sink).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCommandError() {

    try {

      consumerCommand(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testConstantSupplier() throws Exception {

    final TestFunction function = new TestFunction();
    final SupplierDecorator<Object> supplier = constant("test").andThen(function);
    assertThat(supplier.get()).isEqualTo("test");
    assertThat(function.isCalled()).isTrue();
  }

  @Test
  public void testConstantSupplierEquals() {

    final SupplierDecorator<String> supplier = constant("test");
    assertThat(supplier).isEqualTo(supplier);
    assertThat(supplier).isEqualTo(constant("test"));
    assertThat(supplier).isNotEqualTo(constant(1));
    assertThat(supplier).isNotEqualTo("");
    assertThat(supplier.hashCode()).isEqualTo(constant("test").hashCode());
  }

  @Test
  public void testConstructor() {

    boolean failed = false;
    try {
      new Functions();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testConsumer() throws Exception {

    final TestConsumer consumer1 = new TestConsumer();
    final ConsumerDecorator<Object> consumer2 = decorate(consumer1);
    assertThat(decorate(consumer2)).isSameAs(consumer2);
    consumer2.accept("test");
    assertThat(consumer1.isCalled()).isTrue();
    consumer1.reset();
    final TestConsumer consumer3 = new TestConsumer();
    final ConsumerDecorator<Object> consumer4 = consumer2.andThen(consumer3);
    consumer4.accept("test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(consumer3.isCalled()).isTrue();
  }

  @Test
  public void testConsumerContext() {

    assertThat(decorate(new TestConsumer()).hasStaticScope()).isTrue();
    assertThat(decorate(new Consumer<Object>() {

      public void accept(final Object o) {

      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testConsumerEquals() {

    final TestConsumer consumer1 = new TestConsumer();
    assertThat(decorate(consumer1)).isEqualTo(decorate(consumer1));
    final ConsumerDecorator<Object> consumer2 = decorate(consumer1);
    assertThat(consumer2).isEqualTo(consumer2);
    assertThat(consumer2).isNotEqualTo(null);
    assertThat(consumer2).isNotEqualTo("test");
    assertThat(decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer2).hashCode());
    assertThat(decorate(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer2));
    assertThat(consumer2.andThen(consumer2)).isEqualTo(decorate(consumer1).andThen(consumer2));
    assertThat(decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer1).hashCode());
    assertThat(decorate(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer1));
    assertThat(consumer2.andThen(consumer1)).isEqualTo(decorate(consumer1).andThen(consumer2));
    assertThat(decorate(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(decorate(consumer1).andThen(consumer2)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        decorate(consumer1).andThen(consumer2));
    assertThat(decorate(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(decorate(consumer1).andThen(consumer1)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        decorate(consumer1).andThen(consumer1));
    assertThat(consumer2.andThen(consumer1).hashCode()).isNotEqualTo(
        consumer2.andThen(sink()).hashCode());
    assertThat(consumer2.andThen(consumer1)).isNotEqualTo(consumer2.andThen(sink()));
    assertThat(consumer2.andThen(sink())).isNotEqualTo(consumer2.andThen(consumer1));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testConsumerError() {

    try {

      ConsumerDecorator.decorate(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate((Consumer<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate(new TestConsumer()).andThen(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testEqualToPredicate() throws Exception {

    final PredicateDecorator<Object> predicate = isEqualTo("test");
    assertThat(predicate.test("test")).isTrue();
    assertThat(predicate.test(1)).isFalse();
    assertThat(predicate.test(null)).isFalse();
    final PredicateDecorator<Object> predicate1 = isEqualTo(null);
    assertThat(predicate1.test("test")).isFalse();
    assertThat(predicate1.test(1)).isFalse();
    assertThat(predicate1.test(null)).isTrue();
  }

  @Test
  public void testEqualToPredicateEquals() {

    final PredicateDecorator<Object> predicate = isEqualTo("test");
    assertThat(predicate).isEqualTo(predicate);
    assertThat(predicate).isEqualTo(isEqualTo("test"));
    assertThat(predicate).isNotEqualTo(isEqualTo(1.1));
    assertThat(predicate).isNotEqualTo("");
    assertThat(predicate.hashCode()).isEqualTo(isEqualTo("test").hashCode());
    assertThat(isEqualTo(null)).isEqualTo(isNull());
  }

  @Test
  public void testFactory() {

    final Routine<Object, String> routine = JRoutineCore.with(createFactory()).buildRoutine();
    assertThat(routine.call("test", 1).in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testFactoryEquals() {

    final Supplier<Invocation<Object, Object>> supplier =
        constant(IdentityInvocation.factoryOf().newInvocation());
    final InvocationFactory<Object, String> factory = createFactory();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo(supplierFactory(supplier));
    assertThat(factory).isNotEqualTo(createMapping());
    assertThat(factory).isNotEqualTo("");
    assertThat(supplierFactory(supplier)).isEqualTo(supplierFactory(supplier));
    assertThat(supplierFactory(supplier).hashCode()).isEqualTo(
        supplierFactory(supplier).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testFactoryError() {

    try {

      supplierFactory(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testFunction() throws Exception {

    final TestFunction function1 = new TestFunction();
    final FunctionDecorator<Object, Object> function2 = decorate(function1);
    assertThat(decorate(function2)).isSameAs(function2);
    assertThat(function2.apply("test")).isEqualTo("test");
    assertThat(function1.isCalled()).isTrue();
    function1.reset();
    final TestFunction function3 = new TestFunction();
    final FunctionDecorator<Object, Object> function4 = function2.andThen(function3);
    assertThat(function4.apply("test")).isEqualTo("test");
    assertThat(function1.isCalled()).isTrue();
    assertThat(function3.isCalled()).isTrue();
    final FunctionDecorator<String, Integer> function5 = decorate(new Function<String, Integer>() {

      public Integer apply(final String s) {

        return s.length();
      }
    }).andThen(new Function<Integer, Integer>() {

      public Integer apply(final Integer integer) {

        return integer * 3;
      }
    });
    assertThat(function5.apply("test")).isEqualTo(12);
    assertThat(function5.compose(new Function<String, String>() {

      public String apply(final String s) {

        return s + s;
      }
    }).apply("test")).isEqualTo(24);
  }

  @Test
  public void testFunctionContext() {

    assertThat(decorate(new TestFunction()).hasStaticScope()).isTrue();
    assertThat(decorate(new Function<Object, Object>() {

      public Object apply(final Object o) {

        return null;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testFunctionEquals() {

    final TestFunction function1 = new TestFunction();
    assertThat(decorate(function1)).isEqualTo(decorate(function1));
    final FunctionDecorator<Object, Object> function2 = decorate(function1);
    assertThat(function2).isEqualTo(function2);
    assertThat(function2).isNotEqualTo(null);
    assertThat(function2).isNotEqualTo("test");
    assertThat(decorate(function1).andThen(function2).hashCode()).isEqualTo(
        function2.andThen(function2).hashCode());
    assertThat(decorate(function1).andThen(function2)).isEqualTo(function2.andThen(function2));
    assertThat(function2.andThen(function2)).isEqualTo(decorate(function1).andThen(function2));
    assertThat(decorate(function1).andThen(function2).hashCode()).isEqualTo(
        function2.andThen(function1).hashCode());
    assertThat(decorate(function1).andThen(function2)).isEqualTo(function2.andThen(function1));
    assertThat(function2.andThen(function1)).isEqualTo(decorate(function1).andThen(function2));
    assertThat(decorate(function1).andThen(function2).hashCode()).isNotEqualTo(
        function2.andThen(function2.andThen(function1)).hashCode());
    assertThat(decorate(function1).andThen(function2)).isNotEqualTo(
        function2.andThen(function2.andThen(function1)));
    assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
        decorate(function1).andThen(function2));
    assertThat(decorate(function1).andThen(function1).hashCode()).isNotEqualTo(
        function2.andThen(function2.andThen(function1)).hashCode());
    assertThat(decorate(function1).andThen(function1)).isNotEqualTo(
        function2.andThen(function2.andThen(function1)));
    assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
        decorate(function1).andThen(function1));
    assertThat(function2.andThen(function1).hashCode()).isNotEqualTo(
        function2.andThen(identity()).hashCode());
    assertThat(function2.andThen(function1)).isNotEqualTo(function2.andThen(identity()));
    assertThat(function2.andThen(identity())).isNotEqualTo(function2.andThen(function1));
    assertThat(decorate(function1).compose(function2).hashCode()).isEqualTo(
        function2.compose(function2).hashCode());
    assertThat(decorate(function1).compose(function2)).isEqualTo(function2.compose(function2));
    assertThat(function2.compose(function2)).isEqualTo(decorate(function1).compose(function2));
    assertThat(decorate(function1).compose(function2).hashCode()).isEqualTo(
        function2.compose(function1).hashCode());
    assertThat(decorate(function1).compose(function2)).isEqualTo(function2.compose(function1));
    assertThat(function2.compose(function1)).isEqualTo(decorate(function1).compose(function2));
    assertThat(decorate(function1).compose(function2).hashCode()).isNotEqualTo(
        function2.compose(function2.compose(function1)).hashCode());
    assertThat(decorate(function1).compose(function2)).isNotEqualTo(
        function2.compose(function2.compose(function1)));
    assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
        decorate(function1).compose(function2));
    assertThat(decorate(function1).compose(function1).hashCode()).isNotEqualTo(
        function2.compose(function2.compose(function1)).hashCode());
    assertThat(decorate(function1).compose(function1)).isNotEqualTo(
        function2.compose(function2.compose(function1)));
    assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
        decorate(function1).compose(function1));
    assertThat(function2.compose(function1).hashCode()).isNotEqualTo(
        function2.compose(identity()).hashCode());
    assertThat(function2.compose(function1)).isNotEqualTo(function2.compose(identity()));
    assertThat(function2.compose(identity())).isNotEqualTo(function2.compose(function1));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testFunctionError() {

    try {

      FunctionDecorator.decorate(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate((Function<?, ?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate(new TestFunction()).andThen(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      identity().compose(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testFunctionFactory() {

    final Routine<Object, String> routine = JRoutineCore.with(createFunction()).buildRoutine();
    assertThat(routine.call("test", 1).in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testFunctionFactory2() {

    final Routine<Object, String> routine = JRoutineCore.with(createFunction2()).buildRoutine();
    assertThat(routine.call("test", 1).in(seconds(1)).all()).containsOnly("test1");
  }

  @Test
  public void testFunctionFactory2Equals() {

    final InvocationFactory<?, String> factory = createFunction2();
    final FunctionDecorator<List<?>, ? super List<?>> identity = identity();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createFunction2());
    assertThat(factory).isNotEqualTo(functionCall(identity));
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo("");
    assertThat(functionCall(identity)).isEqualTo(functionCall(identity));
    assertThat(functionCall(identity).hashCode()).isEqualTo(functionCall(identity).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testFunctionFactory2Error() {

    try {

      functionCall(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testFunctionFactoryEquals() {

    final InvocationFactory<?, String> factory = createFunction();
    final BiConsumerDecorator<List<?>, Channel<Object, ?>> sink = biSink();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createFunction());
    assertThat(factory).isNotEqualTo(consumerCall(sink));
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo("");
    assertThat(consumerCall(sink)).isEqualTo(consumerCall(sink));
    assertThat(consumerCall(sink).hashCode()).isEqualTo(consumerCall(sink).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testFunctionFactoryError() {

    try {

      consumerCall(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testIdentity() throws Exception {

    final TestFunction function1 = new TestFunction();
    final FunctionDecorator<Object, Object> function2 = identity().andThen(function1);
    assertThat(function2.apply("test")).isEqualTo("test");
    assertThat(function1.isCalled()).isTrue();
    assertThat(identity()).isSameAs(identity());
  }

  @Test
  public void testInstanceOfPredicate() throws Exception {

    final PredicateDecorator<Object> predicate = isInstanceOf(String.class);
    assertThat(predicate.test("test")).isTrue();
    assertThat(predicate.test(1)).isFalse();
    assertThat(predicate.test(null)).isFalse();
  }

  @Test
  public void testInstanceOfPredicateEquals() {

    final PredicateDecorator<Object> predicate = isInstanceOf(String.class);
    assertThat(predicate).isEqualTo(predicate);
    assertThat(predicate).isEqualTo(isInstanceOf(String.class));
    assertThat(predicate).isNotEqualTo(isInstanceOf(Integer.class));
    assertThat(predicate).isNotEqualTo("");
    assertThat(predicate.hashCode()).isEqualTo(isInstanceOf(String.class).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testInstanceOfPredicateError() {

    try {

      isInstanceOf(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testMapping() {

    final Routine<Object, String> routine = JRoutineCore.with(createMapping()).buildRoutine();
    assertThat(routine.call("test", 1).in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testMapping2() {

    final Routine<Object, String> routine = JRoutineCore.with(createMapping2()).buildRoutine();
    assertThat(routine.call("test", 1).in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testMapping2Equals() {

    final FunctionDecorator<Object, ? super Object> identity = identity();
    final InvocationFactory<Object, String> factory = createMapping2();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createMapping2());
    assertThat(factory).isNotEqualTo(functionMapping(identity));
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo("");
    assertThat(functionMapping(identity)).isEqualTo(functionMapping(identity));
    assertThat(functionMapping(identity).hashCode()).isEqualTo(
        functionMapping(identity).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapping2Error() {

    try {

      functionMapping((Function<Object, Channel<Object, ?>>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testMapping3() {

    final Routine<String, String> routine = JRoutineCore.with(createMapping3()).buildRoutine();
    assertThat(routine.call("test", "").in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testMapping3Equals() {

    final PredicateDecorator<Object> negative = negative();
    final InvocationFactory<String, String> factory = createMapping3();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createMapping3());
    assertThat(factory).isNotEqualTo(predicateFilter(negative));
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo("");
    assertThat(predicateFilter(negative)).isEqualTo(predicateFilter(negative));
    assertThat(predicateFilter(negative).hashCode()).isEqualTo(
        predicateFilter(negative).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMapping3Error() {

    try {

      predicateFilter(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testMappingEquals() {

    final InvocationFactory<Object, String> factory = createMapping();
    final BiConsumerDecorator<Object, Channel<String, ?>> sink = biSink();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createMapping());
    assertThat(factory).isNotEqualTo(consumerMapping(sink));
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo("");
    assertThat(consumerMapping(sink)).isEqualTo(consumerMapping(sink));
    assertThat(consumerMapping(sink).hashCode()).isEqualTo(consumerMapping(sink).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMappingError() {

    try {

      consumerMapping(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testMaxBiFunction() throws Exception {

    final BiFunctionDecorator<String, String, String> function = max();
    assertThat(function.apply("A TEST", "test")).isEqualTo("test");
    assertThat(function.andThen(new Function<String, String>() {

      public String apply(final String s) {

        return s.toLowerCase();
      }
    }).apply("A TEST", "test")).isEqualTo("test");
    assertThat(function.apply("2", "1")).isEqualTo("2");
  }

  @Test
  public void testMaxBiFunctionEquals() {

    final BiFunctionDecorator<String, String, String> function = max();
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(max());
    assertThat(function).isNotEqualTo(maxBy(new Comparator<String>() {

      public int compare(final String o1, final String o2) {

        return o2.compareTo(o1);
      }
    }));
    assertThat(function).isNotEqualTo(null);
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(max().hashCode());
  }

  @Test
  public void testMaxByBiFunction() throws Exception {

    final BiFunctionDecorator<String, String, String> function =
        maxBy(String.CASE_INSENSITIVE_ORDER);
    assertThat(function.apply("TEST", "a test")).isEqualTo("TEST");
    assertThat(function.andThen(new Function<String, String>() {

      public String apply(final String s) {

        return s.toLowerCase();
      }
    }).apply("TEST", "a test")).isEqualTo("test");
    assertThat(function.apply("2", "1")).isEqualTo("2");
  }

  @Test
  public void testMaxByBiFunctionEquals() {

    final BiFunctionDecorator<String, String, String> function =
        maxBy(String.CASE_INSENSITIVE_ORDER);
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(maxBy(String.CASE_INSENSITIVE_ORDER));
    assertThat(function).isNotEqualTo(maxBy(new Comparator<String>() {

      public int compare(final String o1, final String o2) {

        return o2.compareTo(o1);
      }
    }));
    assertThat(function).isNotEqualTo(null);
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(maxBy(String.CASE_INSENSITIVE_ORDER).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMaxByBiFunctionError() {

    try {

      maxBy(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testMinBiFunction() throws Exception {

    final BiFunctionDecorator<String, String, String> function = min();
    assertThat(function.apply("A TEST", "test")).isEqualTo("A TEST");
    assertThat(function.andThen(new Function<String, String>() {

      public String apply(final String s) {

        return s.toLowerCase();
      }
    }).apply("A TEST", "test")).isEqualTo("a test");
    assertThat(function.apply("2", "1")).isEqualTo("1");
  }

  @Test
  public void testMinBiFunctionEquals() {

    final BiFunctionDecorator<String, String, String> function = min();
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(min());
    assertThat(function).isNotEqualTo(minBy(new Comparator<String>() {

      public int compare(final String o1, final String o2) {

        return o2.compareTo(o1);
      }
    }));
    assertThat(function).isNotEqualTo(null);
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(min().hashCode());
  }

  @Test
  public void testMinByBiFunction() throws Exception {

    final BiFunctionDecorator<String, String, String> function =
        minBy(String.CASE_INSENSITIVE_ORDER);
    assertThat(function.apply("TEST", "a test")).isEqualTo("a test");
    assertThat(function.andThen(new Function<String, String>() {

      public String apply(final String s) {

        return s.toUpperCase();
      }
    }).apply("TEST", "a test")).isEqualTo("A TEST");
    assertThat(function.apply("2", "1")).isEqualTo("1");
  }

  @Test
  public void testMinByBiFunctionEquals() {

    final BiFunctionDecorator<String, String, String> function =
        minBy(String.CASE_INSENSITIVE_ORDER);
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(minBy(String.CASE_INSENSITIVE_ORDER));
    assertThat(function).isNotEqualTo(minBy(new Comparator<String>() {

      public int compare(final String o1, final String o2) {

        return o1.compareTo(o2);
      }
    }));
    assertThat(function).isNotEqualTo(null);
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(minBy(String.CASE_INSENSITIVE_ORDER).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMinByBiFunctionError() {

    try {

      minBy(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testPredicate() throws Exception {

    final TestPredicate predicate1 = new TestPredicate();
    final PredicateDecorator<Object> predicate2 = decorate(predicate1);
    assertThat(decorate(predicate2)).isSameAs(predicate2);
    assertThat(predicate2.test(this)).isTrue();
    assertThat(predicate1.isCalled()).isTrue();
    predicate1.reset();
    final TestPredicate predicate3 = new TestPredicate();
    final PredicateDecorator<Object> predicate4 = predicate2.and(predicate3);
    assertThat(predicate4.test(this)).isTrue();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isTrue();
    predicate1.reset();
    predicate3.reset();
    assertThat(predicate4.test(null)).isFalse();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isFalse();
    predicate1.reset();
    predicate3.reset();
    final PredicateDecorator<Object> predicate5 = predicate2.or(predicate3);
    assertThat(predicate5.test(this)).isTrue();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isFalse();
    predicate1.reset();
    predicate3.reset();
    assertThat(predicate5.test(null)).isFalse();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isTrue();
    predicate1.reset();
    predicate3.reset();
    final PredicateDecorator<Object> predicate6 = predicate4.negate();
    assertThat(predicate6.test(this)).isFalse();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isTrue();
    predicate1.reset();
    predicate3.reset();
    assertThat(predicate6.test(null)).isTrue();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isFalse();
    predicate1.reset();
    predicate3.reset();
    final PredicateDecorator<Object> predicate7 = predicate5.negate();
    assertThat(predicate7.test(this)).isFalse();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isFalse();
    predicate1.reset();
    predicate3.reset();
    assertThat(predicate7.test(null)).isTrue();
    assertThat(predicate1.isCalled()).isTrue();
    assertThat(predicate3.isCalled()).isTrue();
    predicate1.reset();
    predicate3.reset();
    assertThat(negative().or(positive()).test(null)).isTrue();
    assertThat(negative().and(positive()).test("test")).isFalse();
    assertThat(isNotNull().or(isNull()).test(null)).isTrue();
    assertThat(isNotNull().and(isNull()).test("test")).isFalse();
  }

  @Test
  public void testPredicateContext() {

    assertThat(decorate(new TestPredicate()).hasStaticScope()).isTrue();
    assertThat(decorate(new Predicate<Object>() {

      public boolean test(final Object o) {

        return false;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testPredicateEquals() {

    final TestPredicate predicate1 = new TestPredicate();
    assertThat(decorate(predicate1)).isEqualTo(decorate(predicate1));
    final PredicateDecorator<Object> predicate2 = decorate(predicate1);
    assertThat(predicate2).isEqualTo(predicate2);
    assertThat(predicate2).isNotEqualTo(null);
    assertThat(predicate2).isNotEqualTo("test");
    assertThat(decorate(predicate1).and(predicate2).hashCode()).isEqualTo(
        predicate2.and(predicate2).hashCode());
    assertThat(decorate(predicate1).and(predicate2)).isEqualTo(predicate2.and(predicate2));
    assertThat(predicate2.and(predicate2)).isEqualTo(decorate(predicate1).and(predicate2));
    assertThat(decorate(predicate1).and(predicate2).hashCode()).isEqualTo(
        predicate2.and(predicate1).hashCode());
    assertThat(decorate(predicate1).and(predicate2)).isEqualTo(predicate2.and(predicate1));
    assertThat(predicate2.and(predicate1)).isEqualTo(decorate(predicate1).and(predicate2));
    assertThat(decorate(predicate1).and(predicate2).hashCode()).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)).hashCode());
    assertThat(decorate(predicate1).and(predicate2)).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)));
    assertThat(predicate2.and(predicate2.and(predicate1))).isNotEqualTo(
        decorate(predicate1).and(predicate2));
    assertThat(decorate(predicate1).and(predicate1).hashCode()).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)).hashCode());
    assertThat(decorate(predicate1).and(predicate1)).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)));
    assertThat(predicate2.and(predicate2.and(predicate1))).isNotEqualTo(
        decorate(predicate1).and(predicate1));
    assertThat(predicate2.and(predicate1).hashCode()).isNotEqualTo(
        predicate2.and(positive()).hashCode());
    assertThat(predicate2.and(predicate1)).isNotEqualTo(predicate2.and(positive()));
    assertThat(predicate2.and(positive())).isNotEqualTo(predicate2.and(predicate1));
    assertThat(decorate(predicate1).or(predicate2).hashCode()).isEqualTo(
        predicate2.or(predicate2).hashCode());
    assertThat(decorate(predicate1).or(predicate2)).isEqualTo(predicate2.or(predicate2));
    assertThat(predicate2.or(predicate2)).isEqualTo(decorate(predicate1).or(predicate2));
    assertThat(decorate(predicate1).or(predicate2).hashCode()).isEqualTo(
        predicate2.or(predicate1).hashCode());
    assertThat(decorate(predicate1).or(predicate2)).isEqualTo(predicate2.or(predicate1));
    assertThat(predicate2.or(predicate1)).isEqualTo(decorate(predicate1).or(predicate2));
    assertThat(decorate(predicate1).or(predicate2).hashCode()).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)).hashCode());
    assertThat(decorate(predicate1).or(predicate2)).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)));
    assertThat(predicate2.or(predicate2.or(predicate1))).isNotEqualTo(
        decorate(predicate1).or(predicate2));
    assertThat(decorate(predicate1).or(predicate1).hashCode()).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)).hashCode());
    assertThat(decorate(predicate1).or(predicate1)).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)));
    assertThat(predicate2.or(predicate2.or(predicate1))).isNotEqualTo(
        decorate(predicate1).or(predicate1));
    assertThat(predicate2.or(predicate1).hashCode()).isNotEqualTo(
        predicate2.or(positive()).hashCode());
    assertThat(predicate2.or(predicate1)).isNotEqualTo(predicate2.or(positive()));
    assertThat(predicate2.or(positive())).isNotEqualTo(predicate2.or(predicate1));
    assertThat(predicate2.negate().negate()).isEqualTo(decorate(predicate1));
    assertThat(predicate2.and(predicate1).negate()).isEqualTo(
        predicate2.negate().or(decorate(predicate1).negate()));
    assertThat(predicate2.and(predicate1).negate().hashCode()).isEqualTo(
        predicate2.negate().or(decorate(predicate1).negate()).hashCode());
    final PredicateDecorator<Object> chain =
        predicate2.negate().or(predicate2.negate().and(predicate2.negate()));
    assertThat(predicate2.and(predicate2.or(predicate1)).negate()).isEqualTo(chain);
    assertThat(negative().negate()).isEqualTo(positive());
    assertThat(positive().negate()).isEqualTo(negative());
    assertThat(isNotNull().negate()).isEqualTo(isNull());
    assertThat(isNull().negate()).isEqualTo(isNotNull());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testPredicateError() {

    try {

      PredicateDecorator.decorate(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate((Predicate<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate(new TestPredicate()).and(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate(new TestPredicate()).or(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      negative().and(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      positive().or(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testSameAsPredicate() throws Exception {

    final Identity instance = new Identity();
    final PredicateDecorator<Object> predicate = isSameAs(instance);
    assertThat(predicate.test(instance)).isTrue();
    assertThat(predicate.test(new Identity())).isFalse();
    assertThat(predicate.test(1)).isFalse();
    assertThat(predicate.test(null)).isFalse();
    final PredicateDecorator<Object> predicate1 = isSameAs(null);
    assertThat(predicate1.test(instance)).isFalse();
    assertThat(predicate1.test(1)).isFalse();
    assertThat(predicate1.test(null)).isTrue();
  }

  @Test
  public void testSameAsPredicateEquals() {

    final Identity instance = new Identity();
    final PredicateDecorator<Object> predicate = isSameAs(instance);
    assertThat(predicate).isEqualTo(predicate);
    assertThat(predicate).isEqualTo(isSameAs(instance));
    assertThat(predicate).isNotEqualTo(isSameAs(new Identity()));
    assertThat(predicate).isNotEqualTo(isSameAs(1.1));
    assertThat(predicate).isNotEqualTo("");
    assertThat(predicate.hashCode()).isEqualTo(isSameAs(instance).hashCode());
    assertThat(isSameAs(null)).isEqualTo(isNull());
  }

  @Test
  public void testSink() throws Exception {

    final TestConsumer consumer1 = new TestConsumer();
    final ConsumerDecorator<Object> consumer2 = sink().andThen(consumer1);
    consumer2.accept("test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(sink()).isSameAs(sink());
  }

  @Test
  public void testSupplier() throws Exception {

    final TestSupplier supplier1 = new TestSupplier();
    final SupplierDecorator<Object> supplier2 = decorate(supplier1);
    assertThat(decorate(supplier2)).isSameAs(supplier2);
    assertThat(supplier2.get()).isSameAs(supplier1);
    assertThat(supplier1.isCalled()).isTrue();
    supplier1.reset();
    final TestFunction function = new TestFunction();
    final SupplierDecorator<Object> supplier3 = supplier2.andThen(function);
    assertThat(supplier3.get()).isSameAs(supplier1);
    assertThat(supplier1.isCalled()).isTrue();
    assertThat(function.isCalled()).isTrue();
    assertThat(constant("test").andThen(new Function<String, Integer>() {

      public Integer apply(final String s) {

        return s.length();
      }
    }).andThen(new Function<Integer, Integer>() {

      public Integer apply(final Integer integer) {

        return integer * 3;
      }
    }).get()).isEqualTo(12);
  }

  @Test
  public void testSupplierContext() {

    assertThat(decorate(new TestSupplier()).hasStaticScope()).isTrue();
    assertThat(decorate(new Supplier<Object>() {

      public Object get() {

        return null;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testSupplierEquals() {

    final TestSupplier supplier1 = new TestSupplier();
    assertThat(decorate(supplier1)).isEqualTo(decorate(supplier1));
    final SupplierDecorator<Object> supplier2 = decorate(supplier1);
    assertThat(supplier2).isEqualTo(supplier2);
    assertThat(supplier2).isNotEqualTo(null);
    assertThat(supplier2).isNotEqualTo("test");
    final TestFunction function = new TestFunction();
    assertThat(decorate(supplier1).andThen(function).hashCode()).isEqualTo(
        supplier2.andThen(function).hashCode());
    assertThat(decorate(supplier1).andThen(function)).isEqualTo(supplier2.andThen(function));
    assertThat(supplier2.andThen(function)).isEqualTo(decorate(supplier1).andThen(function));
    assertThat(decorate(supplier1).andThen(decorate(function)).hashCode()).isEqualTo(
        supplier2.andThen(function).hashCode());
    assertThat(decorate(supplier1).andThen(decorate(function))).isEqualTo(
        supplier2.andThen(function));
    assertThat(supplier2.andThen(function)).isEqualTo(
        decorate(supplier1).andThen(decorate(function)));
    assertThat(decorate(supplier1).andThen(decorate(function)).hashCode()).isNotEqualTo(
        supplier2.andThen(decorate(function).andThen(function)).hashCode());
    assertThat(decorate(supplier1).andThen(decorate(function))).isNotEqualTo(
        supplier2.andThen(decorate(function).andThen(function)));
    assertThat(supplier2.andThen(decorate(function).andThen(function))).isNotEqualTo(
        decorate(supplier1).andThen(decorate(function)));
    assertThat(decorate(supplier1).andThen(function).hashCode()).isNotEqualTo(
        supplier2.andThen(decorate(function).andThen(function)).hashCode());
    assertThat(decorate(supplier1).andThen(function)).isNotEqualTo(
        supplier2.andThen(decorate(function).andThen(function)));
    assertThat(supplier2.andThen(decorate(function).andThen(function))).isNotEqualTo(
        decorate(supplier1).andThen(function));
    assertThat(supplier2.andThen(function).hashCode()).isNotEqualTo(
        supplier2.andThen(identity()).hashCode());
    assertThat(supplier2.andThen(function)).isNotEqualTo(supplier2.andThen(identity()));
    assertThat(supplier2.andThen(identity())).isNotEqualTo(supplier2.andThen(function));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testSupplierError() {

    try {

      SupplierDecorator.decorate(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate((Supplier<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      decorate(new TestSupplier()).andThen(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  private static class Identity {

    @Override
    public boolean equals(final Object obj) {

      return (obj != null) && (obj.getClass() == Identity.class);
    }
  }

  private static class TestAction implements Action {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public void perform() {

      mIsCalled = true;
    }

  }

  private static class TestBiConsumer implements BiConsumer<Object, Object> {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public void accept(final Object in1, final Object in2) {

      mIsCalled = true;
    }
  }

  private static class TestBiFunction implements BiFunction<Object, Object, Object> {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public Object apply(final Object in1, final Object in2) {

      mIsCalled = true;
      return in2;
    }
  }

  private static class TestConsumer implements Consumer<Object> {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public void accept(final Object out) {

      mIsCalled = true;
    }
  }

  private static class TestFunction implements Function<Object, Object> {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public Object apply(final Object in) {

      mIsCalled = true;
      return in;
    }
  }

  private static class TestPredicate implements Predicate<Object> {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public boolean test(final Object in) {

      mIsCalled = true;
      return in != null;
    }
  }

  private static class TestRunnable implements Runnable {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public void run() {

      mIsCalled = true;
    }
  }

  private static class TestSupplier implements Supplier<Object> {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public Object get() {

      mIsCalled = true;
      return this;
    }
  }
}
