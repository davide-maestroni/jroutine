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

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.config.ChannelConfiguration.builder;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFrom;
import static com.github.dm.jrt.core.util.Backoffs.afterCount;
import static com.github.dm.jrt.core.util.Backoffs.noDelay;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
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
                                                            .buildConfiguration();
        assertThat(builderFrom(configuration).buildConfiguration().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).buildConfiguration().hashCode()).isEqualTo(
                ChannelConfiguration.defaultConfiguration().hashCode());
        assertThat(builderFrom(null).buildConfiguration()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuildNullPointerError() {

        try {

            new Builder(null);

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
                                                            .withBackoff(
                                                                    afterCount(1).constantDelay(
                                                                            seconds(1)))
                                                            .withOutputTimeout(seconds(10))
                                                            .withOutputTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .buildConfiguration();
        assertThat(builder().with(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).buildConfiguration()).isEqualTo(
                ChannelConfiguration.defaultConfiguration());
    }

    @Test
    public void testChannelBackoffEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withBackoff(noDelay()).buildConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS))
                         .buildConfiguration());
        assertThat(configuration.builderFrom()
                                .withBackoff(afterCount(1).constantDelay(millis(1)))
                                .buildConfiguration()).isNotEqualTo(
                builder().withBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS))
                         .buildConfiguration());
    }

    @Test
    public void testChannelOrderEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withOrder(OrderType.BY_DELAY).buildConfiguration());
        assertThat(configuration.builderFrom()
                                .withOrder(OrderType.BY_CALL)
                                .buildConfiguration()).isNotEqualTo(
                builder().withOrder(OrderType.BY_CALL).buildConfiguration());
    }

    @Test
    public void testChannelSizeEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withMaxSize(10).buildConfiguration());
        assertThat(configuration.builderFrom().withMaxSize(1).buildConfiguration()).isNotEqualTo(
                builder().withMaxSize(1).buildConfiguration());
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
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withLog(Logs.nullLog()).buildConfiguration());
        assertThat(configuration.builderFrom()
                                .withLog(Logs.systemLog())
                                .buildConfiguration()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).buildConfiguration());
    }

    @Test
    public void testLogLevelEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withLogLevel(Level.DEBUG).buildConfiguration());
        assertThat(configuration.builderFrom()
                                .withLogLevel(Level.WARNING)
                                .buildConfiguration()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).buildConfiguration());
    }

    @Test
    public void testReadTimeoutActionEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.ABORT).buildConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.BREAK).buildConfiguration());
        assertThat(configuration.builderFrom()
                                .withOutputTimeoutAction(TimeoutActionType.FAIL)
                                .buildConfiguration()).isNotEqualTo(
                builder().withOutputTimeoutAction(TimeoutActionType.FAIL).buildConfiguration());
    }

    @Test
    public void testReadTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(zero()).buildConfiguration());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(configuration.builderFrom()
                                .withOutputTimeout(millis(1))
                                .buildConfiguration()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
    }

    @Test
    public void testRunnerEquals() {

        final ChannelConfiguration configuration = builder().withOrder(OrderType.BY_CALL)
                                                            .withRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withMaxSize(100)
                                                            .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withRunner(Runners.sharedRunner()).buildConfiguration());
        assertThat(configuration.builderFrom()
                                .withRunner(Runners.syncRunner())
                                .buildConfiguration()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).buildConfiguration());
    }
}
