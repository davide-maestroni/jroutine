/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.core.config;

import com.github.dm.jrt.core.config.ChannelConfiguration.Builder;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logs;
import com.github.dm.jrt.core.log.NullLog;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.common.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.common.BackoffBuilder.noDelay;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builder;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
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

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(builderFrom(configuration).configuration().hashCode()).isEqualTo(
        configuration.hashCode());
    assertThat(builderFrom(configuration).configuration()).isEqualTo(configuration);
    assertThat(builderFrom(null).configuration().hashCode()).isEqualTo(
        ChannelConfiguration.defaultConfiguration().hashCode());
    assertThat(builderFrom(null).configuration()).isEqualTo(
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
                                                        .withLog(new NullLog())
                                                        .withMaxSize(100)
                                                        .withLogLevel(Level.SILENT)
                                                        .withBackoff(
                                                            afterCount(1).constantDelay(seconds(1)))
                                                        .withOutputTimeout(seconds(10))
                                                        .withOutputTimeoutAction(
                                                            TimeoutActionType.ABORT)
                                                        .configuration();
    assertThat(builder().withPatch(configuration).configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withPatch(null).configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withDefaults().configuration()).isEqualTo(
        ChannelConfiguration.defaultConfiguration());
  }

  @Test
  public void testChannelBackoffEquals() {

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(configuration).isNotEqualTo(builder().withBackoff(noDelay()).configuration());
    assertThat(configuration).isNotEqualTo(
        builder().withBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).configuration());
    assertThat(configuration.builderFrom()
                            .withBackoff(afterCount(1).constantDelay(millis(1)))
                            .configuration()).isNotEqualTo(
        builder().withBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).configuration());
  }

  @Test
  public void testChannelOrderEquals() {

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(configuration).isNotEqualTo(builder().withOrder(OrderType.UNSORTED).configuration());
    assertThat(configuration.builderFrom().withOrder(OrderType.SORTED).configuration()).isNotEqualTo(
        builder().withOrder(OrderType.SORTED).configuration());
  }

  @Test
  public void testChannelSizeEquals() {

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(configuration).isNotEqualTo(builder().withMaxSize(10).configuration());
    assertThat(configuration.builderFrom().withMaxSize(1).configuration()).isNotEqualTo(
        builder().withMaxSize(1).configuration());
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

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).configuration());
    assertThat(configuration.builderFrom().withLog(Logs.systemLog()).configuration()).isNotEqualTo(
        builder().withLog(Logs.systemLog()).configuration());
  }

  @Test
  public void testLogLevelEquals() {

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).configuration());
    assertThat(configuration.builderFrom().withLogLevel(Level.WARNING).configuration()).isNotEqualTo(
        builder().withLogLevel(Level.WARNING).configuration());
  }

  @Test
  public void testReadTimeoutActionEquals() {

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(configuration).isNotEqualTo(
        builder().withOutputTimeoutAction(TimeoutActionType.ABORT).configuration());
    assertThat(configuration).isNotEqualTo(
        builder().withOutputTimeoutAction(TimeoutActionType.CONTINUE).configuration());
    assertThat(configuration.builderFrom()
                            .withOutputTimeoutAction(TimeoutActionType.FAIL)
                            .configuration()).isNotEqualTo(
        builder().withOutputTimeoutAction(TimeoutActionType.FAIL).configuration());
  }

  @Test
  public void testReadTimeoutEquals() {

    final ChannelConfiguration configuration =
        builder().withOrder(OrderType.SORTED).withLog(new NullLog()).withMaxSize(100).configuration();
    assertThat(configuration).isNotEqualTo(builder().withOutputTimeout(noTime()).configuration());
    assertThat(configuration).isNotEqualTo(
        builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).configuration());
    assertThat(configuration.builderFrom().withOutputTimeout(millis(1)).configuration()).isNotEqualTo(
        builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).configuration());
  }
}
