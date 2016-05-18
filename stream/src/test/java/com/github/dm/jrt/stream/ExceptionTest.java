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

import com.github.dm.jrt.core.error.RoutineException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Exceptions unit tests.
 * <p>
 * Created by davide-maestroni on 05/10/2016.
 */
public class ExceptionTest {

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testStreamException() {

        assertThat(StreamException.wrap(new NullPointerException()).getCause()).isExactlyInstanceOf(
                NullPointerException.class);
        assertThat(StreamException.wrap(null)).hasNoCause();
        assertThat(StreamException.wrap(new NullPointerException())).isExactlyInstanceOf(
                StreamException.class);
        assertThat(StreamException.wrap(new RoutineException())).isExactlyInstanceOf(
                StreamException.class);
        assertThat(StreamException.wrap(StreamException.wrap(null))).isExactlyInstanceOf(
                StreamException.class);
        assertThat(StreamException.wrap(StreamException.wrap(null))).hasNoCause();
    }
}
