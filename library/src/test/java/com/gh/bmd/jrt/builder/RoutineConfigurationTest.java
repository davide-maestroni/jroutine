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
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withAsyncRunner;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withAvailableTimeout;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withCoreInvocations;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withFactoryArgs;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withInputOrder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withInputSize;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withInputTimeout;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withLog;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withLogLevel;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withMaxInvocations;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withOutputOrder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withOutputSize;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withOutputTimeout;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withReadTimeout;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withSyncRunner;
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

        assertThat(withFactoryArgs(3).buildConfiguration()).isEqualTo(
                builder().withFactoryArgs(3).buildConfiguration());

        final RoutineConfiguration configuration = builder().withFactoryArgs((Object[]) null)
                                                            .withAvailableTimeout(
                                                                    TimeDuration.millis(100))
                                                            .withCoreInvocations(27)
                                                            .withInputOrder(OrderType.PASSING_ORDER)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withOutputSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withFactoryArgs(3).buildConfiguration());
        assertThat(withFactoryArgs(3).buildConfiguration()).isNotEqualTo(
                withFactoryArgs(27).buildConfiguration());
    }

    @Test
    public void testAvailableTimeoutEquals() {

        assertThat(withAvailableTimeout(TimeDuration.ZERO).buildConfiguration()).isEqualTo(
                builder().withAvailableTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withAvailableTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration()).isEqualTo(
                builder().withAvailableTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withAvailableTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(configuration).isNotEqualTo(
                withAvailableTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withAvailableTimeout(TimeDuration.ZERO).buildConfiguration()).isNotEqualTo(
                withAvailableTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuildFromError() {

        try {

            builderFrom(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(builder().apply(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply(null).buildConfiguration()).isEqualTo(
                configuration);
    }

    @Test
    public void testCoreInvocationsEquals() {

        assertThat(withCoreInvocations(3).buildConfiguration()).isEqualTo(
                builder().withCoreInvocations(3).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100)).withCoreInvocations(27)
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withCoreInvocations(3).buildConfiguration());
        assertThat(withCoreInvocations(3).buildConfiguration()).isNotEqualTo(
                withCoreInvocations(27).buildConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCoreInvocationsError() {

        try {

            builder().withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputOrderEquals() {

        assertThat(withInputOrder(OrderType.PASSING_ORDER).buildConfiguration()).isEqualTo(
                builder().withInputOrder(OrderType.PASSING_ORDER).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withInputOrder(OrderType.NONE).buildConfiguration());
        assertThat(withInputOrder(OrderType.NONE).buildConfiguration()).isNotEqualTo(
                withInputOrder(OrderType.PASSING_ORDER).buildConfiguration());
    }

    @Test
    public void testInputSizeEquals() {

        assertThat(withInputSize(10).buildConfiguration()).isEqualTo(
                builder().withInputSize(10).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withInputSize(10).buildConfiguration());
        assertThat(withInputSize(10).buildConfiguration()).isNotEqualTo(
                withInputSize(31).buildConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputSizeError() {

        try {

            builder().withInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputTimeoutEquals() {

        assertThat(withInputTimeout(TimeDuration.ZERO).buildConfiguration()).isEqualTo(
                builder().withInputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withInputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration()).isEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withInputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(configuration).isNotEqualTo(
                withInputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withInputTimeout(TimeDuration.ZERO).buildConfiguration()).isNotEqualTo(
                withInputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
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

            withInputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withInputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withInputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testLogEquals() {

        assertThat(withLog(Logs.nullLog()).buildConfiguration()).isEqualTo(
                builder().withLog(Logs.nullLog()).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withLog(Logs.nullLog()).buildConfiguration());
        assertThat(withLog(Logs.nullLog()).buildConfiguration()).isNotEqualTo(
                withLog(Logs.systemLog()).buildConfiguration());
    }

    @Test
    public void testLogLevelEquals() {

        assertThat(withLogLevel(LogLevel.DEBUG).buildConfiguration()).isEqualTo(
                builder().withLogLevel(LogLevel.DEBUG).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withLogLevel(LogLevel.DEBUG).buildConfiguration());
        assertThat(withLogLevel(LogLevel.DEBUG).buildConfiguration()).isNotEqualTo(
                withLogLevel(LogLevel.WARNING).buildConfiguration());
    }

    @Test
    public void testMaxInvocationsEquals() {

        assertThat(withMaxInvocations(4).buildConfiguration()).isEqualTo(
                builder().withMaxInvocations(4).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withMaxInvocations(4).buildConfiguration());
        assertThat(withMaxInvocations(4).buildConfiguration()).isNotEqualTo(
                withMaxInvocations(41).buildConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMaxInvocationsError() {

        try {

            builder().withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputOrderEquals() {

        assertThat(withOutputOrder(OrderType.NONE).buildConfiguration()).isEqualTo(
                builder().withOutputOrder(OrderType.NONE).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withOutputOrder(OrderType.NONE).buildConfiguration());
        assertThat(withOutputOrder(OrderType.NONE).buildConfiguration()).isNotEqualTo(
                withOutputOrder(OrderType.PASSING_ORDER).buildConfiguration());
    }

    @Test
    public void testOutputSizeEquals() {

        assertThat(withOutputSize(10).buildConfiguration()).isEqualTo(
                builder().withOutputSize(10).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withOutputSize(10).buildConfiguration());
        assertThat(withOutputSize(10).buildConfiguration()).isNotEqualTo(
                withOutputSize(1).buildConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputSizeError() {

        try {

            builder().withOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputTimeoutEquals() {

        assertThat(withOutputTimeout(TimeDuration.ZERO).buildConfiguration()).isEqualTo(
                builder().withOutputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration()).isEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withOutputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(configuration).isNotEqualTo(
                withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withOutputTimeout(TimeDuration.ZERO).buildConfiguration()).isNotEqualTo(
                withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
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

            withOutputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withOutputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withOutputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testReadTimeoutEquals() {

        assertThat(withReadTimeout(TimeDuration.ZERO).buildConfiguration()).isEqualTo(
                builder().withReadTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withReadTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration()).isEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withReadTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(configuration).isNotEqualTo(
                withReadTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withReadTimeout(TimeDuration.ZERO).buildConfiguration()).isNotEqualTo(
                withReadTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
    }

    @Test
    public void testRunnerEquals() {

        assertThat(withAsyncRunner(Runners.sharedRunner()).buildConfiguration()).isEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withAsyncRunner(Runners.sharedRunner()).buildConfiguration());
        assertThat(withAsyncRunner(Runners.sharedRunner()).buildConfiguration()).isNotEqualTo(
                withAsyncRunner(Runners.queuedRunner()).buildConfiguration());
    }

    @Test
    public void testSyncRunnerEquals() {

        assertThat(withSyncRunner(Runners.queuedRunner()).buildConfiguration()).isEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withSyncRunner(Runners.queuedRunner()).buildConfiguration());
        assertThat(withSyncRunner(Runners.sequentialRunner()).buildConfiguration()).isNotEqualTo(
                withSyncRunner(Runners.queuedRunner()).buildConfiguration());
    }
}
