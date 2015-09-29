/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.channel;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exception unit tests.
 * <p/>
 * Created by davide-maestroni on 03/26/2015.
 */
public class ExceptionTest {

    @Test
    public void testAbortException() {

        assertThat(new AbortException(new NullPointerException()).getCause()).isExactlyInstanceOf(
                NullPointerException.class);
        assertThat(new AbortException(null)).hasNoCause();
        assertThat(AbortException.wrapIfNeeded(new NullPointerException())).isExactlyInstanceOf(
                AbortException.class);
        assertThat(AbortException.wrapIfNeeded(new RoutineException())).isExactlyInstanceOf(
                RoutineException.class);
    }

    @Test
    public void testChannelTimeoutException() {

        assertThat(new ChannelTimeoutException("")).hasNoCause();
    }

    @Test
    public void testDeadlockException() {

        assertThat(new DeadlockException("")).hasNoCause();
    }

    @Test
    public void testExceptions() {

        assertThat(new ExecutionTimeoutException("")).hasNoCause();
    }

    @Test
    public void testExecutionDeadlockException() {

        assertThat(new ExecutionDeadlockException().getMessage()).isNotEmpty();
        assertThat(new ExecutionDeadlockException("")).hasNoCause();
    }

    @Test
    public void testExecutionTimeoutException() {

        assertThat(new ExecutionTimeoutException("")).hasNoCause();
    }

    @Test
    public void testInputDeadlockException() {

        assertThat(new InputDeadlockException("")).hasNoCause();
    }

    @Test
    public void testInputTimeoutException() {

        assertThat(new InputTimeoutException("")).hasNoCause();
    }

    @Test
    public void testOutputDeadlockException() {

        assertThat(new OutputDeadlockException("")).hasNoCause();
    }

    @Test
    public void testOutputTimeoutException() {

        assertThat(new OutputTimeoutException("")).hasNoCause();
    }

    @Test
    public void testRoutineException() {

        assertThat(new RoutineException()).hasNoCause();
    }

    @Test
    public void testStreamingDeadlockException() {

        assertThat(new StreamingDeadlockException().getMessage()).isNotEmpty();
        assertThat(new StreamingDeadlockException("")).hasNoCause();
    }

    @Test
    public void testTimeoutException() {

        assertThat(new TimeoutException("")).hasNoCause();
    }
}
