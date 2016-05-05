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
import static com.github.dm.jrt.core.util.UnitDuration.zero;
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
                                                            .apply();
        assertThat(builderFrom(configuration).apply().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).apply()).isEqualTo(configuration);
        assertThat(builderFrom(null).apply().hashCode()).isEqualTo(
                ChannelConfiguration.defaultConfiguration().hashCode());
        assertThat(builderFrom(null).apply()).isEqualTo(
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
                                                            .apply();
        assertThat(builder().with(configuration).apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).apply()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
    }

    @Test
    public void testChannelLimitEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelLimit(100)
                                                            .apply();
        assertThat(configuration).isNotEqualTo(builder().withChannelLimit(10).apply());
        assertThat(
                configuration.builderFrom().withChannelLimit(1).apply()).isNotEqualTo(
                builder().withChannelLimit(1).apply());
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
                                                            .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxDelay(zero()).apply());
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxDelay(1, TimeUnit.MILLISECONDS).apply());
        assertThat(configuration.builderFrom()
                                .withChannelMaxDelay(millis(1))
                                .apply()).isNotEqualTo(
                builder().withChannelMaxDelay(1, TimeUnit.MILLISECONDS).apply());
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
                                                            .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_DELAY).apply());
        assertThat(configuration.builderFrom()
                                .withChannelOrder(OrderType.BY_CALL)
                                .apply()).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_CALL).apply());
    }

    @Test
    public void testChannelSizeEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelMaxSize(10).apply());
        assertThat(configuration.builderFrom()
                                .withChannelMaxSize(1)
                                .apply()).isNotEqualTo(
                builder().withChannelMaxSize(1).apply());
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
                       .apply();
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
                                                            .apply();
        assertThat(builderFromInputChannel(invocationConfiguration).apply()).isEqualTo(
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
                       .apply();
        final ChannelConfiguration configuration = builder().withRunner(Runners.syncRunner())
                                                            .withReadTimeout(millis(100))
                                                            .withReadTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(Level.SILENT)
                                                            .apply();
        assertThat(builderFromInvocation(invocationConfiguration).apply()).isEqualTo(
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
                       .apply();
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
                                                            .apply();
        assertThat(
                builderFromOutputChannel(invocationConfiguration).apply()).isEqualTo(
                configuration);
    }

    @Test
    public void testLogEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withLog(Logs.nullLog()).apply());
        assertThat(configuration.builderFrom()
                                .withLog(Logs.systemLog())
                                .apply()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).apply());
    }

    @Test
    public void testLogLevelEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withLogLevel(Level.DEBUG).apply());
        assertThat(configuration.builderFrom()
                                .withLogLevel(Level.WARNING)
                                .apply()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).apply());
    }

    @Test
    public void testReadTimeoutActionEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
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
    public void testReadTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(zero()).apply());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).apply());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(millis(1))
                                .apply()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).apply());
    }

    @Test
    public void testRunnerEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).apply());
        assertThat(configuration.builderFrom()
                                .withRunner(Runners.syncRunner())
                                .apply()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).apply());
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
                                                            .apply();
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
                       .apply();
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
                                                            .apply();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withReadTimeout(millis(100))
                       .withReadTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(Level.SILENT)
                       .apply();
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
                                                            .apply();
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
                       .apply();
        assertThat(configuration.toOutputChannelConfiguration()).isEqualTo(invocationConfiguration);
    }
}
