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

package com.github.dm.jrt.android.function;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.convertFactory;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android function unit tests.
 * <p>
 * Created by davide-maestroni on 05/29/2017.
 */
@TargetApi(VERSION_CODES.FROYO)
public class JRoutineAndroidFunctionTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public JRoutineAndroidFunctionTest() {
    super(TestActivity.class);
  }

  @NotNull
  private static ContextInvocationFactory<Object, String> createFactory() {
    return JRoutineAndroidFunction.contextFactoryOf(
        new Supplier<ContextInvocation<Object, String>>() {

          public ContextInvocation<Object, String> get() {
            return new TemplateContextInvocation<Object, String>() {

              public void onInput(final Object input, @NotNull final Channel<String, ?> result) {
                result.pass(input.toString());
              }
            };
          }
        });
  }

  public void testFactory() {
    final Routine<Object, String> routine = JRoutineCore.routine()
                                                        .of(convertFactory(
                                                            getActivity().getApplicationContext(),
                                                            createFactory()));
    assertThat(routine.invoke().pass("test", 1).close().in(seconds(1)).all()).containsOnly("test",
        "1");
  }

  public void testFactoryEquals() {
    final Supplier<TemplateContextInvocation<Object, Object>> supplier = SupplierDecorator.constant(
        (TemplateContextInvocation<Object, Object>) new TemplateContextInvocation<Object, Object>
            () {

          public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
            result.pass(input);
          }
        });
    final ContextInvocationFactory<Object, String> factory = createFactory();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(createFactory());
    assertThat(factory).isNotEqualTo(JRoutineAndroidFunction.contextFactoryOf(supplier));
    assertThat(factory).isNotEqualTo(new TemplateContextInvocation<Object, Object>() {

      public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
        result.pass(input);
      }
    });
    assertThat(factory).isNotEqualTo("");
    assertThat(JRoutineAndroidFunction.contextFactoryOf(supplier)).isEqualTo(
        JRoutineAndroidFunction.contextFactoryOf(supplier));
    assertThat(JRoutineAndroidFunction.contextFactoryOf(supplier).hashCode()).isEqualTo(
        JRoutineAndroidFunction.contextFactoryOf(supplier).hashCode());
  }

  @SuppressWarnings("ConstantConditions")
  public void testFactoryError() {
    try {
      JRoutineAndroidFunction.contextFactoryOf(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineAndroidFunction.contextFactoryOf(new Supplier<ContextInvocation<Object, Object>>() {

        @Override
        public ContextInvocation<Object, Object> get() throws Exception {
          return new TemplateContextInvocation<Object, Object>() {

            public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
              result.pass(input);
            }
          };
        }
      });
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }
}
