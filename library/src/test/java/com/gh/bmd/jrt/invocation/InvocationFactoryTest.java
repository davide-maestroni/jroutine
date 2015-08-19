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
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.core.JRoutine;
import com.gh.bmd.jrt.routine.Routine;

import org.junit.Test;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invocation factories unit test.
 * <p/>
 * Created by davide-maestroni on 19/08/15.
 */
public class InvocationFactoryTest {

    @Test
    public void testDecoratingInvocationFactory() {

        final InvocationFactory<String, String> factory = PassingInvocation.factoryOf();
        assertThat(factory.newInvocation()).isExactlyInstanceOf(PassingInvocation.class);
        final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
        assertThat(decoratedFactory.newInvocation()).isExactlyInstanceOf(
                TestInvocationDecorator.class);
    }

    @Test
    public void testInvocationDecoratorAbort() {

        final InvocationFactory<String, String> factory = PassingInvocation.factoryOf();
        final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
        final Routine<String, String> routine = JRoutine.on(decoratedFactory).buildRoutine();
        assertThat(routine.asyncInvoke().after(millis(100)).pass("test").result().abort()).isTrue();
        routine.purge();
    }

    @Test
    public void testInvocationDecoratorLifecycle() {

        final InvocationFactory<String, String> factory = PassingInvocation.factoryOf();
        final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
        final Routine<String, String> routine = JRoutine.on(decoratedFactory).buildRoutine();
        assertThat(routine.asyncCall("test").eventually().all()).containsExactly("test");
        routine.purge();
    }

    private static class TestInvocationDecorator extends InvocationDecorator<String, String> {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped invocation instance.
         */
        public TestInvocationDecorator(@Nonnull final Invocation<String, String> wrapped) {

            super(wrapped);
        }
    }

    private static class TestInvocationFactory extends DecoratingInvocationFactory<String, String> {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped factory instance.
         */
        public TestInvocationFactory(@Nonnull final InvocationFactory<String, String> wrapped) {

            super(wrapped);
        }

        @Nonnull
        @Override
        protected Invocation<String, String> decorate(
                @Nonnull final Invocation<String, String> invocation) {

            return new TestInvocationDecorator(invocation);
        }
    }
}
