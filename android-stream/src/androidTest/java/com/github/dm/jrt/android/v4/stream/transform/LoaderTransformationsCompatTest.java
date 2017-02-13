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

package com.github.dm.jrt.android.v4.stream.transform;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.v4.stream.TestActivity;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.stream.JRoutineStream;

import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader transformations unit test.
 * <p>
 * Created by davide-maestroni on 01/31/2017.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderTransformationsCompatTest
    extends ActivityInstrumentationTestCase2<TestActivity> {

  public LoaderTransformationsCompatTest() {
    super(TestActivity.class);
  }

  public void testRunOn() {
    assertThat(JRoutineStream.withStreamOf("test1", "test2", "test3")
                             .immediate()
                             .map(Operators.filter(new Predicate<String>() {

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
                             .map(Operators.<Integer, Integer>sum(Integer.class))
                             .lift(LoaderTransformationsCompat.<String, Integer>runOn(
                                 loaderFrom(getActivity())).loaderConfiguration()
                                                           .withInvocationId(12)
                                                           .apply()
                                                           .buildFunction())
                             .call()
                             .in(seconds(10))
                             .next()).isEqualTo(10);
  }
}
