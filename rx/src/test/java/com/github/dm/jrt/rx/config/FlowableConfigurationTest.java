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
        builder().withBackpressure(BackpressureStrategy.MISSING).apply();
    assertThat(configuration).isNotEqualTo(
        builder().withBackpressure(BackpressureStrategy.DROP).apply());
    assertThat(
        configuration.builderFrom().withBackpressure(BackpressureStrategy.DROP).apply()).isEqualTo(
        FlowableConfiguration.<String>builder().withBackpressure(BackpressureStrategy.DROP)
                                               .apply());
    assertThat(configuration).isNotEqualTo(
        FlowableConfiguration.<String>builder().withBackpressure(BackpressureStrategy.DROP)
                                               .apply());
  }

  @Test
  public void testBuildFrom() {
    final FlowableConfiguration configuration =
        builder().withBackpressure(BackpressureStrategy.MISSING).apply();
    assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().apply().hashCode()).isEqualTo(configuration.hashCode());
    assertThat(builderFrom(null).apply()).isEqualTo(FlowableConfiguration.defaultConfiguration());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testBuildNullPointerError() {
    try {
      new Builder<Object, Object>(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new Builder<Object, Object>(null, FlowableConfiguration.defaultConfiguration());
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testBuilderFromEquals() {
    final FlowableConfiguration<String> configuration =
        FlowableConfiguration.<String>builder().withInput("test").apply();
    assertThat(FlowableConfiguration.<String>builder().withPatch(configuration).apply()).isEqualTo(
        configuration);
    assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withPatch(null).apply()).isEqualTo(
        FlowableConfiguration.defaultConfiguration());
  }

  @Test
  public void testInputsEquals() {
    final FlowableConfiguration<String> configuration =
        FlowableConfiguration.<String>builder().withInputs("test", "test").apply();
    assertThat(configuration).isNotEqualTo(
        FlowableConfiguration.<String>builder().withInput("test").apply());
    assertThat(configuration.builderFrom().withInput("test").apply()).isEqualTo(
        FlowableConfiguration.<String>builder().withInput("test").apply());
    assertThat(configuration).isNotEqualTo(
        FlowableConfiguration.<String>builder().withInput("test").apply());
  }

  @Test
  public void testToString() {
    assertThat(builder().withInput("test123").apply().toString()).contains("test123");
  }
}
