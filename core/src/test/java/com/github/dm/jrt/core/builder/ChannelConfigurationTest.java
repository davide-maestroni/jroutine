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
import com.github.dm.jrt.core.util.TimeDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.config.ChannelConfiguration.builder;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFrom;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromInputChannel;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromInvocation;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFromOutputChannel;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Channel configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 07/03/2015.
 */
public class ChannelConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .setConfiguration();
        assertThat(builderFrom(configuration).setConfiguration().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).setConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).setConfiguration().hashCode()).isEqualTo(
                ChannelConfiguration.DEFAULT_CONFIGURATION.hashCode());
        assertThat(builderFrom(null).setConfiguration()).isEqualTo(
                ChannelConfiguration.DEFAULT_CONFIGURATION);
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

            new Builder<Object>(null, ChannelConfiguration.DEFAULT_CONFIGURATION);

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
                                                                    TimeDuration.seconds(1))
                                                            .withReadTimeout(
                                                                    TimeDuration.seconds(10))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .setConfiguration();
        assertThat(builder().with(configuration).setConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().setConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).setConfiguration()).isEqualTo(
                ChannelConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testChannelLimitEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelLimit(100)
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withChannelLimit(10).setConfiguration());
        assertThat(configuration.builderFrom().withChannelLimit(1).setConfiguration()).isNotEqualTo(
                builder().withChannelLimit(1).setConfiguration());
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
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxDelay(TimeDuration.ZERO).setConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxDelay(1, TimeUnit.MILLISECONDS).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withChannelMaxDelay(millis(1))
                                .setConfiguration()).isNotEqualTo(
                builder().withChannelMaxDelay(1, TimeUnit.MILLISECONDS).setConfiguration());
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
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_DELAY).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withChannelOrder(OrderType.BY_CALL)
                                .setConfiguration()).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_CALL).setConfiguration());
    }

    @Test
    public void testChannelSizeEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withChannelMaxSize(10).setConfiguration());
        assertThat(configuration.builderFrom().withChannelMaxSize(1).setConfiguration()).isNotEqualTo(
                builder().withChannelMaxSize(1).setConfiguration());
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
                       .setConfiguration();
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
                                                            .setConfiguration();
        assertThat(builderFromInputChannel(invocationConfiguration).setConfiguration()).isEqualTo(
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
                       .setConfiguration();
        final ChannelConfiguration configuration = builder().withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .setConfiguration();
        assertThat(builderFromInvocation(invocationConfiguration).setConfiguration()).isEqualTo(
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
                       .setConfiguration();
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
                                                            .setConfiguration();
        assertThat(builderFromOutputChannel(invocationConfiguration).setConfiguration()).isEqualTo(
                configuration);
    }

    @Test
    public void testLogEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).setConfiguration());
        assertThat(
                configuration.builderFrom().withLog(Logs.systemLog()).setConfiguration()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).setConfiguration());
    }

    @Test
    public void testLogLevelEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withLogLevel(Level.WARNING)
                                .setConfiguration()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).setConfiguration());
    }

    @Test
    public void testReadTimeoutActionEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.ABORT).setConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.EXIT).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withReadTimeoutAction(TimeoutActionType.THROW)
                                .setConfiguration()).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.THROW).setConfiguration());
    }

    @Test
    public void testReadTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(TimeDuration.ZERO).setConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(millis(1))
                                .setConfiguration()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).setConfiguration());
    }

    @Test
    public void testRunnerEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withRunner(Runners.syncRunner())
                                .setConfiguration()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).setConfiguration());
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
                                                            .setConfiguration();
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
                       .setConfiguration();
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
                                                            .setConfiguration();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .setConfiguration();
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
                                                            .setConfiguration();
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
                       .setConfiguration();
        assertThat(configuration.toOutputChannelConfiguration()).isEqualTo(invocationConfiguration);
    }
}
