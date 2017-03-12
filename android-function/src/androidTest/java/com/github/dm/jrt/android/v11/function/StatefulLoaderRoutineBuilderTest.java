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

package com.github.dm.jrt.android.v11.function;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.function.RemoteInvocationService;
import com.github.dm.jrt.android.function.builder.StatefulLoaderRoutineBuilder;
import com.github.dm.jrt.android.v4.function.TestActivity;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.TriFunction;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stateful Loader routine builder unit tests.
 * <p>
 * Created by davide-maestroni on 03/07/2017.
 */
@TargetApi(VERSION_CODES.FROYO)
public class StatefulLoaderRoutineBuilderTest
    extends ActivityInstrumentationTestCase2<TestActivity> {

  public StatefulLoaderRoutineBuilderTest() {
    super(TestActivity.class);
  }

  private static void testCompleteState(final Activity activity) {
    final AtomicBoolean state = new AtomicBoolean(true);
    assertThat(JRoutineLoaderFunction.<String, Void, AtomicBoolean>stateful(loaderFrom(activity), 0)
        .onCreate(new Supplier<AtomicBoolean>() {

          public AtomicBoolean get() {
            return state;
          }
        })
        .onCompleteState(new Function<AtomicBoolean, AtomicBoolean>() {

          public AtomicBoolean apply(final AtomicBoolean atomicBoolean) {
            atomicBoolean.set(false);
            return atomicBoolean;
          }
        })
        .invocationConfiguration()
        .withRunner(Runners.immediateRunner())
        .apply()
        .invoke()
        .close()
        .in(seconds(10))
        .getComplete()).isTrue();
    assertThat(state.get()).isFalse();
  }

  private static void testContextConsume(final Activity activity) {
    final AtomicBoolean state = new AtomicBoolean(true);
    assertThat(JRoutineLoaderFunction.<String, Void, AtomicBoolean>stateful(loaderFrom(activity), 0)
        .onContextConsume(new Consumer<Context>() {

          @Override
          public void accept(final Context context) {
            state.set(false);
          }
        })
        .invoke()
        .close()
        .in(seconds(10))
        .getComplete()).isTrue();
    assertThat(state.get()).isFalse();
  }

  private static void testDestroy(final Activity activity) throws InterruptedException {
    final AtomicBoolean state = new AtomicBoolean(true);
    final StatefulLoaderRoutineBuilder<String, Void, AtomicBoolean> builder =
        JRoutineLoaderFunction.stateful(loaderFrom(activity), 0);
    final LoaderRoutine<String, Void> routine = builder.onCreate(new Supplier<AtomicBoolean>() {

      public AtomicBoolean get() {
        return state;
      }
    })
                                                       .onFinalize(
                                                           FunctionDecorator
                                                               .<AtomicBoolean>identity())
                                                       .onDestroy(new Consumer<AtomicBoolean>() {

                                                         public void accept(
                                                             final AtomicBoolean atomicBoolean) {
                                                           atomicBoolean.set(false);
                                                         }
                                                       })
                                                       .invocationConfiguration()
                                                       .withRunner(Runners.immediateRunner())
                                                       .apply()
                                                       .loaderConfiguration()
                                                       .withCacheStrategy(CacheStrategyType.CACHE)
                                                       .apply()
                                                       .buildRoutine();
    assertThat(routine.invoke().close().in(seconds(10)).getComplete()).isTrue();
    assertThat(state.get()).isTrue();
    routine.clear();
    seconds(2).sleepAtLeast();
    assertThat(state.get()).isFalse();
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  private static void testError(final Activity activity) {
    final AtomicReference<RoutineException> reference = new AtomicReference<RoutineException>();
    final Channel<Void, Void> channel =
        JRoutineLoaderFunction.<Void, Void, RoutineException>stateful(loaderFrom(activity),
            0).onError(new BiFunction<RoutineException, RoutineException, RoutineException>() {

          public RoutineException apply(final RoutineException state, final RoutineException e) {
            reference.set(e);
            return null;
          }
        }).invocationConfiguration().withRunner(Runners.immediateRunner()).apply().invoke();
    assertThat(reference.get()).isNull();
    channel.abort(new IOException());
    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
    assertThat(reference.get()).isExactlyInstanceOf(AbortException.class);
    assertThat(reference.get().getCause()).isExactlyInstanceOf(IOException.class);
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  private static void testErrorConsume(final Activity activity) {
    final AtomicReference<RoutineException> reference = new AtomicReference<RoutineException>();
    final Channel<Void, Void> channel =
        JRoutineLoaderFunction.<Void, Void, RoutineException>stateful(loaderFrom(activity),
            0).onErrorConsume(new BiConsumer<RoutineException, RoutineException>() {

          public void accept(final RoutineException state, final RoutineException e) {
            reference.set(e);
          }
        }).invocationConfiguration().withRunner(Runners.immediateRunner()).apply().invoke();
    assertThat(reference.get()).isNull();
    channel.abort(new IOException());
    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
    assertThat(reference.get()).isExactlyInstanceOf(AbortException.class);
    assertThat(reference.get().getCause()).isExactlyInstanceOf(IOException.class);
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  private static void testErrorException(final Activity activity) {
    final AtomicReference<RoutineException> reference = new AtomicReference<RoutineException>();
    final Channel<Void, Void> channel =
        JRoutineLoaderFunction.<Void, Void, RoutineException>stateful(loaderFrom(activity),
            0).onErrorException(new Function<RoutineException, RoutineException>() {

          public RoutineException apply(final RoutineException e) {
            reference.set(e);
            return null;
          }
        }).invocationConfiguration().withRunner(Runners.immediateRunner()).apply().invoke();
    assertThat(reference.get()).isNull();
    channel.abort(new IOException());
    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
    assertThat(reference.get()).isExactlyInstanceOf(AbortException.class);
  }

  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  private static void testErrorState(final Activity activity) {
    final AtomicBoolean state = new AtomicBoolean(true);
    final Channel<Void, Void> channel =
        JRoutineLoaderFunction.<Void, Void, AtomicBoolean>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<AtomicBoolean>() {

          public AtomicBoolean get() {
            return state;
          }
        }).onErrorState(new Function<AtomicBoolean, AtomicBoolean>() {

          public AtomicBoolean apply(final AtomicBoolean atomicBoolean) {
            atomicBoolean.set(false);
            return atomicBoolean;
          }
        }).invocationConfiguration().withRunner(Runners.immediateRunner()).apply().invoke();
    assertThat(state.get()).isTrue();
    channel.abort(new IOException());
    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
    assertThat(state.get()).isFalse();
  }

  private static void testFinalizeConsume(final Activity activity) {
    final AtomicBoolean state = new AtomicBoolean(true);
    final StatefulLoaderRoutineBuilder<String, Void, AtomicBoolean> builder =
        JRoutineLoaderFunction.stateful(loaderFrom(activity), 0);
    final LoaderRoutine<String, Void> routine = builder.onCreate(new Supplier<AtomicBoolean>() {

      public AtomicBoolean get() {
        return state;
      }
    }).onFinalizeConsume(new Consumer<AtomicBoolean>() {

      public void accept(final AtomicBoolean atomicBoolean) {
        atomicBoolean.set(false);
      }
    }).invocationConfiguration().withRunner(Runners.immediateRunner()).apply().buildRoutine();
    assertThat(routine.invoke().close().in(seconds(10)).getComplete()).isTrue();
    assertThat(state.get()).isFalse();
  }

  private static void testIncrementArray(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

          public Integer get() {
            return 1;
          }
        }).onNextArray(new BiFunction<Integer, Integer, Integer[]>() {

          public Integer[] apply(final Integer integer1, final Integer integer2) {
            final Integer[] integers = new Integer[1];
            integers[0] = integer1 + integer2;
            return integers;
          }
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testIncrementIterable(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

          public Integer get() {
            return 1;
          }
        }).onNextIterable(new BiFunction<Integer, Integer, Iterable<? extends Integer>>() {

          public Iterable<? extends Integer> apply(final Integer integer1, final Integer integer2) {
            return Collections.singleton(integer1 + integer2);
          }
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  @SuppressWarnings("unchecked")
  private static void testIncrementList(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, List<Integer>, List<Integer>> routine =
        JRoutineLoaderFunction.<Integer, List<Integer>, List<Integer>>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<List<Integer>>() {

          public List<Integer> get() {
            return new ArrayList<Integer>();
          }
        }).onNextConsume(new BiConsumer<List<Integer>, Integer>() {

          public void accept(final List<Integer> list, final Integer integer) {
            list.add(integer + 1);
          }
        }).onCompleteOutput(FunctionDecorator.<List<Integer>>identity());
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(
        Arrays.asList(2, 3, 4, 5));
  }

  private static void testIncrementOutput(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

          public Integer get() {
            return 1;
          }
        }).onNextOutput(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testIncrementRemoteService(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, ServiceState> routine =
        JRoutineLoaderFunction.<Integer, Integer, ServiceState>stateful(loaderFrom(activity),
            0).onContext(new Function<Context, ServiceState>() {

          @Override
          public ServiceState apply(final Context context) {
            return new ServiceState(context, RemoteInvocationService.class);
          }
        }).onCreateState(new Function<ServiceState, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState) {
            return serviceState.invoke();
          }
        }).onNext(new TriFunction<ServiceState, Integer, Channel<Integer, ?>, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState, final Integer integer,
              final Channel<Integer, ?> result) {
            return serviceState.next(integer, result);
          }
        }).onError(new BiFunction<ServiceState, RoutineException, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState, final RoutineException e) {
            return serviceState.abort(e);
          }
        }).onComplete(new BiFunction<ServiceState, Channel<Integer, ?>, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState,
              final Channel<Integer, ?> result) {
            return serviceState.close();
          }
        }).onFinalize(new Function<ServiceState, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState) {
            return serviceState.reset();
          }
        }).onDestroy(new Consumer<ServiceState>() {

          @Override
          public void accept(final ServiceState serviceState) {
            serviceState.clear();
          }
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testIncrementService(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, ServiceState> routine =
        JRoutineLoaderFunction.<Integer, Integer, ServiceState>stateful(loaderFrom(activity),
            0).onContext(new Function<Context, ServiceState>() {

          @Override
          public ServiceState apply(final Context context) {
            return new ServiceState(context);
          }
        }).onCreateState(new Function<ServiceState, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState) {
            return serviceState.invoke();
          }
        }).onNext(new TriFunction<ServiceState, Integer, Channel<Integer, ?>, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState, final Integer integer,
              final Channel<Integer, ?> result) {
            return serviceState.next(integer, result);
          }
        }).onError(new BiFunction<ServiceState, RoutineException, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState, final RoutineException e) {
            return serviceState.abort(e);
          }
        }).onComplete(new BiFunction<ServiceState, Channel<Integer, ?>, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState,
              final Channel<Integer, ?> result) {
            return serviceState.close();
          }
        }).onFinalize(new Function<ServiceState, ServiceState>() {

          @Override
          public ServiceState apply(final ServiceState serviceState) {
            return serviceState.reset();
          }
        }).onDestroy(new Consumer<ServiceState>() {

          @Override
          public void accept(final ServiceState serviceState) {
            serviceState.clear();
          }
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testList(final Activity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer>statefulList(loaderFrom(activity),
            0).onCompleteOutput(new Function<List<Integer>, Integer>() {

          public Integer apply(final List<Integer> list) {
            return list.size();
          }
        }).invocationConfiguration().withRunner(Runners.immediateRunner()).apply().buildRoutine();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(4);
  }

  private static void testSumArray(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

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
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(10);
  }

  private static void testSumConsume(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

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
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(10);
  }

  private static void testSumDefault(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

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
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(10);
  }

  private static void testSumIterable(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

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
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(10);
  }

  private static void testSumOutput(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

          public Integer get() {
            return 0;
          }
        }).onNextState(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).onCompleteOutput(FunctionDecorator.<Integer>identity());
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(10);
  }

  private static void testSumState(final Activity activity) {
    final StatefulLoaderRoutineBuilder<Integer, Integer, Integer> routine =
        JRoutineLoaderFunction.<Integer, Integer, Integer>stateful(loaderFrom(activity),
            0).onCreate(new Supplier<Integer>() {

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
        });
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(10);
  }

  public void testCompleteState() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testCompleteState(getActivity());
  }

  public void testContextConsume() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testContextConsume(getActivity());
  }

  public void testDestroy() throws InterruptedException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testDestroy(getActivity());
  }

  public void testError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testError(getActivity());
  }

  public void testErrorConsume() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testErrorConsume(getActivity());
  }

  public void testErrorException() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testErrorException(getActivity());
  }

  public void testErrorState() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testErrorState(getActivity());
  }

  public void testFinalizeConsume() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testFinalizeConsume(getActivity());
  }

  public void testIncrementArray() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testIncrementArray(getActivity());
  }

  public void testIncrementIterable() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testIncrementIterable(getActivity());
  }

  public void testIncrementList() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testIncrementList(getActivity());
  }

  public void testIncrementOutput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testIncrementOutput(getActivity());
  }

  public void testIncrementRemoteService() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testIncrementRemoteService(getActivity());
  }

  public void testIncrementService() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testIncrementService(getActivity());
  }

  public void testList() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testList(getActivity());
  }

  public void testSumArray() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSumArray(getActivity());
  }

  public void testSumConsume() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSumConsume(getActivity());
  }

  public void testSumDefault() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSumDefault(getActivity());
  }

  public void testSumIterable() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSumIterable(getActivity());
  }

  public void testSumOutput() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSumOutput(getActivity());
  }

  public void testSumState() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testSumState(getActivity());
  }

  private static class IncrementInvocation extends MappingInvocation<Integer, Integer> {

    private final int mIncrement;

    private IncrementInvocation(final int increment) {
      super(asArgs(increment));
      mIncrement = increment;
    }

    @Override
    public void onInput(final Integer input, @NotNull final Channel<Integer, ?> result) {
      result.pass(input + mIncrement);
    }
  }

  private static class ServiceState {

    private final Routine<Integer, Integer> mRoutine;

    private Channel<Integer, Integer> mChannel;

    private ServiceState(final Context context, final Class<? extends InvocationService> service) {
      mRoutine = JRoutineService.on(serviceFrom(context, service))
                                .with(factoryOf(IncrementInvocation.class, 1))
                                .buildRoutine();
    }

    private ServiceState(final Context context) {
      this(context, InvocationService.class);
    }

    private ServiceState abort(final RoutineException reason) {
      mChannel.abort(reason);
      return this;
    }

    private ServiceState clear() {
      mRoutine.clear();
      return this;
    }

    private ServiceState close() {
      mChannel.close();
      return this;
    }

    private ServiceState invoke() {
      mChannel = mRoutine.invoke();
      return this;
    }

    private ServiceState next(final Integer input, final Channel<Integer, ?> result) {
      final Channel<Integer, Integer> channel = mChannel;
      if (!channel.isBound()) {
        result.pass(channel);
      }

      channel.pass(input);
      return this;
    }

    private ServiceState reset() {
      mChannel = null;
      return this;
    }
  }
}
