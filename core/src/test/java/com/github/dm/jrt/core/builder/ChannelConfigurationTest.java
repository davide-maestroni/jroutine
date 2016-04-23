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
import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logs;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.UnitDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.config.ChannelConfiguration.builder;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFrom;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromInputChannel;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromInvocation;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromOutputChannel;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Channel configuration unit tests.
 * <p>
 * Created by davide-maestroni on 07/03/2015.
 */
public class ChannelConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(builderFrom(configuration).applyConfiguration().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).applyConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).applyConfiguration().hashCode()).isEqualTo(
                ChannelConfiguration.defaultConfiguration().hashCode());
        assertThat(builderFrom(null).applyConfiguration()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
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

            new Builder<Object>(null, ChannelConfiguration.defaultConfiguration());

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .withLogLevel(Level.SILENT)
                                                            .withChannelMaxDelay(
                                                                    UnitDuration.seconds(1))
                                                            .withReadTimeout(
                                                                    UnitDuration.seconds(10))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .applyConfiguration();
        assertThat(builder().with(configuration).applyConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().applyConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).applyConfiguration()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
    }

    @Test
    public void testChannelLimitEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelLimit(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withChannelLimit(10).applyConfiguration());
        assertThat(
                configuration.builderFrom().withChannelLimit(1).applyConfiguration()).isNotEqualTo(
                builder().withChannelLimit(1).applyConfiguration());
    }

    @Test
    public void testChannelLimitError() {

        try {

            builder().withChannelLimit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testChannelMaxDelayEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxDelay(UnitDuration.ZERO).applyConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxDelay(1, TimeUnit.MILLISECONDS).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withChannelMaxDelay(millis(1))
                                .applyConfiguration()).isNotEqualTo(
                builder().withChannelMaxDelay(1, TimeUnit.MILLISECONDS).applyConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testChannelMaxDelayError() {

        try {

            builder().withChannelMaxDelay(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withChannelMaxDelay(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testChannelOrderEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_DELAY).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withChannelOrder(OrderType.BY_CALL)
                                .applyConfiguration()).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_CALL).applyConfiguration());
    }

    @Test
    public void testChannelSizeEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxSize(10).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withChannelMaxSize(1)
                                .applyConfiguration()).isNotEqualTo(
                builder().withChannelMaxSize(1).applyConfiguration());
    }

    @Test
    public void testChannelSizeError() {

        try {

            builder().withChannelMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testFromInputChannelConfiguration() {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withInputOrder(OrderType.BY_CALL)
                       .withInputLimit(10)
                       .withInputMaxDelay(millis(33))
                       .withInputMaxSize(100)
                       .applyConfiguration();
        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelLimit(10)
                                                            .withChannelMaxDelay(millis(33))
                                                            .withChannelMaxSize(100)
                                                            .withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .applyConfiguration();
        assertThat(builderFromInputChannel(invocationConfiguration).applyConfiguration()).isEqualTo(
                configuration);
    }

    @Test
    public void testFromInvocationConfiguration() {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withInputOrder(OrderType.BY_CALL)
                       .withInputLimit(10)
                       .withInputMaxDelay(millis(33))
                       .withInputMaxSize(100)
                       .applyConfiguration();
        final ChannelConfiguration configuration = builder().withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .applyConfiguration();
        assertThat(builderFromInvocation(invocationConfiguration).applyConfiguration()).isEqualTo(
                configuration);
    }

    @Test
    public void testFromOutputChannelConfiguration() {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withOutputOrder(OrderType.BY_CALL)
                       .withOutputLimit(10)
                       .withOutputMaxDelay(millis(33))
                       .withOutputMaxSize(100)
                       .applyConfiguration();
        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelLimit(10)
                                                            .withChannelMaxDelay(millis(33))
                                                            .withChannelMaxSize(100)
                                                            .withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .applyConfiguration();
        assertThat(
                builderFromOutputChannel(invocationConfiguration).applyConfiguration()).isEqualTo(
                configuration);
    }

    @Test
    public void testLogEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withLog(Logs.nullLog()).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withLog(Logs.systemLog())
                                .applyConfiguration()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).applyConfiguration());
    }

    @Test
    public void testLogLevelEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withLogLevel(Level.DEBUG).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withLogLevel(Level.WARNING)
                                .applyConfiguration()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).applyConfiguration());
    }

    @Test
    public void testReadTimeoutActionEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.ABORT).applyConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.EXIT).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withReadTimeoutAction(TimeoutActionType.THROW)
                                .applyConfiguration()).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.THROW).applyConfiguration());
    }

    @Test
    public void testReadTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(UnitDuration.ZERO).applyConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(millis(1))
                                .applyConfiguration()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).applyConfiguration());
    }

    @Test
    public void testRunnerEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .applyConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).applyConfiguration());
        assertThat(configuration.builderFrom()
                                .withRunner(Runners.syncRunner())
                                .applyConfiguration()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).applyConfiguration());
    }

    @Test
    public void testToInputChannelConfiguration() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelLimit(10)
                                                            .withChannelMaxDelay(millis(33))
                                                            .withChannelMaxSize(100)
                                                            .withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .applyConfiguration();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withInputOrder(OrderType.BY_CALL)
                       .withInputLimit(10)
                       .withInputMaxDelay(millis(33))
                       .withInputMaxSize(100)
                       .applyConfiguration();
        assertThat(configuration.toInputChannelConfiguration()).isEqualTo(invocationConfiguration);
    }

    @Test
    public void testToInvocationConfiguration() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelLimit(10)
                                                            .withChannelMaxDelay(millis(33))
                                                            .withChannelMaxSize(100)
                                                            .withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .applyConfiguration();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .applyConfiguration();
        assertThat(configuration.toInvocationConfiguration()).isEqualTo(invocationConfiguration);
    }

    @Test
    public void testToOutputChannelConfiguration() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelLimit(10)
                                                            .withChannelMaxDelay(millis(33))
                                                            .withChannelMaxSize(100)
                                                            .withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .applyConfiguration();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .withOutputOrder(OrderType.BY_CALL)
                       .withOutputLimit(10)
                       .withOutputMaxDelay(millis(33))
                       .withOutputMaxSize(100)
                       .applyConfiguration();
        assertThat(configuration.toOutputChannelConfiguration()).isEqualTo(invocationConfiguration);
    }
}
