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

package com.github.dm.jrt.rx.config;

import com.github.dm.jrt.rx.config.FlowableConfiguration.Builder;

import org.junit.Test;

import io.reactivex.BackpressureStrategy;

import static com.github.dm.jrt.rx.config.FlowableConfiguration.builder;
import static com.github.dm.jrt.rx.config.FlowableConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Flowable configuration unit tests.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
public class FlowableConfigurationTest {

  @Test
  public void testBackpressureEquals() {
    final FlowableConfiguration configuration =
        builder().withBackpressure(BackpressureStrategy.MISSING).configuration();
    assertThat(configuration).isNotEqualTo(
        builder().withBackpressure(BackpressureStrategy.DROP).configuration());
    assertThat(configuration.builderFrom()
                            .withBackpressure(BackpressureStrategy.DROP)
                            .configuration()).isEqualTo(
        builder().withBackpressure(BackpressureStrategy.DROP).configuration());
    assertThat(configuration).isNotEqualTo(
        builder().withBackpressure(BackpressureStrategy.DROP).configuration());
  }

  @Test
  public void testBuildFrom() {
    final FlowableConfiguration configuration =
        builder().withBackpressure(BackpressureStrategy.MISSING).configuration();
    assertThat(configuration.builderFrom().configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configuration().hashCode()).isEqualTo(
        configuration.hashCode());
    assertThat(builderFrom(null).configuration()).isEqualTo(
        FlowableConfiguration.defaultConfiguration());
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
      new Builder<Object>(null, FlowableConfiguration.defaultConfiguration());
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testBuilderFromEquals() {
    final FlowableConfiguration configuration =
        builder().withBackpressure(BackpressureStrategy.BUFFER).configuration();
    assertThat(builder().withPatch(configuration).configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withPatch(null).configuration()).isEqualTo(
        configuration);
    assertThat(configuration.builderFrom().withDefaults().configuration()).isEqualTo(
        FlowableConfiguration.defaultConfiguration());
  }
}
