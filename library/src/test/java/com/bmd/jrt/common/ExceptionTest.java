/**
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
package com.bmd.jrt.common;

import junit.framework.TestCase;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Exceptions unit tests.
 * <p/>
 * Created by davide on 10/2/14.
 */
public class ExceptionTest extends TestCase {

    public void testRoutineException() {

        assertThat(new RoutineException(new NullPointerException())).isExactlyInstanceOf(
                RoutineException.class);
        assertThat(new RoutineException(new NullPointerException()).getCause()).isExactlyInstanceOf(
                NullPointerException.class);
        assertThat(new RoutineException(null)).hasNoCause();
    }

    public void testRoutineInterruptedException() {

        assertThat(new RoutineInterruptedException(new InterruptedException())).isExactlyInstanceOf(
                RoutineInterruptedException.class);
        assertThat(new RoutineInterruptedException(
                new InterruptedException()).getCause()).isExactlyInstanceOf(
                InterruptedException.class);
        assertThat(new RoutineInterruptedException(null)).hasNoCause();

        try {

            RoutineInterruptedException.interrupt(new InterruptedException());

            fail();

        } catch (final RoutineInterruptedException ignored) {

        }
    }

    public void testRoutineNotAvailableException() {

        assertThat(new RoutineNotAvailableException()).hasNoCause();
    }
}