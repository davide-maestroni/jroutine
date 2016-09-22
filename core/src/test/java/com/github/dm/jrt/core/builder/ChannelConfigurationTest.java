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
import static com.github.dm.jrt.core.util.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.util.BackoffBuilder.noDelay;
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

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(builderFrom(configuration).configured().hashCode()).isEqualTo(
        configuration.hashCode());
    assertThat(builderFrom(configuration).configured()).isEqualTo(configuration);
    assertThat(builderFrom(null).configured().hashCode()).isEqualTo(
        ChannelConfiguration.defaultConfiguration().hashCode());
    assertThat(builderFrom(null).configured()).isEqualTo(
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
  }

  @Test
  public void testBuilderFromEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .withLogLevel(Level.SILENT)
                                                        .withBackoff(
                                                            afterCount(1).constantDelay(seconds(1)))
                                                        .withOutputTimeout(seconds(10))
                                                        .withOutputTimeoutAction(
                                                            TimeoutActionType.ABORT)
                                                        .configured();
    assertThat(builder().with(configuration).configured()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().with(null).configured()).isEqualTo(
        ChannelConfiguration.defaultConfiguration());
  }

  @Test
  public void testChannelBackoffEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withBackoff(noDelay()).configured());
    assertThat(configuration).isNotEqualTo(
        builder().withBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).configured());
    assertThat(configuration.builderFrom()
                            .withBackoff(afterCount(1).constantDelay(millis(1)))
                            .configured()).isNotEqualTo(
        builder().withBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).configured());
  }

  @Test
  public void testChannelOrderEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withOrder(OrderType.UNSORTED).configured());
    assertThat(configuration.builderFrom().withOrder(OrderType.SORTED).configured()).isNotEqualTo(
        builder().withOrder(OrderType.SORTED).configured());
  }

  @Test
  public void testChannelSizeEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withMaxSize(10).configured());
    assertThat(configuration.builderFrom().withMaxSize(1).configured()).isNotEqualTo(
        builder().withMaxSize(1).configured());
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

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).configured());
    assertThat(configuration.builderFrom().withLog(Logs.systemLog()).configured()).isNotEqualTo(
        builder().withLog(Logs.systemLog()).configured());
  }

  @Test
  public void testLogLevelEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).configured());
    assertThat(configuration.builderFrom().withLogLevel(Level.WARNING).configured()).isNotEqualTo(
        builder().withLogLevel(Level.WARNING).configured());
  }

  @Test
  public void testReadTimeoutActionEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
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
  public void testReadTimeoutEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withOutputTimeout(zero()).configured());
    assertThat(configuration).isNotEqualTo(
        builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).configured());
    assertThat(configuration.builderFrom().withOutputTimeout(millis(1)).configured()).isNotEqualTo(
        builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).configured());
  }

  @Test
  public void testRunnerEquals() {

    final ChannelConfiguration configuration = builder().withOrder(OrderType.SORTED)
                                                        .withRunner(Runners.syncRunner())
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(
        builder().withRunner(Runners.sharedRunner()).configured());
    assertThat(
        configuration.builderFrom().withRunner(Runners.syncRunner()).configured()).isNotEqualTo(
        builder().withRunner(Runners.syncRunner()).configured());
  }
}
