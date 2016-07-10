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

import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logs;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.UnitDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFrom;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFromInput;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFromOutput;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
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
                                                               .applied();
        assertThat(builderFrom(configuration).applied().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).applied()).isEqualTo(configuration);
        assertThat(builderFrom(null).applied().hashCode()).isEqualTo(
                InvocationConfiguration.defaultConfiguration().hashCode());
        assertThat(builderFrom(null).applied()).isEqualTo(
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
                                                               .applied();
        assertThat(builder().with(configuration).applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).applied()).isEqualTo(
                InvocationConfiguration.defaultConfiguration());
    }

    @Test
    public void testCoreInvocationsEquals() {

        final InvocationConfiguration configuration = builder().withCoreInstances(27)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withCoreInstances(3).applied());
        assertThat(configuration.builderFrom().withCoreInstances(27).applied()).isNotEqualTo(
                builder().withCoreInstances(27).applied());
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
                                                               .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.ABORT).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.BREAK).applied());
        assertThat(configuration.builderFrom()
                                .withOutputTimeoutAction(TimeoutActionType.FAIL)
                                .applied()).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.FAIL).applied());
    }

    @Test
    public void testExecutionTimeoutEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withOutputTimeout(zero()).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom()
                                .withOutputTimeout(UnitDuration.millis(1))
                                .applied()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).applied());
    }

    @Test
    public void testFromInputChannelConfiguration() {

        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.BY_CALL)
                                                                       .withLimit(10)
                                                                       .withBackoff(millis(33))
                                                                       .withMaxSize(100)
                                                                       .withRunner(
                                                                               Runners.syncRunner())
                                                                       .withOutputTimeout(
                                                                               millis(100))
                                                                       .withOutputTimeoutAction(
                                                                               TimeoutActionType
                                                                                       .ABORT)
                                                                       .withLog(Logs.nullLog())
                                                                       .withLogLevel(Level.SILENT)
                                                                       .applied();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withOutputTimeout(millis(100))
                       .withOutputTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withInputOrder(OrderType.BY_CALL)
                       .withInputLimit(10)
                       .withInputBackoff(millis(33))
                       .withInputMaxSize(100)
                       .applied();
        assertThat(builderFromInput(configuration).applied()).isEqualTo(invocationConfiguration);
    }

    @Test
    public void testFromOutputChannelConfiguration() {

        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.BY_CALL)
                                                                       .withLimit(10)
                                                                       .withBackoff(millis(33))
                                                                       .withMaxSize(100)
                                                                       .withRunner(
                                                                               Runners.syncRunner())
                                                                       .withOutputTimeout(
                                                                               millis(100))
                                                                       .withOutputTimeoutAction(
                                                                               TimeoutActionType
                                                                                       .ABORT)
                                                                       .withLog(Logs.nullLog())
                                                                       .withLogLevel(Level.SILENT)
                                                                       .applied();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withOutputTimeout(millis(100))
                       .withOutputTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withOutputOrder(OrderType.BY_CALL)
                       .withOutputLimit(10)
                       .withOutputBackoff(millis(33))
                       .withOutputMaxSize(100)
                       .applied();
        assertThat(builderFromOutput(configuration).applied()).isEqualTo(invocationConfiguration);
    }

    @Test
    public void testInputChannelConfiguration() {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withOutputTimeout(millis(100))
                       .withOutputTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withInputOrder(OrderType.BY_CALL)
                       .withInputLimit(10)
                       .withInputBackoff(millis(33))
                       .withInputMaxSize(100)
                       .applied();
        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.BY_CALL)
                                                                       .withLimit(10)
                                                                       .withBackoff(millis(33))
                                                                       .withMaxSize(100)
                                                                       .withRunner(
                                                                               Runners.syncRunner())
                                                                       .withOutputTimeout(
                                                                               millis(100))
                                                                       .withOutputTimeoutAction(
                                                                               TimeoutActionType
                                                                                       .ABORT)
                                                                       .withLog(Logs.nullLog())
                                                                       .withLogLevel(Level.SILENT)
                                                                       .applied();
        assertThat(invocationConfiguration.inputConfigurationBuilder().applied()).isEqualTo(
                configuration);
    }

    @Test
    public void testInputLimitEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withInputLimit(10).applied());
        assertThat(configuration.builderFrom().withInputLimit(31).applied()).isNotEqualTo(
                builder().withInputLimit(31).applied());
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
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withInputBackoff(zero()).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withInputBackoff(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom()
                                .withInputBackoff(UnitDuration.millis(1))
                                .applied()).isNotEqualTo(
                builder().withInputBackoff(1, TimeUnit.MILLISECONDS).applied());
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
                                                               .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withInputOrder(OrderType.BY_DELAY).applied());
        assertThat(configuration.builderFrom()
                                .withInputOrder(OrderType.BY_CALL)
                                .applied()).isNotEqualTo(
                builder().withInputOrder(OrderType.BY_CALL).applied());
    }

    @Test
    public void testInputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withInputMaxSize(10).applied());
        assertThat(configuration.builderFrom().withInputMaxSize(31).applied()).isNotEqualTo(
                builder().withInputMaxSize(31).applied());
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
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).applied());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).applied()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).applied());
    }

    @Test
    public void testLogLevelEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).applied());
        assertThat(configuration.builderFrom().withLogLevel(Level.WARNING).applied()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).applied());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withMaxInstances(4).applied());
        assertThat(configuration.builderFrom().withMaxInstances(41).applied()).isNotEqualTo(
                builder().withMaxInstances(41).applied());
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
    public void testOutputChannelConfiguration() {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withOutputTimeout(millis(100))
                       .withOutputTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withOutputOrder(OrderType.BY_CALL)
                       .withOutputLimit(10)
                       .withOutputBackoff(millis(33))
                       .withOutputMaxSize(100)
                       .applied();
        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.BY_CALL)
                                                                       .withLimit(10)
                                                                       .withBackoff(millis(33))
                                                                       .withMaxSize(100)
                                                                       .withRunner(
                                                                               Runners.syncRunner())
                                                                       .withOutputTimeout(
                                                                               millis(100))
                                                                       .withOutputTimeoutAction(
                                                                               TimeoutActionType
                                                                                       .ABORT)
                                                                       .withLog(Logs.nullLog())
                                                                       .withLogLevel(Level.SILENT)
                                                                       .applied();
        assertThat(invocationConfiguration.outputConfigurationBuilder().applied()).isEqualTo(
                configuration);
    }

    @Test
    public void testOutputLimitEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withOutputLimit(10).applied());
        assertThat(configuration.builderFrom().withOutputLimit(31).applied()).isNotEqualTo(
                builder().withOutputLimit(31).applied());
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
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withOutputBackoff(zero()).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputBackoff(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom()
                                .withOutputBackoff(UnitDuration.millis(1))
                                .applied()).isNotEqualTo(
                builder().withOutputBackoff(1, TimeUnit.MILLISECONDS).applied());
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
                                                               .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputOrder(OrderType.BY_DELAY).applied());
        assertThat(configuration.builderFrom()
                                .withOutputOrder(OrderType.BY_CALL)
                                .applied()).isNotEqualTo(
                builder().withOutputOrder(OrderType.BY_CALL).applied());
    }

    @Test
    public void testOutputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withOutputMaxSize(10).applied());
        assertThat(configuration.builderFrom().withOutputMaxSize(1).applied()).isNotEqualTo(
                builder().withOutputMaxSize(1).applied());
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
                                                               .applied();
        assertThat(configuration).isNotEqualTo(builder().withPriority(3).applied());
        assertThat(configuration.builderFrom().withPriority(17).applied()).isNotEqualTo(
                builder().withPriority(17).applied());
    }

    @Test
    public void testRunnerEquals() {

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).applied());
        assertThat(configuration.builderFrom()
                                .withRunner(Runners.syncRunner())
                                .applied()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).applied());
    }
}
