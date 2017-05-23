/*
 * Copyright 2017 Davide Maestroni
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
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.builder.StatefulRoutineBuilder;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.TriFunction;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.immediateExecutor;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stateful routine builder unit tests.
 * <p>
 * Created by davide-maestroni on 03/02/2017.
 */
public class StatefulRoutineBuilderTest {

  @Test
  public void testCompleteState() {
    final AtomicBoolean state = new AtomicBoolean(true);
    JRoutineFunction.<String, Void, AtomicBoolean>statefulRoutineOn(immediateExecutor()).onCreate(
        new Supplier<AtomicBoolean>() {

          public AtomicBoolean get() {
            return state;
          }
        }).onCompleteState(new Function<AtomicBoolean, AtomicBoolean>() {

      public AtomicBoolean apply(final AtomicBoolean atomicBoolean) {
        atomicBoolean.set(false);
        return atomicBoolean;
      }
    }).create().invoke().close();
    assertThat(state.get()).isFalse();
  }

  @Test
  public void testDestroy() {
    final AtomicBoolean state = new AtomicBoolean(true);
    final StatefulRoutineBuilder<String, Void, AtomicBoolean> builder =
        JRoutineFunction.statefulRoutineOn(immediateExecutor());
    final Routine<String, Void> routine = builder.onCreate(new Supplier<AtomicBoolean>() {

      public AtomicBoolean get() {
        return state;
      }
    })
                                                 .onFinalize(
                                                     FunctionDecorator.<AtomicBoolean>identity())
                                                 .onDestroy(new Consumer<AtomicBoolean>() {

                                                   public void accept(
                                                       final AtomicBoolean atomicBoolean) {
                                                     atomicBoolean.set(false);
                                                   }
                                                 })
                                                 .create();
    routine.invoke().close();
    assertThat(state.get()).isTrue();
    routine.clear();
    assertThat(state.get()).isFalse();
  }

  @Test
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void testError() {
    final AtomicReference<RoutineException> reference = new AtomicReference<RoutineException>();
    final Channel<Void, Void> channel =
        JRoutineFunction.<Void, Void, RoutineException>statefulRoutineOn(
            immediateExecutor()).onError(
            new BiFunction<RoutineException, RoutineException, RoutineException>() {

              public RoutineException apply(final RoutineException state,
                  final RoutineException e) {
                reference.set(e);
                return null;
              }
            }).create().invoke();
    assertThat(reference.get()).isNull();
    channel.abort(new IOException());
    assertThat(reference.get()).isExactlyInstanceOf(AbortException.class);
    assertThat(reference.get().getCause()).isExactlyInstanceOf(IOException.class);
  }

  @Test
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void testErrorConsume() {
    final AtomicReference<RoutineException> reference = new AtomicReference<RoutineException>();
    final Channel<Void, Void> channel =
        JRoutineFunction.<Void, Void, RoutineException>statefulRoutineOn(
            immediateExecutor()).onErrorConsume(
            new BiConsumer<RoutineException, RoutineException>() {

              public void accept(final RoutineException state, final RoutineException e) {
                reference.set(e);
              }
            }).create().invoke();
    assertThat(reference.get()).isNull();
    channel.abort(new IOException());
    assertThat(reference.get()).isExactlyInstanceOf(AbortException.class);
    assertThat(reference.get().getCause()).isExactlyInstanceOf(IOException.class);
  }

