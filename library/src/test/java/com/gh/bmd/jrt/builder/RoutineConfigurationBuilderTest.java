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
 * Configuration builder unit tests.
 * <p/>
 * Created by davide on 11/22/14.
 */
public class RoutineConfigurationBuilderTest {

    @Test
    public void tesAvailableTimeoutEquals() {

        assertThat(withAvailableTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withAvailableTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withAvailableTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withAvailableTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withAvailableTimeout(TimeDuration.ZERO));
        assertThat(configuration).isNotEqualTo(withAvailableTimeout(1, TimeUnit.MILLISECONDS));
        assertThat(withAvailableTimeout(TimeDuration.ZERO)).isNotEqualTo(
                withAvailableTimeout(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void tesCoreInvocationsEquals() {

        assertThat(withCoreInvocations(3)).isEqualTo(
                builder().withCoreInvocations(3).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withCoreInvocations(27)
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withCoreInvocations(3));
        assertThat(withCoreInvocations(3)).isNotEqualTo(withCoreInvocations(27));
    }

    @Test
    public void tesInputOrderEquals() {

        assertThat(withInputOrder(OrderType.PASSING_ORDER)).isEqualTo(
                builder().withInputOrder(OrderType.PASSING_ORDER).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withInputOrder(OrderType.NONE));
        assertThat(withInputOrder(OrderType.NONE)).isNotEqualTo(
                withInputOrder(OrderType.PASSING_ORDER));
    }

    @Test
    public void tesInputSizeEquals() {

        assertThat(withInputSize(10)).isEqualTo(builder().withInputSize(10).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withInputSize(10));
        assertThat(withInputSize(10)).isNotEqualTo(withInputSize(31));
    }

    @Test
    public void tesInputTimeoutEquals() {

        assertThat(withInputTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withInputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withInputTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withInputTimeout(TimeDuration.ZERO));
        assertThat(configuration).isNotEqualTo(withInputTimeout(1, TimeUnit.MILLISECONDS));
        assertThat(withInputTimeout(TimeDuration.ZERO)).isNotEqualTo(
                withInputTimeout(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void tesLogEquals() {

        assertThat(withLog(Logs.nullLog())).isEqualTo(
                builder().withLog(Logs.nullLog()).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withLog(Logs.nullLog()));
        assertThat(withLog(Logs.nullLog())).isNotEqualTo(withLog(Logs.systemLog()));
    }

    @Test
    public void tesLogLevelEquals() {

        assertThat(withLogLevel(LogLevel.DEBUG)).isEqualTo(
                builder().withLogLevel(LogLevel.DEBUG).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withLogLevel(LogLevel.DEBUG));
        assertThat(withLogLevel(LogLevel.DEBUG)).isNotEqualTo(withLogLevel(LogLevel.WARNING));
    }

    @Test
    public void tesMaxInvocationsEquals() {

        assertThat(withMaxInvocations(4)).isEqualTo(
                builder().withMaxInvocations(4).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withMaxInvocations(4));
        assertThat(withMaxInvocations(4)).isNotEqualTo(withMaxInvocations(41));
    }

    @Test
    public void tesOutputOrderEquals() {

        assertThat(withOutputOrder(OrderType.NONE)).isEqualTo(
                builder().withOutputOrder(OrderType.NONE).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withOutputOrder(OrderType.NONE));
        assertThat(withOutputOrder(OrderType.NONE)).isNotEqualTo(
                withOutputOrder(OrderType.PASSING_ORDER));
    }

    @Test
    public void tesOutputSizeEquals() {

        assertThat(withOutputSize(10)).isEqualTo(builder().withOutputSize(10).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withOutputSize(10));
        assertThat(withOutputSize(10)).isNotEqualTo(withOutputSize(1));
    }

    @Test
    public void tesOutputTimeoutEquals() {

        assertThat(withOutputTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withOutputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withOutputTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withOutputTimeout(TimeDuration.ZERO));
        assertThat(configuration).isNotEqualTo(withOutputTimeout(1, TimeUnit.MILLISECONDS));
        assertThat(withOutputTimeout(TimeDuration.ZERO)).isNotEqualTo(
                withOutputTimeout(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void tesReadTimeoutEquals() {

        assertThat(withReadTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withReadTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withReadTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withReadTimeout(TimeDuration.ZERO));
        assertThat(configuration).isNotEqualTo(withReadTimeout(1, TimeUnit.MILLISECONDS));
        assertThat(withReadTimeout(TimeDuration.ZERO)).isNotEqualTo(
                withReadTimeout(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void tesRunnerEquals() {

        assertThat(withAsyncRunner(Runners.sharedRunner())).isEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withAsyncRunner(Runners.sharedRunner()));
        assertThat(Runners.sharedRunner()).isNotEqualTo(withAsyncRunner(Runners.queuedRunner()));
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
        assertThat(builder().withConfiguration(configuration).buildConfiguration()).isEqualTo(
                configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationError() {

        try {

            builder().withConfiguration(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
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
    public void testSyncRunnerEquals() {

        assertThat(withSyncRunner(Runners.queuedRunner())).isEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).buildConfiguration());

        final RoutineConfiguration configuration =
                builder().withAvailableTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.PASSING_ORDER)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputSize(100)
                         .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withSyncRunner(Runners.queuedRunner()));
        assertThat(withSyncRunner(Runners.sequentialRunner())).isNotEqualTo(
                withSyncRunner(Runners.queuedRunner()));
    }
}
