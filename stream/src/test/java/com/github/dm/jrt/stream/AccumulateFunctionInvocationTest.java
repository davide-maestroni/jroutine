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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Supplier;

import org.junit.Test;

import static com.github.dm.jrt.function.Functions.first;
import static com.github.dm.jrt.stream.AccumulateFunctionInvocation.functionFactory;
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
        assertThat(JRoutineCore.on(functionFactory(function))
                               .sync("test1", "test2", "test3")
                               .next()).isEqualTo("test1test2test3");
        assertThat(JRoutineCore.on(functionFactory(new Supplier<String>() {

            public String get() {

                return "test0";
            }
        }, function)).sync("test1", "test2", "test3").next()).isEqualTo("test0test1test2test3");
    }

    @Test
    public void testFactoryEquals() {

        final BiFunction<String, String, String> function = createFunction();
        final InvocationFactory<String, String> factory = functionFactory(function);
        final BiFunction<String, String, String> first = first();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(functionFactory(function));
        assertThat(functionFactory(function)).isEqualTo(functionFactory(function));
        assertThat(functionFactory(first)).isEqualTo(functionFactory(first));
        assertThat(factory).isNotEqualTo(functionFactory(first));
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(functionFactory(function).hashCode());
        assertThat(factory.hashCode()).isNotEqualTo(functionFactory(first).hashCode());
    }
}
