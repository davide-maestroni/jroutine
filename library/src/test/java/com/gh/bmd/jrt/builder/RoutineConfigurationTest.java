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
                                                            .build();
        assertThat(configuration).isNotEqualTo(builder().withFactoryArgs(3).build());
        assertThat(configuration.builderFrom().withFactoryArgs(27).build()).isNotEqualTo(
                builder().withFactoryArgs(27).build());
    }

    @Test
    public void testAsyncRunnerEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).build());
        assertThat(configuration.builderFrom()
                                .withAsyncRunner(Runners.queuedRunner()).build()).isNotEqualTo(
                builder().withAsyncRunner(Runners.queuedRunner()).build());
    }

    @Test
    public void testAvailableTimeoutEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(
                builder().withAvailableTimeout(TimeDuration.ZERO).build());
        assertThat(configuration).isNotEqualTo(
                builder().withAvailableTimeout(1, TimeUnit.MILLISECONDS).build());
        assertThat(configuration.builderFrom().withAvailableTimeout(TimeDuration.millis(1)).build())
                .isNotEqualTo(builder().withAvailableTimeout(1, TimeUnit.MILLISECONDS).build());
    }

    @Test
    public void testBuildFrom() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();

        assertThat(builderFrom(configuration).build()).isEqualTo(configuration);
        assertThat(builderFrom(null).build()).isEqualTo(RoutineConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(builder().with(configuration).build()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().build()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).build()).isEqualTo(
                RoutineConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testCoreInvocationsEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100)).withCoreInvocations(27)
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withCoreInvocations(3).build());
        assertThat(configuration.builderFrom().withCoreInvocations(27).build()).isNotEqualTo(
                builder().withCoreInvocations(27).build());
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
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withInputOrder(OrderType.NONE).build());
        assertThat(configuration.builderFrom()
                                .withInputOrder(OrderType.PASSING_ORDER).build()).isNotEqualTo(
                builder().withInputOrder(OrderType.PASSING_ORDER).build());
    }

    @Test
    public void testInputSizeEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withInputSize(10).build());
        assertThat(configuration.builderFrom().withInputSize(31).build()).isNotEqualTo(
                builder().withInputSize(31).build());
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
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(
                builder().withInputTimeout(TimeDuration.ZERO).build());
        assertThat(configuration).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).build());
        assertThat(configuration.builderFrom()
                                .withInputTimeout(TimeDuration.millis(1)).build()).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).build());
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
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).build());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).build()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).build());
    }

    @Test
    public void testLogLevelEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(LogLevel.DEBUG).build());
        assertThat(configuration.builderFrom().withLogLevel(LogLevel.WARNING).build()).isNotEqualTo(
                builder().withLogLevel(LogLevel.WARNING).build());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withMaxInvocations(4).build());
        assertThat(configuration.builderFrom().withMaxInvocations(41).build()).isNotEqualTo(
                builder().withMaxInvocations(41).build());
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
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withOutputOrder(OrderType.NONE).build());
        assertThat(configuration.builderFrom()
                                .withOutputOrder(OrderType.PASSING_ORDER).build()).isNotEqualTo(
                builder().withOutputOrder(OrderType.PASSING_ORDER).build());
    }

    @Test
    public void testOutputSizeEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(builder().withOutputSize(10).build());
        assertThat(configuration.builderFrom().withOutputSize(1).build()).isNotEqualTo(
                builder().withOutputSize(1).build());
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
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(TimeDuration.ZERO).build());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).build());
        assertThat(configuration.builderFrom()
                                .withOutputTimeout(TimeDuration.millis(1)).build()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).build());
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
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(TimeDuration.ZERO).build());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).build());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(TimeDuration.millis(1)).build()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).build());
    }

    @Test
    public void testSyncRunnerEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog()).withOutputSize(100).build();
        assertThat(configuration).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).build());
        assertThat(configuration.builderFrom()
                                .withSyncRunner(Runners.queuedRunner()).build()).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).build());
    }
}
