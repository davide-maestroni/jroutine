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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.gh.bmd.jrt.invocation.Invocations.factoryOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine builder unit tests.
 * <p/>
 * Created by davide on 10/16/14.
 */
public class JRoutineTest {

    @Test
    public void testRoutineBuilder() {

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .withRoutine()
                           .withSyncRunner(Runners.sequentialRunner())
                           .withAsyncRunner(Runners.poolRunner())
                           .withCoreInvocations(0)
                           .withMaxInvocations(1)
                           .withAvailInvocationTimeout(1, TimeUnit.SECONDS)
                           .withInputMaxSize(2)
                           .withInputTimeout(1, TimeUnit.SECONDS)
                           .withOutputMaxSize(2)
                           .withOutputTimeout(1, TimeUnit.SECONDS)
                           .withOutputOrder(OrderType.PASS_ORDER)
                           .set()
                           .callSync("test1", "test2")
                           .readAll()).containsExactly("test1", "test2");

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .withRoutine()
                           .withSyncRunner(Runners.queuedRunner())
                           .withAsyncRunner(Runners.poolRunner())
                           .withCoreInvocations(0)
                           .withMaxInvocations(1)
                           .withAvailInvocationTimeout(TimeDuration.ZERO)
                           .withInputMaxSize(2)
                           .withInputTimeout(TimeDuration.ZERO)
                           .withOutputMaxSize(2)
                           .withOutputTimeout(TimeDuration.ZERO)
                           .withOutputOrder(OrderType.PASS_ORDER)
                           .set()
                           .callSync("test1", "test2")
                           .readAll()).containsExactly("test1", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        try {

            new DefaultRoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }
}
