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
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Supplier;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.dm.jrt.function.Functions.biSink;
import static com.github.dm.jrt.stream.AccumulateConsumerInvocation.consumerFactory;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accumulate invocation unit tests.
 * <p>
 * Created by davide-maestroni on 04/19/2016.
 */
public class AccumulateConsumerInvocationTest {

    private static BiConsumer<List<String>, List<String>> createConsumer() {

        return new BiConsumer<List<String>, List<String>>() {

            public void accept(final List<String> strings1, final List<String> strings2) {

                strings1.addAll(strings2);
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFactory() {

        final BiConsumer<List<String>, List<String>> consumer = createConsumer();
        assertThat(JRoutineCore.with(consumerFactory(consumer)).syncCall(new ArrayList<String>() {{
            add("test1");
        }}, new ArrayList<String>() {{
            add("test2");
        }}, new ArrayList<String>() {{
            add("test3");
        }}).next()).isEqualTo(Arrays.asList("test1", "test2", "test3"));
        assertThat(JRoutineCore.with(consumerFactory(new Supplier<List<String>>() {

            public List<String> get() {

                return new ArrayList<String>() {{
                    add("test0");
                }};
            }
        }, consumer)).syncCall(new ArrayList<String>() {{
            add("test1");
        }}, new ArrayList<String>() {{
            add("test2");
        }}, new ArrayList<String>() {{
            add("test3");
        }}).next()).isEqualTo(Arrays.asList("test0", "test1", "test2", "test3"));
    }

    @Test
    public void testFactoryEquals() {

        final BiConsumer<List<String>, List<String>> consumer = createConsumer();
        final InvocationFactory<List<String>, List<String>> factory = consumerFactory(consumer);
        final BiConsumer<List<String>, List<String>> biSink = biSink();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(consumerFactory(consumer));
        assertThat(consumerFactory(consumer)).isEqualTo(consumerFactory(consumer));
        assertThat(consumerFactory(biSink)).isEqualTo(consumerFactory(biSink));
        assertThat(factory).isNotEqualTo(consumerFactory(biSink));
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(consumerFactory(consumer).hashCode());
        assertThat(factory.hashCode()).isNotEqualTo(consumerFactory(biSink).hashCode());
    }
}
