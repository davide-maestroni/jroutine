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

package com.github.dm.jrt.android.v4.rx;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.android.rx.R;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests.
 * <p>
 * Created by davide-maestroni on 12/02/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class RoutineRxCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public RoutineRxCompatTest() {
    super(TestActivity.class);
  }

  public void testActivity() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(3);
    final List<String> expected = Arrays.asList("TEST1", "TEST2", "TEST3");
    final AtomicBoolean isSuccess = new AtomicBoolean(true);
    JRoutineRxCompat.withObservable(Observable.just("test1", "test2", "test3"))
                    .applyInvocationConfiguration()
                    .withLog(AndroidLogs.androidLog())
                    .configured()
                    .applyLoaderConfiguration()
                    .withResultStaleTime(seconds(10))
                    .configured()
                    .subscribeOn(loaderFrom(getActivity()))
                    .map(new Func1<String, String>() {

                      @Override
                      public String call(final String s) {
                        return s.toUpperCase();
                      }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {

                      @Override
                      public void call(final String s) {
                        if (!expected.contains(s)) {
                          isSuccess.set(false);
                        }

                        latch.countDown();
                      }
                    });
    latch.await(10, TimeUnit.SECONDS);
    assertThat(isSuccess.get()).isTrue();
  }

  public void testFragment() throws InterruptedException {
    final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                              .findFragmentById(R.id.test_fragment);
    final CountDownLatch latch = new CountDownLatch(3);
    final List<String> expected = Arrays.asList("TEST1", "TEST2", "TEST3");
    final AtomicBoolean isSuccess = new AtomicBoolean(true);
    JRoutineRxCompat.withObservable(Observable.just("test1", "test2", "test3"))
                    .subscribeOn(loaderFrom(fragment))
                    .map(new Func1<String, String>() {

                      @Override
                      public String call(final String s) {
                        return s.toUpperCase();
                      }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {

                      @Override
                      public void call(final String s) {
                        if (!expected.contains(s)) {
                          isSuccess.set(false);
                        }

                        latch.countDown();
                      }
                    });
    latch.await(10, TimeUnit.SECONDS);
    assertThat(isSuccess.get()).isTrue();
  }
}
