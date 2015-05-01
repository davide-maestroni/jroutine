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

import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logs;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine configuration unit tests.
 * <p/>
 * Created by davide on 11/22/14.
 */
public class RoutineConfigurationTest {

    @Test
    public void testArgsEquals() {

        final RoutineConfiguration configuration = builder().withFactoryArgs((Object[]) null)
                                                            .withAvailableInvocationTimeout(
                                                                    TimeDuration.millis(100))
                                                            .withCoreInvocations(27)
                                                            .withInputOrder(OrderType.PASSING_ORDER)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withOutputMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(builder().withFactoryArgs(3).set());
        assertThat(configuration.builderFrom().withFactoryArgs(27).set()).isNotEqualTo(
                builder().withFactoryArgs(27).set());
    }

    @Test
    public void testAsyncRunnerEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).set());
        assertThat(configuration.builderFrom()
                                .withAsyncRunner(Runners.queuedRunner())
                                .set()).isNotEqualTo(
                builder().withAsyncRunner(Runners.queuedRunner()).set());
    }

    @Test
    public void testAvailableTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withAvailableInvocationTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withAvailableInvocationTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withAvailableInvocationTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withAvailableInvocationTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    public void testBuildFrom() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();

        assertThat(builderFrom(configuration).set()).isEqualTo(configuration);
        assertThat(builderFrom(null).set()).isEqualTo(RoutineConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                RoutineConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testCoreInvocationsEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withCoreInvocations(27)
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withCoreInvocations(3).set());
        assertThat(configuration.builderFrom().withCoreInvocations(27).set()).isNotEqualTo(
                builder().withCoreInvocations(27).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCoreInvocationsError() {

        try {

            builder().withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputOrderEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withInputOrder(OrderType.NONE).set());
        assertThat(configuration.builderFrom()
                                .withInputOrder(OrderType.PASSING_ORDER)
                                .set()).isNotEqualTo(
                builder().withInputOrder(OrderType.PASSING_ORDER).set());
    }

    @Test
    public void testInputSizeEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withInputMaxSize(10).set());
        assertThat(configuration.builderFrom().withInputMaxSize(31).set()).isNotEqualTo(
                builder().withInputMaxSize(31).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputSizeError() {

        try {

            builder().withInputMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withInputTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withInputTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputTimeoutError() {

        try {

            builder().withInputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withInputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testLogEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).set());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).set()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).set());
    }

    @Test
    public void testLogLevelEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(LogLevel.DEBUG).set());
        assertThat(configuration.builderFrom().withLogLevel(LogLevel.WARNING).set()).isNotEqualTo(
                builder().withLogLevel(LogLevel.WARNING).set());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withMaxInvocations(4).set());
        assertThat(configuration.builderFrom().withMaxInvocations(41).set()).isNotEqualTo(
                builder().withMaxInvocations(41).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMaxInvocationsError() {

        try {

            builder().withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputOrderEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withOutputOrder(OrderType.NONE).set());
        assertThat(configuration.builderFrom()
                                .withOutputOrder(OrderType.PASSING_ORDER)
                                .set()).isNotEqualTo(
                builder().withOutputOrder(OrderType.PASSING_ORDER).set());
    }

    @Test
    public void testOutputSizeEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withOutputMaxSize(10).set());
        assertThat(configuration.builderFrom().withOutputMaxSize(1).set()).isNotEqualTo(
                builder().withOutputMaxSize(1).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputSizeError() {

        try {

            builder().withOutputMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withOutputTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputTimeoutError() {

        try {

            builder().withOutputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withOutputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testReadTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withReadTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    public void testSyncRunnerEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableInvocationTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).set());
        assertThat(configuration.builderFrom()
                                .withSyncRunner(Runners.queuedRunner())
                                .set()).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).set());
    }
}