  @Test
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void testErrorException() {
    final AtomicReference<RoutineException> reference = new AtomicReference<RoutineException>();
    final Channel<Void, Void> channel =
        JRoutineFunction.<Void, Void, RoutineException>statefulRoutineOn(
            immediateExecutor()).onErrorException(
            new Function<RoutineException, RoutineException>() {

              public RoutineException apply(final RoutineException e) {
                reference.set(e);
                return null;
              }
            }).create().invoke();
    assertThat(reference.get()).isNull();
    channel.abort(new IOException());
    assertThat(reference.get()).isExactlyInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testErrorState() {
    final AtomicBoolean state = new AtomicBoolean(true);
    final Channel<Void, Void> channel =
        JRoutineFunction.<Void, Void, AtomicBoolean>statefulRoutineOn(immediateExecutor()).onCreate(
            new Supplier<AtomicBoolean>() {

              public AtomicBoolean get() {
                return state;
              }
            }).onErrorState(new Function<AtomicBoolean, AtomicBoolean>() {

          public AtomicBoolean apply(final AtomicBoolean atomicBoolean) {
            atomicBoolean.set(false);
            return atomicBoolean;
          }
        }).create().invoke();
    assertThat(state.get()).isTrue();
    channel.abort(new IOException());
    assertThat(state.get()).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFactory() {
    final InvocationFactory<Integer, List<Integer>> factory =
        JRoutineFunction.<Integer, List<Integer>, List<Integer>>statefulFactory().onCreate(
            new Supplier<List<Integer>>() {

              public List<Integer> get() {
                return new ArrayList<Integer>();
              }
            }).onNextConsume(new BiConsumer<List<Integer>, Integer>() {

          public void accept(final List<Integer> list, final Integer integer) {
            list.add(integer + 1);
          }
        }).onCompleteOutput(FunctionDecorator.<List<Integer>>identity()).create();
    final Routine<Integer, List<Integer>> routine = JRoutineCore.routine().of(factory);
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(
        Arrays.asList(2, 3, 4, 5));
  }

  @Test
  public void testFinalizeConsume() {
    final AtomicBoolean state = new AtomicBoolean(true);
    final StatefulRoutineBuilder<String, Void, AtomicBoolean> builder =
        JRoutineFunction.statefulRoutineOn(immediateExecutor());
    final Routine<String, Void> routine = builder.onCreate(new Supplier<AtomicBoolean>() {

      public AtomicBoolean get() {
        return state;
      }
    }).onFinalizeConsume(new Consumer<AtomicBoolean>() {

      public void accept(final AtomicBoolean atomicBoolean) {
        atomicBoolean.set(false);
      }
    }).create();
    routine.invoke().close();
    assertThat(state.get()).isFalse();
  }

  @Test
  public void testIncrementArray() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 1;
              }
            }).onNextArray(new BiFunction<Integer, Integer, Integer[]>() {

          public Integer[] apply(final Integer integer1, final Integer integer2) {
            final Integer[] integers = new Integer[1];
            integers[0] = integer1 + integer2;
            return integers;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsExactly(2, 3,
        4, 5);
  }

  @Test
  public void testIncrementIterable() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 1;
              }
            }).onNextIterable(new BiFunction<Integer, Integer, Iterable<? extends Integer>>() {

          public Iterable<? extends Integer> apply(final Integer integer1, final Integer integer2) {
            return Collections.singleton(integer1 + integer2);
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsExactly(2, 3,
        4, 5);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testIncrementList() {
    final Routine<Integer, List<Integer>> routine =
        JRoutineFunction.<Integer, List<Integer>, List<Integer>>statefulRoutine().onCreate(
            new Supplier<List<Integer>>() {

              public List<Integer> get() {
                return new ArrayList<Integer>();
              }
            }).onNextConsume(new BiConsumer<List<Integer>, Integer>() {

          public void accept(final List<Integer> list, final Integer integer) {
            list.add(integer + 1);
          }
        }).onCompleteOutput(FunctionDecorator.<List<Integer>>identity()).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(
        Arrays.asList(2, 3, 4, 5));
  }

  @Test
  public void testIncrementOutput() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 1;
              }
            }).onNextOutput(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsExactly(2, 3,
        4, 5);
  }

  @Test
  public void testSumArray() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 0;
              }
            }).onNextState(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).onCompleteArray(new Function<Integer, Integer[]>() {

          public Integer[] apply(final Integer integer) {
            final Integer[] integers = new Integer[1];
            integers[0] = integer;
            return integers;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(10);
  }

  @Test
  public void testSumConsume() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 0;
              }
            }).onNext(new TriFunction<Integer, Integer, Channel<Integer, ?>, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2,
              final Channel<Integer, ?> result) {
            return integer1 + integer2;
          }
        }).onCompleteConsume(new BiConsumer<Integer, Channel<Integer, ?>>() {

          public void accept(final Integer integer, final Channel<Integer, ?> result) {
            result.pass(integer);
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(10);
  }

  @Test
  public void testSumDefault() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 0;
              }
            }).onNext(new TriFunction<Integer, Integer, Channel<Integer, ?>, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2,
              final Channel<Integer, ?> result) {
            return integer1 + integer2;
          }
        }).onComplete(new BiFunction<Integer, Channel<Integer, ?>, Integer>() {

          public Integer apply(final Integer integer, final Channel<Integer, ?> result) {
            result.pass(integer);
            return null;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(10);
  }

  @Test
  public void testSumIterable() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 0;
              }
            }).onNextState(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).onCompleteIterable(new Function<Integer, Iterable<? extends Integer>>() {

          public Iterable<? extends Integer> apply(final Integer integer) {
            return Collections.singleton(integer);
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(10);
  }

  @Test
  public void testSumOutput() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 0;
              }
            }).onNextState(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).onCompleteOutput(FunctionDecorator.<Integer>identity()).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(10);
  }

  @Test
  public void testSumState() {
    final Routine<Integer, Integer> routine =
        JRoutineFunction.<Integer, Integer, Integer>statefulRoutine().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 0;
              }
            }).onNextState(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).onComplete(new BiFunction<Integer, Channel<Integer, ?>, Integer>() {

          public Integer apply(final Integer integer, final Channel<Integer, ?> result) {
            result.pass(integer);
            return null;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(1)).all()).containsOnly(10);
  }
}
