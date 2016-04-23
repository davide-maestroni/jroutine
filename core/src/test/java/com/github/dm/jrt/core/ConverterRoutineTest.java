/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.runner.Runners;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Converter routine unit test.
 * <p>
 * Created by davide-maestroni on 04/17/2016.
 */
public class ConverterRoutineTest {

    @Test
    public void testConversion() {

        final TestRoutine routine = new TestRoutine();
        routine.asyncCall();
        assertThat(AsyncInvocation.sCount).isEqualTo(1);
        assertThat(SyncInvocation.sCount).isEqualTo(0);
        routine.syncCall();
        assertThat(AsyncInvocation.sCount).isEqualTo(0);
        assertThat(SyncInvocation.sCount).isEqualTo(1);
    }

    private static class AsyncInvocation extends TemplateInvocation<Object, Object> {

        private static int sCount;

        private AsyncInvocation() {

            ++sCount;
        }

        @Override
        public void onDestroy() throws Exception {

            --sCount;
            throw new IllegalStateException();
        }
    }

    private static class SyncInvocation extends TemplateInvocation<Object, Object> {

        private static int sCount;

        private SyncInvocation() {

            ++sCount;
        }

        @Override
        public void onDestroy() throws Exception {

            --sCount;
        }
    }

    private static class TestRoutine extends ConverterRoutine<Object, Object> {

        /**
         * Constructor.
         */
        protected TestRoutine() {

            super(InvocationConfiguration.builder()
                                         .withMaxInstances(1)
                                         .withRunner(Runners.syncRunner())
                                         .applyConfiguration());
        }

        @NotNull
        @Override
        protected Invocation<Object, Object> newInvocation(
                @NotNull final InvocationType type) throws Exception {

            if (type == InvocationType.SYNC) {
                return new SyncInvocation();
            }

            return new AsyncInvocation();
        }
    }
}
