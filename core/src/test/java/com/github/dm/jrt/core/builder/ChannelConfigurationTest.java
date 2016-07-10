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
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logs;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.UnitDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.config.ChannelConfiguration.builder;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFrom;
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

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(builderFrom(configuration).applied().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).applied()).isEqualTo(configuration);
        assertThat(builderFrom(null).applied().hashCode()).isEqualTo(
                ChannelConfiguration.defaultConfiguration().hashCode());
        assertThat(builderFrom(null).applied()).isEqualTo(
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

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .withLogLevel(Level.SILENT)
                                                            .withBackoff(UnitDuration.seconds(1))
                                                            .withOutputTimeout(
                                                                    UnitDuration.seconds(10))
                                                            .withOutputTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .applied();
        assertThat(builder().with(configuration).applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).applied()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
    }

    @Test
    public void testChannelLimitEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withLimit(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withLimit(10).applied());
        assertThat(configuration.builderFrom().withLimit(1).applied()).isNotEqualTo(
                builder().withLimit(1).applied());
    }

    @Test
    public void testChannelLimitError() {

        try {

            builder().withLimit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testChannelMaxDelayEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withBackoff(zero()).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withBackoff(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom().withBackoff(millis(1)).applied()).isNotEqualTo(
                builder().withBackoff(1, TimeUnit.MILLISECONDS).applied());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testChannelMaxDelayError() {

        try {

            builder().withBackoff(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withBackoff(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testChannelOrderEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withOrder(OrderType.BY_DELAY).applied());
        assertThat(configuration.builderFrom().withOrder(OrderType.BY_CALL).applied()).isNotEqualTo(
                builder().withOrder(OrderType.BY_CALL).applied());
    }

    @Test
    public void testChannelSizeEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withMaxSize(10).applied());
        assertThat(configuration.builderFrom().withMaxSize(1).applied()).isNotEqualTo(
                builder().withMaxSize(1).applied());
    }

    @Test
    public void testChannelSizeError() {

        try {

            builder().withMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testLogEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).applied());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).applied()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).applied());
    }

    @Test
    public void testLogLevelEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).applied());
        assertThat(configuration.builderFrom().withLogLevel(Level.WARNING).applied()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).applied());
    }

    @Test
    public void testReadTimeoutActionEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
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
    public void testReadTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withOutputTimeout(zero()).applied());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).applied());
        assertThat(configuration.builderFrom().withOutputTimeout(millis(1)).applied()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).applied());
    }

    @Test
    public void testRunnerEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).applied());
        assertThat(configuration.builderFrom()
                                .withRunner(Runners.syncRunner())
                                .applied()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).applied());
    }
}
