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

import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.core.JRoutine;
import com.gh.bmd.jrt.invocation.Invocations.Function;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocations unit tests.
 * <p/>
 * Created by davide-maestroni on 2/16/15.
 */
public class InvocationsTest {

    @Test
    public void testFunction() {

        final Routine<Object, String> routine =
                JRoutine.on(Invocations.factoryOf(new Function<String>() {

                    public String call(@Nonnull final Object... params) {

                        final StringBuilder builder = new StringBuilder(String.valueOf(params[0]));

                        for (int i = 1; i < params.length; i++) {

                            builder.append(", ").append(params[i]);
                        }

                        return builder.toString();
                    }
                }))
                        .withInvocation()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withReadTimeout(TimeDuration.seconds(1))
                        .set()
                        .buildRoutine();
        assertThat(routine.callAsync("test1", "test2", "test3", "test4").readNext()).isEqualTo(
                "test1, test2, test3, test4");
        assertThat(routine.callParallel("test1", "test2", "test3", "test4").readAll()).containsOnly(
                "test1", "test2", "test3", "test4");
    }

    @Test
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testInvocationFactory() {

        final InvocationFactory<Object, Object> factory =
                Invocations.factoryOf(TestInvocation.class);

        assertThat(factory.newInvocation()).isExactlyInstanceOf(TestInvocation.class);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullClassError() {

        try {

            Invocations.factoryOf((Class<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunctionError() {

        try {

            Invocations.factoryOf((Function<Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullTokenError() {

        try {

            Invocations.factoryOf((ClassToken<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TestInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

        }
    }
}
