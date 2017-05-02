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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.BiFunctionDecorator;
import com.github.dm.jrt.function.util.Supplier;

import org.junit.Test;

import static com.github.dm.jrt.operator.AccumulateFunctionInvocation.factoryOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accumulate invocation unit tests.
 * <p>
 * Created by davide-maestroni on 10/27/2015.
 */
public class AccumulateFunctionInvocationTest {

  private static BiFunction<String, String, String> createFunction() {

    return new BiFunction<String, String, String>() {

      public String apply(final String s1, final String s2) {

        return s1 + s2;
      }
    };
  }

  @Test
  public void testFactory() {

    final BiFunction<String, String, String> function = createFunction();
    assertThat(JRoutineCore.routineOn(ScheduledExecutors.syncExecutor())
                           .of(AccumulateFunctionInvocation.factoryOf(function))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .next()).isEqualTo("test1test2test3");
    assertThat(JRoutineCore.routineOn(ScheduledExecutors.syncExecutor())
                           .of(factoryOf(new Supplier<String>() {

                             public String get() {
                               return "test0";
                             }
                           }, function))
                           .invoke()
                           .pass("test1", "test2", "test3")
                           .close()
                           .next()).isEqualTo("test0test1test2test3");
  }

  @Test
  public void testFactoryEquals() {

    final BiFunction<String, String, String> function = createFunction();
    final InvocationFactory<String, String> factory = AccumulateFunctionInvocation.factoryOf(function);
    final BiFunction<String, String, String> first = BiFunctionDecorator.first();
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isEqualTo(AccumulateFunctionInvocation.factoryOf(function));
    assertThat(AccumulateFunctionInvocation.factoryOf(function)).isEqualTo(
        AccumulateFunctionInvocation.factoryOf(function));
    assertThat(AccumulateFunctionInvocation.factoryOf(first)).isEqualTo(
        AccumulateFunctionInvocation.factoryOf(first));
    assertThat(factory).isNotEqualTo(AccumulateFunctionInvocation.factoryOf(first));
    assertThat(factory).isNotEqualTo("");
    assertThat(factory.hashCode()).isEqualTo(AccumulateFunctionInvocation.factoryOf(function).hashCode());
    assertThat(factory.hashCode()).isNotEqualTo(AccumulateFunctionInvocation.factoryOf(first).hashCode());
  }
}
