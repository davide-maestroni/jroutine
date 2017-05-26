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

package com.github.dm.jrt.android.v4.function;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stateless Loader routine builder unit tests.
 * <p>
 * Created by davide-maestroni on 03/07/2017.
 */
@TargetApi(VERSION_CODES.FROYO)
public class StatelessLoaderRoutineBuilderCompatTest
    extends ActivityInstrumentationTestCase2<TestActivity> {

  public StatelessLoaderRoutineBuilderCompatTest() {
    super(TestActivity.class);
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  private static void testError(final FragmentActivity activity) {
    final AtomicReference<RoutineException> reference = new AtomicReference<RoutineException>();
    final Channel<Void, Void> channel =
        JRoutineLoaderFunctionCompat.<Void, Void>statelessRoutineOn(LoaderSourceCompat.loaderOf(activity),
            0).onError(new Consumer<RoutineException>() {

          public void accept(final RoutineException e) throws Exception {
            reference.set(e);
          }
        }).create().invoke();
    assertThat(reference.get()).isNull();
    channel.abort(new IOException());
    assertThat(channel.in(seconds(10)).getComplete()).isTrue();
    assertThat(reference.get()).isExactlyInstanceOf(AbortException.class);
    assertThat(reference.get().getCause()).isExactlyInstanceOf(IOException.class);
  }

  private static void testIncrement(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunctionCompat.<Integer, Integer>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onNext(
            new BiConsumer<Integer, Channel<Integer, ?>>() {

              public void accept(final Integer integer, final Channel<Integer, ?> result) {
                result.pass(integer + 1);
              }
            }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testIncrementArray(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunctionCompat.<Integer, Integer>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onNextArray(
            new Function<Integer, Integer[]>() {

              public Integer[] apply(final Integer integer) {
                final Integer[] integers = new Integer[1];
                integers[0] = integer + 1;
                return integers;
              }
            }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testIncrementIterable(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunctionCompat.<Integer, Integer>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onNextIterable(
            new Function<Integer, Iterable<? extends Integer>>() {

              public Iterable<? extends Integer> apply(final Integer integer) {
                return Collections.singleton(integer + 1);
              }
            }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  @SuppressWarnings("unchecked")
  private static void testIncrementList(final FragmentActivity activity) {
    final ArrayList<Integer> list = new ArrayList<Integer>();
    final LoaderRoutine<Integer, List<Integer>> routine =
        JRoutineLoaderFunctionCompat.<Integer, List<Integer>>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onNextConsume(new Consumer<Integer>() {

          public void accept(final Integer integer) {
            list.add(integer + 1);
          }
        }).onCompleteOutput(new Supplier<List<Integer>>() {

          public List<Integer> get() {
            return list;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(
        Arrays.asList(2, 3, 4, 5));
  }

  private static void testIncrementOutput(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunctionCompat.<Integer, Integer>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onNextOutput(
            new Function<Integer, Integer>() {

              public Integer apply(final Integer integer) {
                return integer + 1;
              }
            }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testProduceArray(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunctionCompat.<Integer, Integer>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onCompleteArray(new Supplier<Integer[]>() {

          public Integer[] get() {
            final Integer[] integers = new Integer[1];
            integers[0] = 17;
            return integers;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(17);
  }

  private static void testProduceIterable(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunctionCompat.<Integer, Integer>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onCompleteIterable(
            new Supplier<Iterable<? extends Integer>>() {

              public Iterable<? extends Integer> get() {
                return Collections.singleton(17);
              }
            }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(17);
  }

  private static void testProduceOutput(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineLoaderFunctionCompat.<Integer, Integer>statelessRoutineOn(
            LoaderSourceCompat.loaderOf(activity), 0).onCompleteOutput(new Supplier<Integer>() {

          public Integer get() {
            return 17;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(17);
  }

  public void testError() {
    testError(getActivity());
  }

  public void testIncrement() {
    testIncrement(getActivity());
  }

  public void testIncrementArray() {
    testIncrementArray(getActivity());
  }

  public void testIncrementIterable() {
    testIncrementIterable(getActivity());
  }

  public void testIncrementList() {
    testIncrementList(getActivity());
  }

  public void testIncrementOutput() {
    testIncrementOutput(getActivity());
  }

  public void testProduceArray() {
    testProduceArray(getActivity());
  }

  public void testProduceIterable() {
    testProduceIterable(getActivity());
  }

  public void testProduceOutput() {
    testProduceOutput(getActivity());
  }
}
