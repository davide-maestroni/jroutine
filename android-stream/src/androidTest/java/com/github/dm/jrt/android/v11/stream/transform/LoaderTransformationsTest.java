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

package com.github.dm.jrt.android.v11.stream.transform;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.v11.stream.TestActivity;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.operator.JRoutineOperators;
import com.github.dm.jrt.stream.JRoutineStream;

import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;

/**
 * Loader transformations unit test.
 * <p>
 * Created by davide-maestroni on 01/31/2017.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderTransformationsTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public LoaderTransformationsTest() {
    super(TestActivity.class);
  }

  public void testRunOn() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineStream.withStreamOf("test1", "test2", "test3")
                             .immediate()
                             .map(JRoutineOperators.filter(new Predicate<String>() {

                               @Override
                               public boolean test(final String s) {
                                 return !"test2".equals(s);
                               }
                             }))
                             .map(new Function<String, Integer>() {

                               @Override
                               public Integer apply(final String s) {
                                 return s.length();
                               }
                             })
                             .map(JRoutineOperators.<Integer, Integer>sum(Integer.class))
                             .lift(LoaderTransformations.<String, Integer>runOn(
                                 loaderFrom(getActivity())).loaderConfiguration()
                                                           .withInvocationId(12)
                                                           .apply()
                                                           .buildFunction())
                             .invoke()
                             .close()
                             .in(seconds(10))
                             .next()).isEqualTo(10);
  }
}
