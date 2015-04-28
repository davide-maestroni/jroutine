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
                                                            .withAvailableTimeout(
                                                                    TimeDuration.millis(100))
                                                            .withCoreInvocations(27)
                                                            .withInputOrder(OrderType.PASSING_ORDER)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withOutputSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withFactoryArgs(3).applied());
        assertThat(configuration.builderFrom().withFactoryArgs(27).applied()).isNotEqualTo(
                builder().withFactoryArgs(27).applied());
    }

    @Test
    public void testAsyncRunnerEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).applied());
        assertThat(configuration.builderFrom()
                                .withAsyncRunner(Runners.queuedRunner())
                                .applied()).isNotEqualTo(
                builder().withAsyncRunner(Runners.queuedRunner()).applied());
    }

    @Test
    public void testAvailableTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withAvailableTimeout(TimeDuration.ZERO).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withAvailableTimeout(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom()
                                .withAvailableTimeout(TimeDuration.millis(1))
                                .applied()).isNotEqualTo(
                builder().withAvailableTimeout(1, TimeUnit.MILLISECONDS).applied());
    }

    @Test
    public void testBuildFrom() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();

        assertThat(builderFrom(configuration).applied()).isEqualTo(configuration);
        assertThat(builderFrom(null).applied()).isEqualTo(
                RoutineConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(builder().with(configuration).applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).applied()).isEqualTo(
                RoutineConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testCoreInvocationsEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withCoreInvocations(27)
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withCoreInvocations(3).applied());
        assertThat(configuration.builderFrom().withCoreInvocations(27).applied()).isNotEqualTo(
                builder().withCoreInvocations(27).applied());
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
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withInputOrder(OrderType.NONE).applied());
        assertThat(configuration.builderFrom()
                                .withInputOrder(OrderType.PASSING_ORDER)
                                .applied()).isNotEqualTo(
                builder().withInputOrder(OrderType.PASSING_ORDER).applied());
    }

    @Test
    public void testInputSizeEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withInputSize(10).applied());
        assertThat(configuration.builderFrom().withInputSize(31).applied()).isNotEqualTo(
                builder().withInputSize(31).applied());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputSizeError() {

        try {

            builder().withInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withInputTimeout(TimeDuration.ZERO).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom()
                                .withInputTimeout(TimeDuration.millis(1))
                                .applied()).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).applied());
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
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).applied());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).applied()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).applied());
    }

    @Test
    public void testLogLevelEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(LogLevel.DEBUG).applied());
        assertThat(
                configuration.builderFrom().withLogLevel(LogLevel.WARNING).applied()).isNotEqualTo(
                builder().withLogLevel(LogLevel.WARNING).applied());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withMaxInvocations(4).applied());
        assertThat(configuration.builderFrom().withMaxInvocations(41).applied()).isNotEqualTo(
                builder().withMaxInvocations(41).applied());
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
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withOutputOrder(OrderType.NONE).applied());
        assertThat(configuration.builderFrom()
                                .withOutputOrder(OrderType.PASSING_ORDER)
                                .applied()).isNotEqualTo(
                builder().withOutputOrder(OrderType.PASSING_ORDER).applied());
    }

    @Test
    public void testOutputSizeEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(builder().withOutputSize(10).applied());
        assertThat(configuration.builderFrom().withOutputSize(1).applied()).isNotEqualTo(
                builder().withOutputSize(1).applied());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputSizeError() {

        try {

            builder().withOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(TimeDuration.ZERO).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom()
                                .withOutputTimeout(TimeDuration.millis(1))
                                .applied()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).applied());
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
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(TimeDuration.ZERO).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(TimeDuration.millis(1))
                                .applied()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).applied());
    }

    @Test
    public void testSyncRunnerEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).applied());
        assertThat(configuration.builderFrom()
                                .withSyncRunner(Runners.queuedRunner())
                                .applied()).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).applied());
    }
}
