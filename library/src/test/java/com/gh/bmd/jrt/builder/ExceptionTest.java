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
package com.gh.bmd.jrt.builder;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exception unit tests.
 * <p/>
 * Created by davide on 3/26/15.
 */
public class ExceptionTest {

    @Test
    public void testInputDeadlockException() {

        assertThat(new InputDeadlockException("")).hasNoCause();
    }

    @Test
    public void testOutputDeadlockException() {

        assertThat(new OutputDeadlockException("")).hasNoCause();
    }
}
