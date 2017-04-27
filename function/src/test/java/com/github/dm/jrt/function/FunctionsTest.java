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
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.ActionDecorator;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiConsumerDecorator;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.BiFunctionDecorator;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.ConsumerDecorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.function.util.PredicateDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;
import com.github.dm.jrt.function.util.TriFunction;
import com.github.dm.jrt.function.util.TriFunctionDecorator;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.util.ActionDecorator.noOp;
import static com.github.dm.jrt.function.util.BiConsumerDecorator.biSink;
import static com.github.dm.jrt.function.util.BiFunctionDecorator.max;
import static com.github.dm.jrt.function.util.BiFunctionDecorator.maxBy;
import static com.github.dm.jrt.function.util.BiFunctionDecorator.min;
import static com.github.dm.jrt.function.util.BiFunctionDecorator.minBy;
import static com.github.dm.jrt.function.util.ConsumerDecorator.sink;
import static com.github.dm.jrt.function.util.FunctionDecorator.castTo;
import static com.github.dm.jrt.function.util.FunctionDecorator.identity;
import static com.github.dm.jrt.function.util.PredicateDecorator.isEqualTo;
import static com.github.dm.jrt.function.util.PredicateDecorator.isInstanceOf;
import static com.github.dm.jrt.function.util.PredicateDecorator.isNotNull;
import static com.github.dm.jrt.function.util.PredicateDecorator.isNull;
import static com.github.dm.jrt.function.util.PredicateDecorator.isSameAs;
import static com.github.dm.jrt.function.util.PredicateDecorator.negative;
import static com.github.dm.jrt.function.util.PredicateDecorator.positive;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Functions unit tests.
 * <p>
 * Created by davide-maestroni on 09/24/2015.
 */
public class FunctionsTest {

  @NotNull
  private static InvocationFactory<Object, String> createFactory() {

    return SupplierDecorator.factoryOf(new Supplier<Invocation<Object, String>>() {

      public Invocation<Object, String> get() {

        return new MappingInvocation<Object, String>(null) {

          public void onInput(final Object input, @NotNull final Channel<String, ?> result) {

            result.pass(input.toString());
          }
        };
      }
    });
  }

