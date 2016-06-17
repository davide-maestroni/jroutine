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

import com.github.dm.jrt.core.runner.Execution;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sequential runner unit tests.
 * <p>
 * Created by davide-maestroni on 06/11/2016.
 */
public class ImmediateRunnerTest {

    @Test
    public void testRun() {

        final TestExecution testExecution = new TestExecution();
        final ImmediateRunner runner = new ImmediateRunner();
        runner.run(testExecution, 0, TimeUnit.MILLISECONDS);
        assertThat(testExecution.mIsRun).isTrue();
        testExecution.mIsRun = false;
        final long start = System.currentTimeMillis();
        runner.run(testExecution, 1, TimeUnit.SECONDS);
        assertThat(testExecution.mIsRun).isTrue();
        assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(start + 1000);
    }

    private static class TestExecution implements Execution {

        private boolean mIsRun;

        public void run() {

            mIsRun = true;
        }
    }
}
