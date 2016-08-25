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
import static com.github.dm.jrt.core.util.Backoffs.afterCount;
import static com.github.dm.jrt.core.util.Backoffs.noDelay;
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
                                                               .withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(builderFrom(configuration).configured().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).configured()).isEqualTo(configuration);
        assertThat(builderFrom(null).configured().hashCode()).isEqualTo(
                InvocationConfiguration.defaultConfiguration().hashCode());
        assertThat(builderFrom(null).configured()).isEqualTo(
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
                                                               .withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(builder().with(configuration).configured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).configured()).isEqualTo(
                InvocationConfiguration.defaultConfiguration());
    }

    @Test
    public void testCoreInvocationsEquals() {

        final InvocationConfiguration configuration = builder().withCoreInstances(27)
                                                               .withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withCoreInstances(3).configured());
        assertThat(configuration.builderFrom().withCoreInstances(27).configured()).isNotEqualTo(
                builder().withCoreInstances(27).configured());
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

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.ABORT).configured());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.CONTINUE).configured());
        assertThat(configuration.builderFrom()
                                .withOutputTimeoutAction(TimeoutActionType.FAIL)
                                .configured()).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.FAIL).configured());
    }

    @Test
    public void testExecutionTimeoutEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withOutputTimeout(zero()).configured());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).configured());
        assertThat(configuration.builderFrom()
                                .withOutputTimeout(UnitDuration.millis(1))
                                .configured()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).configured());
    }

    @Test
    public void testFromInputChannelConfiguration() {

        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.SORTED)
                                                                       .withBackoff(afterCount(
                                                                               1).constantDelay(
                                                                               millis(33)))
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
                                                                       .configured();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withOutputTimeout(millis(100))
                       .withOutputTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withInputOrder(OrderType.SORTED)
                       .withInputBackoff(afterCount(1).constantDelay(millis(33)))
                       .withInputMaxSize(100)
                       .configured();
        assertThat(builderFromInput(configuration).configured()).isEqualTo(invocationConfiguration);
    }

    @Test
    public void testFromOutputChannelConfiguration() {

        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.SORTED)
                                                                       .withBackoff(afterCount(
                                                                               1).constantDelay(
                                                                               millis(33)))
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
                                                                       .configured();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withOutputTimeout(millis(100))
                       .withOutputTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withOutputOrder(OrderType.SORTED)
                       .withOutputBackoff(afterCount(1).constantDelay(millis(33)))
                       .withOutputMaxSize(100)
                       .configured();
        assertThat(builderFromOutput(configuration).configured()).isEqualTo(
                invocationConfiguration);
    }

    @Test
    public void testInputBackoffEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withInputBackoff(noDelay()).configured());
        assertThat(configuration).isNotEqualTo(
                builder().withInputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS))
                         .configured());
        assertThat(configuration.builderFrom()
                                .withInputBackoff(
                                        afterCount(1).constantDelay(UnitDuration.millis(1)))
                                .configured()).isNotEqualTo(
                builder().withInputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS))
                         .configured());
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
                       .withInputOrder(OrderType.SORTED)
                       .withInputBackoff(afterCount(1).constantDelay(millis(33)))
                       .withInputMaxSize(100)
                       .configured();
        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.SORTED)
                                                                       .withBackoff(afterCount(
                                                                               1).constantDelay(
                                                                               millis(33)))
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
                                                                       .configured();
        assertThat(invocationConfiguration.inputConfigurationBuilder().configured()).isEqualTo(
                configuration);
    }

    @Test
    public void testInputOrderEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withInputOrder(OrderType.UNSORTED).configured());
        assertThat(configuration.builderFrom()
                                .withInputOrder(OrderType.SORTED)
                                .configured()).isNotEqualTo(
                builder().withInputOrder(OrderType.SORTED).configured());
    }

    @Test
    public void testInputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withInputMaxSize(10).configured());
        assertThat(configuration.builderFrom().withInputMaxSize(31).configured()).isNotEqualTo(
                builder().withInputMaxSize(31).configured());
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

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).configured());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).configured()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).configured());
    }

    @Test
    public void testLogLevelEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).configured());
        assertThat(
                configuration.builderFrom().withLogLevel(Level.WARNING).configured()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).configured());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withMaxInstances(4).configured());
        assertThat(configuration.builderFrom().withMaxInstances(41).configured()).isNotEqualTo(
                builder().withMaxInstances(41).configured());
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
    public void testOutputBackoffEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withOutputBackoff(noDelay()).configured());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS))
                         .configured());
        assertThat(configuration.builderFrom()
                                .withOutputBackoff(
                                        afterCount(1).constantDelay(UnitDuration.millis(1)))
                                .configured()).isNotEqualTo(
                builder().withOutputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS))
                         .configured());
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
                       .withOutputOrder(OrderType.SORTED)
                       .withOutputBackoff(afterCount(1).constantDelay(millis(33)))
                       .withOutputMaxSize(100)
                       .configured();
        final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                       .withOrder(OrderType.SORTED)
                                                                       .withBackoff(afterCount(
                                                                               1).constantDelay(
                                                                               millis(33)))
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
                                                                       .configured();
        assertThat(invocationConfiguration.outputConfigurationBuilder().configured()).isEqualTo(
                configuration);
    }

    @Test
    public void testOutputOrderEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputOrder(OrderType.UNSORTED).configured());
        assertThat(configuration.builderFrom()
                                .withOutputOrder(OrderType.SORTED)
                                .configured()).isNotEqualTo(
                builder().withOutputOrder(OrderType.SORTED).configured());
    }

    @Test
    public void testOutputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withOutputMaxSize(10).configured());
        assertThat(configuration.builderFrom().withOutputMaxSize(1).configured()).isNotEqualTo(
                builder().withOutputMaxSize(1).configured());
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
                                                               .withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(builder().withPriority(3).configured());
        assertThat(configuration.builderFrom().withPriority(17).configured()).isNotEqualTo(
                builder().withPriority(17).configured());
    }

    @Test
    public void testRunnerEquals() {

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.SORTED)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).configured());
        assertThat(configuration.builderFrom()
                                .withRunner(Runners.syncRunner())
                                .configured()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).configured());
    }
}
