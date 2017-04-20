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

import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logs;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.runner.Runners;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.common.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.common.BackoffBuilder.noDelay;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFrom;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFromInput;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFromOutput;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;
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
                                                           .apply();
    assertThat(builderFrom(configuration).apply().hashCode()).isEqualTo(configuration.hashCode());
    assertThat(builderFrom(configuration).apply()).isEqualTo(configuration);
    assertThat(builderFrom(null).apply().hashCode()).isEqualTo(
        InvocationConfiguration.defaultConfiguration().hashCode());
    assertThat(builderFrom(null).apply()).isEqualTo(InvocationConfiguration.defaultConfiguration());
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
                                                           .apply();
    assertThat(builder().withPatch(configuration).apply()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withPatch(null).apply()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withDefaults().apply()).isEqualTo(
        InvocationConfiguration.defaultConfiguration());
  }

  @Test
  public void testCoreInvocationsEquals() {

    final InvocationConfiguration configuration = builder().withCoreInvocations(27)
                                                           .withInputOrder(OrderType.SORTED)
                                                           .withRunner(Runners.syncRunner())
                                                           .withLog(new NullLog())
                                                           .withOutputMaxSize(100)
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withCoreInvocations(3).apply());
    assertThat(configuration.builderFrom().withCoreInvocations(27).apply()).isNotEqualTo(
        builder().withCoreInvocations(27).apply());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCoreInvocationsError() {

    try {

      builder().withCoreInvocations(-1);

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
                                                           .apply();
    assertThat(configuration).isNotEqualTo(
        builder().withOutputTimeoutAction(TimeoutActionType.ABORT).apply());
    assertThat(configuration).isNotEqualTo(
        builder().withOutputTimeoutAction(TimeoutActionType.CONTINUE).apply());
    assertThat(configuration.builderFrom()
                            .withOutputTimeoutAction(TimeoutActionType.FAIL)
                            .apply()).isNotEqualTo(
        builder().withOutputTimeoutAction(TimeoutActionType.FAIL).apply());
  }

  @Test
  public void testExecutionTimeoutEquals() {

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                           .withRunner(Runners.syncRunner())
                                                           .withLog(new NullLog())
                                                           .withOutputMaxSize(100)
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withOutputTimeout(noTime()).apply());
    assertThat(configuration).isNotEqualTo(
        builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).apply());
    assertThat(configuration.builderFrom().withOutputTimeout(millis(1)).apply()).isNotEqualTo(
        builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).apply());
  }

  @Test
  public void testFromInputChannelConfiguration() {

    final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                   .withOrder(OrderType.SORTED)
                                                                   .withBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withMaxSize(100)
                                                                   .withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .apply();
    final InvocationConfiguration.Builder<InvocationConfiguration> builder =
        InvocationConfiguration.builder();
    final InvocationConfiguration invocationConfiguration = builder.withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .withInputOrder(OrderType.SORTED)
                                                                   .withInputBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withInputMaxSize(100)
                                                                   .apply();
    assertThat(builderFromInput(configuration).apply()).isEqualTo(invocationConfiguration);
  }

  @Test
  public void testFromOutputChannelConfiguration() {

    final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                   .withOrder(OrderType.SORTED)
                                                                   .withBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withMaxSize(100)
                                                                   .withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .apply();
    final InvocationConfiguration.Builder<InvocationConfiguration> builder =
        InvocationConfiguration.builder();
    final InvocationConfiguration invocationConfiguration = builder.withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .withOutputOrder(
                                                                       OrderType.SORTED)
                                                                   .withOutputBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withOutputMaxSize(100)
                                                                   .apply();
    assertThat(builderFromOutput(configuration).apply()).isEqualTo(invocationConfiguration);
  }

  @Test
  public void testInputBackoffEquals() {

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                           .withRunner(Runners.syncRunner())
                                                           .withLog(new NullLog())
                                                           .withOutputMaxSize(100)
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withInputBackoff(noDelay()).apply());
    assertThat(configuration).isNotEqualTo(
        builder().withInputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).apply());
    assertThat(configuration.builderFrom()
                            .withInputBackoff(afterCount(1).constantDelay(millis(1)))
                            .apply()).isNotEqualTo(
        builder().withInputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).apply());
  }

  @Test
  public void testInputChannelConfiguration() {

    final InvocationConfiguration.Builder<InvocationConfiguration> builder =
        InvocationConfiguration.builder();
    final InvocationConfiguration invocationConfiguration = builder.withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .withInputOrder(OrderType.SORTED)
                                                                   .withInputBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withInputMaxSize(100)
                                                                   .apply();
    final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                   .withOrder(OrderType.SORTED)
                                                                   .withBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withMaxSize(100)
                                                                   .withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .apply();
    assertThat(invocationConfiguration.inputConfigurationBuilder().apply()).isEqualTo(
        configuration);
  }

  @Test
  public void testInputOrderEquals() {

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                           .withRunner(Runners.syncRunner())
                                                           .withLog(new NullLog())
                                                           .withOutputMaxSize(100)
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withInputOrder(OrderType.UNSORTED).apply());
    assertThat(configuration.builderFrom().withInputOrder(OrderType.SORTED).apply()).isNotEqualTo(
        builder().withInputOrder(OrderType.SORTED).apply());
  }

  @Test
  public void testInputSizeEquals() {

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
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

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
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

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
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

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                           .withRunner(Runners.syncRunner())
                                                           .withLog(new NullLog())
                                                           .withOutputMaxSize(100)
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withMaxInvocations(4).apply());
    assertThat(configuration.builderFrom().withMaxInvocations(41).apply()).isNotEqualTo(
        builder().withMaxInvocations(41).apply());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testMaxInvocationsError() {

    try {

      builder().withMaxInvocations(0);

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
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withOutputBackoff(noDelay()).apply());
    assertThat(configuration).isNotEqualTo(
        builder().withOutputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).apply());
    assertThat(configuration.builderFrom()
                            .withOutputBackoff(afterCount(1).constantDelay(millis(1)))
                            .apply()).isNotEqualTo(
        builder().withOutputBackoff(afterCount(1).constantDelay(1, TimeUnit.MILLISECONDS)).apply());
  }

  @Test
  public void testOutputChannelConfiguration() {

    final InvocationConfiguration.Builder<InvocationConfiguration> builder =
        InvocationConfiguration.builder();
    final InvocationConfiguration invocationConfiguration = builder.withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .withOutputOrder(
                                                                       OrderType.SORTED)
                                                                   .withOutputBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withOutputMaxSize(100)
                                                                   .apply();
    final ChannelConfiguration configuration = ChannelConfiguration.builder()
                                                                   .withOrder(OrderType.SORTED)
                                                                   .withBackoff(
                                                                       afterCount(1).constantDelay(
                                                                           millis(33)))
                                                                   .withMaxSize(100)
                                                                   .withRunner(Runners.syncRunner())
                                                                   .withOutputTimeout(millis(100))
                                                                   .withOutputTimeoutAction(
                                                                       TimeoutActionType.ABORT)
                                                                   .withLog(Logs.nullLog())
                                                                   .withLogLevel(Level.SILENT)
                                                                   .apply();
    assertThat(invocationConfiguration.outputConfigurationBuilder().apply()).isEqualTo(
        configuration);
  }

  @Test
  public void testOutputOrderEquals() {

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
                                                           .withRunner(Runners.syncRunner())
                                                           .withLog(new NullLog())
                                                           .withOutputMaxSize(100)
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withOutputOrder(OrderType.UNSORTED).apply());
    assertThat(configuration.builderFrom().withOutputOrder(OrderType.SORTED).apply()).isNotEqualTo(
        builder().withOutputOrder(OrderType.SORTED).apply());
  }

  @Test
  public void testOutputSizeEquals() {

    final InvocationConfiguration configuration = builder().withInputOrder(OrderType.SORTED)
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
                                                           .withCoreInvocations(27)
                                                           .withInputOrder(OrderType.SORTED)
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
                                                           .withInputOrder(OrderType.SORTED)
                                                           .withRunner(Runners.syncRunner())
                                                           .withLog(new NullLog())
                                                           .withOutputMaxSize(100)
                                                           .apply();
    assertThat(configuration).isNotEqualTo(builder().withRunner(Runners.sharedRunner()).apply());
    assertThat(configuration.builderFrom().withRunner(Runners.syncRunner()).apply()).isNotEqualTo(
        builder().withRunner(Runners.syncRunner()).apply());
  }
}