  @Test
  public void testAction() throws Exception {

    final TestAction action1 = new TestAction();
    final ActionDecorator action2 = ActionDecorator.decorate(action1);
    assertThat(ActionDecorator.decorate(action2)).isSameAs(action2);
    action2.perform();
    assertThat(action1.isCalled()).isTrue();
    action1.reset();
    final TestAction action3 = new TestAction();
    final ActionDecorator action4 = action2.andThen(action3);
    action4.perform();
    assertThat(action1.isCalled()).isTrue();
    assertThat(action3.isCalled()).isTrue();

    final TestRunnable runnable1 = new TestRunnable();
    final ActionDecorator action5 = ActionDecorator.decorate(runnable1);
    assertThat(ActionDecorator.decorate(action5)).isSameAs(action5);
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

    assertThat(ActionDecorator.decorate(new TestAction()).hasStaticScope()).isTrue();
    assertThat(ActionDecorator.decorate(new Action() {

      public void perform() {

      }
    }).hasStaticScope()).isFalse();

    assertThat(ActionDecorator.decorate(new TestRunnable()).hasStaticScope()).isTrue();
    assertThat(ActionDecorator.decorate(new Runnable() {

      public void run() {

      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testActionEquals() {

    final TestAction action1 = new TestAction();
    assertThat(ActionDecorator.decorate(action1)).isEqualTo(ActionDecorator.decorate(action1));
    final ActionDecorator action2 = ActionDecorator.decorate(action1);
    assertThat(action2).isEqualTo(action2);
    assertThat(action2).isNotEqualTo(null);
    assertThat(action2).isNotEqualTo("test");
    assertThat(ActionDecorator.decorate(action1).andThen(action2).hashCode()).isEqualTo(
        action2.andThen(action2).hashCode());
    assertThat(ActionDecorator.decorate(action1).andThen(action2)).isEqualTo(
        action2.andThen(action2));
    assertThat(action2.andThen(action2)).isEqualTo(
        ActionDecorator.decorate(action1).andThen(action2));
    assertThat(ActionDecorator.decorate(action1).andThen(action2).hashCode()).isEqualTo(
        action2.andThen(action1).hashCode());
    assertThat(ActionDecorator.decorate(action1).andThen(action2)).isEqualTo(
        action2.andThen(action1));
    assertThat(action2.andThen(action1)).isEqualTo(
        ActionDecorator.decorate(action1).andThen(action2));
    assertThat(ActionDecorator.decorate(action1).andThen(action2).hashCode()).isNotEqualTo(
        action2.andThen(action2.andThen(action1)).hashCode());
    assertThat(ActionDecorator.decorate(action1).andThen(action2)).isNotEqualTo(
        action2.andThen(action2.andThen(action1)));
    assertThat(action2.andThen(action2.andThen(action1))).isNotEqualTo(
        ActionDecorator.decorate(action1).andThen(action2));
    assertThat(ActionDecorator.decorate(action1).andThen(action1).hashCode()).isNotEqualTo(
        action2.andThen(action2.andThen(action1)).hashCode());
    assertThat(ActionDecorator.decorate(action1).andThen(action1)).isNotEqualTo(
        action2.andThen(action2.andThen(action1)));
    assertThat(action2.andThen(action2.andThen(action1))).isNotEqualTo(
        ActionDecorator.decorate(action1).andThen(action1));
    assertThat(action2.andThen(action1).hashCode()).isNotEqualTo(
        action2.andThen(noOp()).hashCode());
    assertThat(action2.andThen(action1)).isNotEqualTo(action2.andThen(noOp()));
    assertThat(action2.andThen(noOp())).isNotEqualTo(action2.andThen(action1));

    final TestRunnable runnable1 = new TestRunnable();
    assertThat(ActionDecorator.decorate(runnable1)).isEqualTo(ActionDecorator.decorate(runnable1));
    final ActionDecorator action3 = ActionDecorator.decorate(runnable1);
    assertThat(action3).isEqualTo(action3);
    assertThat(action3).isNotEqualTo(null);
    assertThat(action3).isNotEqualTo("test");
    assertThat(ActionDecorator.decorate(runnable1).andThen(action3).hashCode()).isEqualTo(
        action3.andThen(action3).hashCode());
    assertThat(ActionDecorator.decorate(runnable1).andThen(action3)).isEqualTo(
        action3.andThen(action3));
    assertThat(action3.andThen(action3)).isEqualTo(
        ActionDecorator.decorate(runnable1).andThen(action3));
    assertThat(ActionDecorator.decorate(runnable1).andThen(action3).hashCode()).isEqualTo(
        action3.andThen(runnable1).hashCode());
    assertThat(ActionDecorator.decorate(runnable1).andThen(action3)).isEqualTo(
        action3.andThen(runnable1));
    assertThat(action3.andThen(runnable1)).isEqualTo(
        ActionDecorator.decorate(runnable1).andThen(action3));
    assertThat(ActionDecorator.decorate(runnable1).andThen(action3).hashCode()).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)).hashCode());
    assertThat(ActionDecorator.decorate(runnable1).andThen(action3)).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)));
    assertThat(action3.andThen(action3.andThen(runnable1))).isNotEqualTo(
        ActionDecorator.decorate(runnable1).andThen(action3));
    assertThat(ActionDecorator.decorate(runnable1).andThen(runnable1).hashCode()).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)).hashCode());
    assertThat(ActionDecorator.decorate(runnable1).andThen(runnable1)).isNotEqualTo(
        action3.andThen(action3.andThen(runnable1)));
    assertThat(action3.andThen(action3.andThen(runnable1))).isNotEqualTo(
        ActionDecorator.decorate(runnable1).andThen(runnable1));
    assertThat(action3.andThen(runnable1).hashCode()).isNotEqualTo(
        action3.andThen(noOp()).hashCode());
    assertThat(action3.andThen(runnable1)).isNotEqualTo(action3.andThen(noOp()));
    assertThat(action3.andThen(noOp())).isNotEqualTo(action3.andThen(runnable1));
  }

  @Test
  public void testBiConsumer() throws Exception {

    final TestBiConsumer consumer1 = new TestBiConsumer();
    final BiConsumerDecorator<Object, Object> consumer2 = BiConsumerDecorator.decorate(consumer1);
    assertThat(BiConsumerDecorator.decorate(consumer2)).isSameAs(consumer2);
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

    assertThat(BiConsumerDecorator.decorate(new TestBiConsumer()).hasStaticScope()).isTrue();
    assertThat(BiConsumerDecorator.decorate(new BiConsumer<Object, Object>() {

      public void accept(final Object o, final Object o2) {

      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testBiConsumerEquals() {

    final TestBiConsumer consumer1 = new TestBiConsumer();
    assertThat(BiConsumerDecorator.decorate(consumer1)).isEqualTo(
        BiConsumerDecorator.decorate(consumer1));
    final BiConsumerDecorator<Object, Object> consumer2 = BiConsumerDecorator.decorate(consumer1);
    assertThat(consumer2).isEqualTo(consumer2);
    assertThat(consumer2).isNotEqualTo(null);
    assertThat(consumer2).isNotEqualTo("test");
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer2).hashCode());
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer2)).isEqualTo(
        consumer2.andThen(consumer2));
    assertThat(consumer2.andThen(consumer2)).isEqualTo(
        BiConsumerDecorator.decorate(consumer1).andThen(consumer2));
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer1).hashCode());
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer2)).isEqualTo(
        consumer2.andThen(consumer1));
    assertThat(consumer2.andThen(consumer1)).isEqualTo(
        BiConsumerDecorator.decorate(consumer1).andThen(consumer2));
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer2)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        BiConsumerDecorator.decorate(consumer1).andThen(consumer2));
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(BiConsumerDecorator.decorate(consumer1).andThen(consumer1)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        BiConsumerDecorator.decorate(consumer1).andThen(consumer1));
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

      BiConsumerDecorator.decorate((BiConsumer<?, ?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      BiConsumerDecorator.decorate(new TestBiConsumer()).andThen(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testBiFunction() throws Exception {

    final TestBiFunction function1 = new TestBiFunction();
    final BiFunctionDecorator<Object, Object, Object> function2 =
        BiFunctionDecorator.decorate(function1);
    assertThat(BiFunctionDecorator.decorate(function2)).isSameAs(function2);
    assertThat(function2.apply("test", function1)).isSameAs(function1);
    assertThat(function1.isCalled()).isTrue();
    function1.reset();
    final TestFunction function = new TestFunction();
    final BiFunctionDecorator<Object, Object, Object> function3 = function2.andThen(function);
    assertThat(function3.apply("test", function1)).isSameAs(function1);
    assertThat(function1.isCalled()).isTrue();
    assertThat(function.isCalled()).isTrue();
    assertThat(BiFunctionDecorator.<String, String>first().andThen(new Function<String, Integer>() {

      public Integer apply(final String s) {

        return s.length();
      }
    }).andThen(new Function<Integer, Integer>() {

      public Integer apply(final Integer integer) {

        return integer * 3;
      }
    }).apply("test", "long test")).isEqualTo(12);
    assertThat(
        BiFunctionDecorator.<String, Integer>second().andThen(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {

            return integer + 2;
          }
        }).apply("test", 3)).isEqualTo(5);
  }

  @Test
  public void testBiFunctionContext() {

    assertThat(BiFunctionDecorator.decorate(new TestBiFunction()).hasStaticScope()).isTrue();
    assertThat(BiFunctionDecorator.decorate(new BiFunction<Object, Object, Object>() {

      public Object apply(final Object o, final Object o2) {

        return null;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testBiFunctionEquals() {

    final TestBiFunction function1 = new TestBiFunction();
    assertThat(BiFunctionDecorator.decorate(function1)).isEqualTo(
        BiFunctionDecorator.decorate(function1));
    final BiFunctionDecorator<Object, Object, Object> function2 =
        BiFunctionDecorator.decorate(function1);
    assertThat(function2).isEqualTo(function2);
    assertThat(function2).isNotEqualTo(null);
    assertThat(function2).isNotEqualTo("test");
    final TestFunction function = new TestFunction();
    assertThat(BiFunctionDecorator.decorate(function1).andThen(function).hashCode()).isEqualTo(
        function2.andThen(function).hashCode());
    assertThat(BiFunctionDecorator.decorate(function1).andThen(function)).isEqualTo(
        function2.andThen(function));
    assertThat(function2.andThen(function)).isEqualTo(
        BiFunctionDecorator.decorate(function1).andThen(function));
    assertThat(BiFunctionDecorator.decorate(function1)
                                  .andThen(FunctionDecorator.decorate(function))
                                  .hashCode()).isEqualTo(function2.andThen(function).hashCode());
    assertThat(BiFunctionDecorator.decorate(function1)
                                  .andThen(FunctionDecorator.decorate(function))).isEqualTo(
        function2.andThen(function));
    assertThat(function2.andThen(function)).isEqualTo(
        BiFunctionDecorator.decorate(function1).andThen(FunctionDecorator.decorate(function)));
    assertThat(BiFunctionDecorator.decorate(function1)
                                  .andThen(FunctionDecorator.decorate(function))
                                  .hashCode()).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)).hashCode());
    assertThat(BiFunctionDecorator.decorate(function1)
                                  .andThen(FunctionDecorator.decorate(function))).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)));
    assertThat(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function))).isNotEqualTo(
        BiFunctionDecorator.decorate(function1).andThen(FunctionDecorator.decorate(function)));
    assertThat(BiFunctionDecorator.decorate(function1).andThen(function).hashCode()).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)).hashCode());
    assertThat(BiFunctionDecorator.decorate(function1).andThen(function)).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)));
    assertThat(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function))).isNotEqualTo(
        BiFunctionDecorator.decorate(function1).andThen(function));
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

      BiFunctionDecorator.decorate((BiFunction<?, ?, ?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      BiFunctionDecorator.decorate(new TestBiFunction()).andThen(null);

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
  public void testConstantBiFunction() throws Exception {

    final TestFunction function = new TestFunction();
    final BiFunctionDecorator<Object, Object, Object> constant =
        BiFunctionDecorator.constant("test").andThen(function);
    assertThat(constant.apply("TEST1", "TEST2")).isEqualTo("test");
    assertThat(function.isCalled()).isTrue();
  }

  @Test
  public void testConstantBiFunctionEquals() {

    final BiFunctionDecorator<String, String, String> function =
        BiFunctionDecorator.constant("test");
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(BiFunctionDecorator.constant("test"));
    assertThat(function).isNotEqualTo(BiFunctionDecorator.constant(1));
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(BiFunctionDecorator.constant("test").hashCode());
  }

  @Test
  public void testConstantFunction() throws Exception {

    final TestFunction function = new TestFunction();
    final FunctionDecorator<Object, Object> constant =
        FunctionDecorator.constant("test").andThen(function);
    assertThat(constant.apply("TEST")).isEqualTo("test");
    assertThat(function.isCalled()).isTrue();
  }

  @Test
  public void testConstantFunctionEquals() {

    final FunctionDecorator<String, String> function = FunctionDecorator.constant("test");
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(FunctionDecorator.constant("test"));
    assertThat(function).isNotEqualTo(FunctionDecorator.constant(1));
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(FunctionDecorator.constant("test").hashCode());
  }

  @Test
  public void testConstantSupplier() throws Exception {

    final TestFunction function = new TestFunction();
    final SupplierDecorator<Object> supplier = SupplierDecorator.constant("test").andThen(function);
    assertThat(supplier.get()).isEqualTo("test");
    assertThat(function.isCalled()).isTrue();
  }

  @Test
  public void testConstantSupplierEquals() {

    final SupplierDecorator<String> supplier = SupplierDecorator.constant("test");
    assertThat(supplier).isEqualTo(supplier);
    assertThat(supplier).isEqualTo(SupplierDecorator.constant("test"));
    assertThat(supplier).isNotEqualTo(SupplierDecorator.constant(1));
    assertThat(supplier).isNotEqualTo("");
    assertThat(supplier.hashCode()).isEqualTo(SupplierDecorator.constant("test").hashCode());
  }

  @Test
  public void testConstantTriFunction() throws Exception {

    final TestFunction function = new TestFunction();
    final TriFunctionDecorator<Object, Object, Object, Object> constant =
        TriFunctionDecorator.constant("test").andThen(function);
    assertThat(constant.apply("TEST1", "TEST2", "TEST3")).isEqualTo("test");
    assertThat(function.isCalled()).isTrue();
  }

  @Test
  public void testConstantTriFunctionEquals() {

    final TriFunctionDecorator<String, String, String, String> function =
        TriFunctionDecorator.constant("test");
    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(TriFunctionDecorator.constant("test"));
    assertThat(function).isNotEqualTo(TriFunctionDecorator.constant(1));
    assertThat(function).isNotEqualTo("");
    assertThat(function.hashCode()).isEqualTo(TriFunctionDecorator.constant("test").hashCode());
  }

  @Test
  public void testConsumer() throws Exception {

    final TestConsumer consumer1 = new TestConsumer();
    final ConsumerDecorator<Object> consumer2 = ConsumerDecorator.decorate(consumer1);
    assertThat(ConsumerDecorator.decorate(consumer2)).isSameAs(consumer2);
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

    assertThat(ConsumerDecorator.decorate(new TestConsumer()).hasStaticScope()).isTrue();
    assertThat(ConsumerDecorator.decorate(new Consumer<Object>() {

      public void accept(final Object o) {

      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testConsumerEquals() {

    final TestConsumer consumer1 = new TestConsumer();
    assertThat(ConsumerDecorator.decorate(consumer1)).isEqualTo(
        ConsumerDecorator.decorate(consumer1));
    final ConsumerDecorator<Object> consumer2 = ConsumerDecorator.decorate(consumer1);
    assertThat(consumer2).isEqualTo(consumer2);
    assertThat(consumer2).isNotEqualTo(null);
    assertThat(consumer2).isNotEqualTo("test");
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer2).hashCode());
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer2)).isEqualTo(
        consumer2.andThen(consumer2));
    assertThat(consumer2.andThen(consumer2)).isEqualTo(
        ConsumerDecorator.decorate(consumer1).andThen(consumer2));
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer2).hashCode()).isEqualTo(
        consumer2.andThen(consumer1).hashCode());
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer2)).isEqualTo(
        consumer2.andThen(consumer1));
    assertThat(consumer2.andThen(consumer1)).isEqualTo(
        ConsumerDecorator.decorate(consumer1).andThen(consumer2));
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer2)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        ConsumerDecorator.decorate(consumer1).andThen(consumer2));
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
    assertThat(ConsumerDecorator.decorate(consumer1).andThen(consumer1)).isNotEqualTo(
        consumer2.andThen(consumer2.andThen(consumer1)));
    assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
        ConsumerDecorator.decorate(consumer1).andThen(consumer1));
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

      ConsumerDecorator.decorate((Consumer<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      ConsumerDecorator.decorate(new TestConsumer()).andThen(null);

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

    final Routine<Object, String> routine = JRoutineCore.routine().of(createFactory());
    assertThat(routine.invoke().pass("test", 1).close().in(seconds(1)).all()).containsOnly("test",
        "1");
  }

  @Test
  public void testFactoryEquals() {

    final Supplier<Invocation<Object, Object>> supplier =
        SupplierDecorator.constant(IdentityInvocation.factory().newInvocation());
    final InvocationFactory<Object, String> factory = createFactory();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo(SupplierDecorator.factoryOf(supplier));
    assertThat(factory).isNotEqualTo(IdentityInvocation.factory());
    assertThat(factory).isNotEqualTo("");
    assertThat(SupplierDecorator.factoryOf(supplier)).isEqualTo(
        SupplierDecorator.factoryOf(supplier));
    assertThat(SupplierDecorator.factoryOf(supplier).hashCode()).isEqualTo(
        SupplierDecorator.factoryOf(supplier).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testFactoryError() {

    try {

      SupplierDecorator.factoryOf(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testFunction() throws Exception {

    final TestFunction function1 = new TestFunction();
    final FunctionDecorator<Object, Object> function2 = FunctionDecorator.decorate(function1);
    assertThat(FunctionDecorator.decorate(function2)).isSameAs(function2);
    assertThat(function2.apply("test")).isEqualTo("test");
    assertThat(function1.isCalled()).isTrue();
    function1.reset();
    final TestFunction function3 = new TestFunction();
    final FunctionDecorator<Object, Object> function4 = function2.andThen(function3);
    assertThat(function4.apply("test")).isEqualTo("test");
    assertThat(function1.isCalled()).isTrue();
    assertThat(function3.isCalled()).isTrue();
    final FunctionDecorator<String, Integer> function5 =
        FunctionDecorator.decorate(new Function<String, Integer>() {

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

    assertThat(FunctionDecorator.decorate(new TestFunction()).hasStaticScope()).isTrue();
    assertThat(FunctionDecorator.decorate(new Function<Object, Object>() {

      public Object apply(final Object o) {

        return null;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testFunctionEquals() {

    final TestFunction function1 = new TestFunction();
    assertThat(FunctionDecorator.decorate(function1)).isEqualTo(
        FunctionDecorator.decorate(function1));
    final FunctionDecorator<Object, Object> function2 = FunctionDecorator.decorate(function1);
    assertThat(function2).isEqualTo(function2);
    assertThat(function2).isNotEqualTo(null);
    assertThat(function2).isNotEqualTo("test");
    assertThat(FunctionDecorator.decorate(function1).andThen(function2).hashCode()).isEqualTo(
        function2.andThen(function2).hashCode());
    assertThat(FunctionDecorator.decorate(function1).andThen(function2)).isEqualTo(
        function2.andThen(function2));
    assertThat(function2.andThen(function2)).isEqualTo(
        FunctionDecorator.decorate(function1).andThen(function2));
    assertThat(FunctionDecorator.decorate(function1).andThen(function2).hashCode()).isEqualTo(
        function2.andThen(function1).hashCode());
    assertThat(FunctionDecorator.decorate(function1).andThen(function2)).isEqualTo(
        function2.andThen(function1));
    assertThat(function2.andThen(function1)).isEqualTo(
        FunctionDecorator.decorate(function1).andThen(function2));
    assertThat(FunctionDecorator.decorate(function1).andThen(function2).hashCode()).isNotEqualTo(
        function2.andThen(function2.andThen(function1)).hashCode());
    assertThat(FunctionDecorator.decorate(function1).andThen(function2)).isNotEqualTo(
        function2.andThen(function2.andThen(function1)));
    assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
        FunctionDecorator.decorate(function1).andThen(function2));
    assertThat(FunctionDecorator.decorate(function1).andThen(function1).hashCode()).isNotEqualTo(
        function2.andThen(function2.andThen(function1)).hashCode());
    assertThat(FunctionDecorator.decorate(function1).andThen(function1)).isNotEqualTo(
        function2.andThen(function2.andThen(function1)));
    assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
        FunctionDecorator.decorate(function1).andThen(function1));
    assertThat(function2.andThen(function1).hashCode()).isNotEqualTo(
        function2.andThen(identity()).hashCode());
    assertThat(function2.andThen(function1)).isNotEqualTo(function2.andThen(identity()));
    assertThat(function2.andThen(identity())).isNotEqualTo(function2.andThen(function1));
    assertThat(FunctionDecorator.decorate(function1).compose(function2).hashCode()).isEqualTo(
        function2.compose(function2).hashCode());
    assertThat(FunctionDecorator.decorate(function1).compose(function2)).isEqualTo(
        function2.compose(function2));
    assertThat(function2.compose(function2)).isEqualTo(
        FunctionDecorator.decorate(function1).compose(function2));
    assertThat(FunctionDecorator.decorate(function1).compose(function2).hashCode()).isEqualTo(
        function2.compose(function1).hashCode());
    assertThat(FunctionDecorator.decorate(function1).compose(function2)).isEqualTo(
        function2.compose(function1));
    assertThat(function2.compose(function1)).isEqualTo(
        FunctionDecorator.decorate(function1).compose(function2));
    assertThat(FunctionDecorator.decorate(function1).compose(function2).hashCode()).isNotEqualTo(
        function2.compose(function2.compose(function1)).hashCode());
    assertThat(FunctionDecorator.decorate(function1).compose(function2)).isNotEqualTo(
        function2.compose(function2.compose(function1)));
    assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
        FunctionDecorator.decorate(function1).compose(function2));
    assertThat(FunctionDecorator.decorate(function1).compose(function1).hashCode()).isNotEqualTo(
        function2.compose(function2.compose(function1)).hashCode());
    assertThat(FunctionDecorator.decorate(function1).compose(function1)).isNotEqualTo(
        function2.compose(function2.compose(function1)));
    assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
        FunctionDecorator.decorate(function1).compose(function1));
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

      FunctionDecorator.decorate((Function<?, ?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      FunctionDecorator.decorate(new TestFunction()).andThen(null);

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
    final PredicateDecorator<Object> predicate2 = PredicateDecorator.decorate(predicate1);
    assertThat(PredicateDecorator.decorate(predicate2)).isSameAs(predicate2);
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

    assertThat(PredicateDecorator.decorate(new TestPredicate()).hasStaticScope()).isTrue();
    assertThat(PredicateDecorator.decorate(new Predicate<Object>() {

      public boolean test(final Object o) {

        return false;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testPredicateEquals() {

    final TestPredicate predicate1 = new TestPredicate();
    assertThat(PredicateDecorator.decorate(predicate1)).isEqualTo(
        PredicateDecorator.decorate(predicate1));
    final PredicateDecorator<Object> predicate2 = PredicateDecorator.decorate(predicate1);
    assertThat(predicate2).isEqualTo(predicate2);
    assertThat(predicate2).isNotEqualTo(null);
    assertThat(predicate2).isNotEqualTo("test");
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate2).hashCode()).isEqualTo(
        predicate2.and(predicate2).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate2)).isEqualTo(
        predicate2.and(predicate2));
    assertThat(predicate2.and(predicate2)).isEqualTo(
        PredicateDecorator.decorate(predicate1).and(predicate2));
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate2).hashCode()).isEqualTo(
        predicate2.and(predicate1).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate2)).isEqualTo(
        predicate2.and(predicate1));
    assertThat(predicate2.and(predicate1)).isEqualTo(
        PredicateDecorator.decorate(predicate1).and(predicate2));
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate2).hashCode()).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate2)).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)));
    assertThat(predicate2.and(predicate2.and(predicate1))).isNotEqualTo(
        PredicateDecorator.decorate(predicate1).and(predicate2));
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate1).hashCode()).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).and(predicate1)).isNotEqualTo(
        predicate2.and(predicate2.and(predicate1)));
    assertThat(predicate2.and(predicate2.and(predicate1))).isNotEqualTo(
        PredicateDecorator.decorate(predicate1).and(predicate1));
    assertThat(predicate2.and(predicate1).hashCode()).isNotEqualTo(
        predicate2.and(positive()).hashCode());
    assertThat(predicate2.and(predicate1)).isNotEqualTo(predicate2.and(positive()));
    assertThat(predicate2.and(positive())).isNotEqualTo(predicate2.and(predicate1));
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate2).hashCode()).isEqualTo(
        predicate2.or(predicate2).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate2)).isEqualTo(
        predicate2.or(predicate2));
    assertThat(predicate2.or(predicate2)).isEqualTo(
        PredicateDecorator.decorate(predicate1).or(predicate2));
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate2).hashCode()).isEqualTo(
        predicate2.or(predicate1).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate2)).isEqualTo(
        predicate2.or(predicate1));
    assertThat(predicate2.or(predicate1)).isEqualTo(
        PredicateDecorator.decorate(predicate1).or(predicate2));
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate2).hashCode()).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate2)).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)));
    assertThat(predicate2.or(predicate2.or(predicate1))).isNotEqualTo(
        PredicateDecorator.decorate(predicate1).or(predicate2));
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate1).hashCode()).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)).hashCode());
    assertThat(PredicateDecorator.decorate(predicate1).or(predicate1)).isNotEqualTo(
        predicate2.or(predicate2.or(predicate1)));
    assertThat(predicate2.or(predicate2.or(predicate1))).isNotEqualTo(
        PredicateDecorator.decorate(predicate1).or(predicate1));
    assertThat(predicate2.or(predicate1).hashCode()).isNotEqualTo(
        predicate2.or(positive()).hashCode());
    assertThat(predicate2.or(predicate1)).isNotEqualTo(predicate2.or(positive()));
    assertThat(predicate2.or(positive())).isNotEqualTo(predicate2.or(predicate1));
    assertThat(predicate2.negate().negate()).isEqualTo(PredicateDecorator.decorate(predicate1));
    assertThat(predicate2.and(predicate1).negate()).isEqualTo(
        predicate2.negate().or(PredicateDecorator.decorate(predicate1).negate()));
    assertThat(predicate2.and(predicate1).negate().hashCode()).isEqualTo(
        predicate2.negate().or(PredicateDecorator.decorate(predicate1).negate()).hashCode());
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

      PredicateDecorator.decorate((Predicate<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      PredicateDecorator.decorate(new TestPredicate()).and(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      PredicateDecorator.decorate(new TestPredicate()).or(null);

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
    final SupplierDecorator<Object> supplier2 = SupplierDecorator.decorate(supplier1);
    assertThat(SupplierDecorator.decorate(supplier2)).isSameAs(supplier2);
    assertThat(supplier2.get()).isSameAs(supplier1);
    assertThat(supplier1.isCalled()).isTrue();
    supplier1.reset();
    final TestFunction function = new TestFunction();
    final SupplierDecorator<Object> supplier3 = supplier2.andThen(function);
    assertThat(supplier3.get()).isSameAs(supplier1);
    assertThat(supplier1.isCalled()).isTrue();
    assertThat(function.isCalled()).isTrue();
    assertThat(SupplierDecorator.constant("test").andThen(new Function<String, Integer>() {

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

    assertThat(SupplierDecorator.decorate(new TestSupplier()).hasStaticScope()).isTrue();
    assertThat(SupplierDecorator.decorate(new Supplier<Object>() {

      public Object get() {

        return null;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testSupplierEquals() {

    final TestSupplier supplier1 = new TestSupplier();
    assertThat(SupplierDecorator.decorate(supplier1)).isEqualTo(
        SupplierDecorator.decorate(supplier1));
    final SupplierDecorator<Object> supplier2 = SupplierDecorator.decorate(supplier1);
    assertThat(supplier2).isEqualTo(supplier2);
    assertThat(supplier2).isNotEqualTo(null);
    assertThat(supplier2).isNotEqualTo("test");
    final TestFunction function = new TestFunction();
    assertThat(SupplierDecorator.decorate(supplier1).andThen(function).hashCode()).isEqualTo(
        supplier2.andThen(function).hashCode());
    assertThat(SupplierDecorator.decorate(supplier1).andThen(function)).isEqualTo(
        supplier2.andThen(function));
    assertThat(supplier2.andThen(function)).isEqualTo(
        SupplierDecorator.decorate(supplier1).andThen(function));
    assertThat(SupplierDecorator.decorate(supplier1)
                                .andThen(FunctionDecorator.decorate(function))
                                .hashCode()).isEqualTo(supplier2.andThen(function).hashCode());
    assertThat(SupplierDecorator.decorate(supplier1)
                                .andThen(FunctionDecorator.decorate(function))).isEqualTo(
        supplier2.andThen(function));
    assertThat(supplier2.andThen(function)).isEqualTo(
        SupplierDecorator.decorate(supplier1).andThen(FunctionDecorator.decorate(function)));
    assertThat(SupplierDecorator.decorate(supplier1)
                                .andThen(FunctionDecorator.decorate(function))
                                .hashCode()).isNotEqualTo(
        supplier2.andThen(FunctionDecorator.decorate(function).andThen(function)).hashCode());
    assertThat(SupplierDecorator.decorate(supplier1)
                                .andThen(FunctionDecorator.decorate(function))).isNotEqualTo(
        supplier2.andThen(FunctionDecorator.decorate(function).andThen(function)));
    assertThat(
        supplier2.andThen(FunctionDecorator.decorate(function).andThen(function))).isNotEqualTo(
        SupplierDecorator.decorate(supplier1).andThen(FunctionDecorator.decorate(function)));
    assertThat(SupplierDecorator.decorate(supplier1).andThen(function).hashCode()).isNotEqualTo(
        supplier2.andThen(FunctionDecorator.decorate(function).andThen(function)).hashCode());
    assertThat(SupplierDecorator.decorate(supplier1).andThen(function)).isNotEqualTo(
        supplier2.andThen(FunctionDecorator.decorate(function).andThen(function)));
    assertThat(
        supplier2.andThen(FunctionDecorator.decorate(function).andThen(function))).isNotEqualTo(
        SupplierDecorator.decorate(supplier1).andThen(function));
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

      SupplierDecorator.decorate((Supplier<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      SupplierDecorator.decorate(new TestSupplier()).andThen(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testTriFunction() throws Exception {

    final TestTriFunction function1 = new TestTriFunction();
    final TriFunctionDecorator<Object, Object, Object, Object> function2 =
        TriFunctionDecorator.decorate(function1);
    assertThat(TriFunctionDecorator.decorate(function2)).isSameAs(function2);
    assertThat(function2.apply("test", 1, function1)).isSameAs(function1);
    assertThat(function1.isCalled()).isTrue();
    function1.reset();
    final TestFunction function = new TestFunction();
    final TriFunctionDecorator<Object, Object, Object, Object> function3 =
        function2.andThen(function);
    assertThat(function3.apply("test", 1, function1)).isSameAs(function1);
    assertThat(function1.isCalled()).isTrue();
    assertThat(function.isCalled()).isTrue();
    assertThat(TriFunctionDecorator.<String, String, String>first().andThen(
        new Function<String, Integer>() {

          public Integer apply(final String s) {

            return s.length();
          }
        }).andThen(new Function<Integer, Integer>() {

      public Integer apply(final Integer integer) {

        return integer * 3;
      }
    }).apply("test", "medium test", "long test")).isEqualTo(12);
    assertThat(TriFunctionDecorator.<String, String, String>second().andThen(
        new Function<String, Integer>() {

          public Integer apply(final String s) {

            return s.length();
          }
        }).andThen(new Function<Integer, Integer>() {

      public Integer apply(final Integer integer) {

        return integer * 3;
      }
    }).apply("test", "medium test", "long test")).isEqualTo(33);
    assertThat(TriFunctionDecorator.<String, String, Integer>third().andThen(
        new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {

            return integer + 2;
          }
        }).apply("test", "medium test", 3)).isEqualTo(5);
  }

  @Test
  public void testTriFunctionContext() {

    assertThat(TriFunctionDecorator.decorate(new TestTriFunction()).hasStaticScope()).isTrue();
    assertThat(TriFunctionDecorator.decorate(new TriFunction<Object, Object, Object, Object>() {

      public Object apply(final Object o, final Object o2, final Object o3) {
        return null;
      }
    }).hasStaticScope()).isFalse();
  }

  @Test
  public void testTriFunctionEquals() {

    final TestTriFunction function1 = new TestTriFunction();
    assertThat(TriFunctionDecorator.decorate(function1)).isEqualTo(
        TriFunctionDecorator.decorate(function1));
    final TriFunctionDecorator<Object, Object, Object, Object> function2 =
        TriFunctionDecorator.decorate(function1);
    assertThat(function2).isEqualTo(function2);
    assertThat(function2).isNotEqualTo(null);
    assertThat(function2).isNotEqualTo("test");
    final TestFunction function = new TestFunction();
    assertThat(TriFunctionDecorator.decorate(function1).andThen(function).hashCode()).isEqualTo(
        function2.andThen(function).hashCode());
    assertThat(TriFunctionDecorator.decorate(function1).andThen(function)).isEqualTo(
        function2.andThen(function));
    assertThat(function2.andThen(function)).isEqualTo(
        TriFunctionDecorator.decorate(function1).andThen(function));
    assertThat(TriFunctionDecorator.decorate(function1)
                                   .andThen(FunctionDecorator.decorate(function))
                                   .hashCode()).isEqualTo(function2.andThen(function).hashCode());
    assertThat(TriFunctionDecorator.decorate(function1)
                                   .andThen(FunctionDecorator.decorate(function))).isEqualTo(
        function2.andThen(function));
    assertThat(function2.andThen(function)).isEqualTo(
        TriFunctionDecorator.decorate(function1).andThen(FunctionDecorator.decorate(function)));
    assertThat(TriFunctionDecorator.decorate(function1)
                                   .andThen(FunctionDecorator.decorate(function))
                                   .hashCode()).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)).hashCode());
    assertThat(TriFunctionDecorator.decorate(function1)
                                   .andThen(FunctionDecorator.decorate(function))).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)));
    assertThat(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function))).isNotEqualTo(
        TriFunctionDecorator.decorate(function1).andThen(FunctionDecorator.decorate(function)));
    assertThat(TriFunctionDecorator.decorate(function1).andThen(function).hashCode()).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)).hashCode());
    assertThat(TriFunctionDecorator.decorate(function1).andThen(function)).isNotEqualTo(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function)));
    assertThat(
        function2.andThen(FunctionDecorator.decorate(function).andThen(function))).isNotEqualTo(
        TriFunctionDecorator.decorate(function1).andThen(function));
    assertThat(function2.andThen(function).hashCode()).isNotEqualTo(
        function2.andThen(identity()).hashCode());
    assertThat(function2.andThen(function)).isNotEqualTo(function2.andThen(identity()));
    assertThat(function2.andThen(identity())).isNotEqualTo(function2.andThen(function));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testTriFunctionError() {

    try {

      TriFunctionDecorator.decorate(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      TriFunctionDecorator.decorate((TriFunction<?, ?, ?, ?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      TriFunctionDecorator.decorate(new TestTriFunction()).andThen(null);

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

  private static class TestTriFunction implements TriFunction<Object, Object, Object, Object> {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void reset() {

      mIsCalled = false;
    }

    public Object apply(final Object in1, final Object in2, final Object in3) {
      mIsCalled = true;
      return in3;
    }
  }
}
