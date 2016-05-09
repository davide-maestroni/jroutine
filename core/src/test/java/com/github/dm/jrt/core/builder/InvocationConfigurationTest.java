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

package com.github.dm.jrt.core.builder;

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logs;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.UnitDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.zero;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocation configuration unit tests.
 * <p>
 * Created by davide-maestroni on 11/22/2014.
 */
public class InvocationConfigurationTest {

    @Test
    public void testBuildFrom() {

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(builderFrom(configuration).apply().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).apply()).isEqualTo(configuration);
        assertThat(builderFrom(null).apply().hashCode()).isEqualTo(
                InvocationConfiguration.defaultConfiguration().hashCode());
        assertThat(builderFrom(null).apply()).isEqualTo(
                InvocationConfiguration.defaultConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuildNullPointerError() {

        try {

            new Builder<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new Builder<Object>(null, InvocationConfiguration.defaultConfiguration());

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(builder().with(configuration).apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).apply()).isEqualTo(
                InvocationConfiguration.defaultConfiguration());
    }

    @Test
    public void testCoreInvocationsEquals() {

        final InvocationConfiguration configuration = builder().withCoreInstances(27)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withCoreInstances(3).apply());
        assertThat(configuration.builderFrom().withCoreInstances(27).apply()).isNotEqualTo(
                builder().withCoreInstances(27).apply());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCoreInvocationsError() {

        try {

            builder().withCoreInstances(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testExecutionTimeoutActionEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.ABORT).apply());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.EXIT).apply());
        assertThat(configuration.builderFrom()
                                .withReadTimeoutAction(TimeoutActionType.THROW)
                                .apply()).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.THROW).apply());
    }

    @Test
    public void testExecutionTimeoutEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withReadTimeout(zero()).apply());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).apply());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(UnitDuration.millis(1))
                                .apply()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).apply());
    }

    @Test
    public void testInputLimitEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withInputLimit(10).apply());
        assertThat(configuration.builderFrom().withInputLimit(31).apply()).isNotEqualTo(
                builder().withInputLimit(31).apply());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputLimitError() {

        try {

            builder().withInputLimit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputMaxDelayEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withInputBackoff(zero()).apply());
        assertThat(configuration).isNotEqualTo(
                builder().withInputBackoff(1, TimeUnit.MILLISECONDS).apply());
        assertThat(configuration.builderFrom()
                                .withInputBackoff(UnitDuration.millis(1))
                                .apply()).isNotEqualTo(
                builder().withInputBackoff(1, TimeUnit.MILLISECONDS).apply());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputMaxDelayError() {

        try {

            builder().withInputBackoff(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withInputBackoff(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputOrderEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withInputOrder(OrderType.BY_DELAY).apply());
        assertThat(
                configuration.builderFrom().withInputOrder(OrderType.BY_CALL).apply()).isNotEqualTo(
                builder().withInputOrder(OrderType.BY_CALL).apply());
    }

    @Test
    public void testInputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withInputMaxSize(10).apply());
        assertThat(configuration.builderFrom().withInputMaxSize(31).apply()).isNotEqualTo(
                builder().withInputMaxSize(31).apply());
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
    public void testLogEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).apply());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).apply()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).apply());
    }

    @Test
    public void testLogLevelEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).apply());
        assertThat(configuration.builderFrom().withLogLevel(Level.WARNING).apply()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).apply());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withMaxInstances(4).apply());
        assertThat(configuration.builderFrom().withMaxInstances(41).apply()).isNotEqualTo(
                builder().withMaxInstances(41).apply());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMaxInvocationsError() {

        try {

            builder().withMaxInstances(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputLimitEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withOutputLimit(10).apply());
        assertThat(configuration.builderFrom().withOutputLimit(31).apply()).isNotEqualTo(
                builder().withOutputLimit(31).apply());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputLimitError() {

        try {

            builder().withOutputLimit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputMaxDelayEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withOutputBackoff(zero()).apply());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputBackoff(1, TimeUnit.MILLISECONDS).apply());
        assertThat(configuration.builderFrom()
                                .withOutputBackoff(UnitDuration.millis(1))
                                .apply()).isNotEqualTo(
                builder().withOutputBackoff(1, TimeUnit.MILLISECONDS).apply());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputMaxDelayError() {

        try {

            builder().withOutputBackoff(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withOutputBackoff(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputOrderEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputOrder(OrderType.BY_DELAY).apply());
        assertThat(configuration.builderFrom()
                                .withOutputOrder(OrderType.BY_CALL)
                                .apply()).isNotEqualTo(
                builder().withOutputOrder(OrderType.BY_CALL).apply());
    }

    @Test
    public void testOutputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withOutputMaxSize(10).apply());
        assertThat(configuration.builderFrom().withOutputMaxSize(1).apply()).isNotEqualTo(
                builder().withOutputMaxSize(1).apply());
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
    public void testPriorityEquals() {

        final InvocationConfiguration configuration = builder().withPriority(17)
                                                               .withCoreInstances(27)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withPriority(3).apply());
        assertThat(configuration.builderFrom().withPriority(17).apply()).isNotEqualTo(
                builder().withPriority(17).apply());
    }

    @Test
    public void testRunnerEquals() {

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).apply());
        assertThat(
                configuration.builderFrom().withRunner(Runners.syncRunner()).apply()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).apply());
    }
}
