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

import com.github.dm.jrt.rx.config.ObservableConfiguration.Builder;

import org.junit.Test;

import rx.Emitter.BackpressureMode;

import static com.github.dm.jrt.rx.config.ObservableConfiguration.builder;
import static com.github.dm.jrt.rx.config.ObservableConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Observable configuration unit tests.
 * <p>
 * Created by davide-maestroni on 02/08/2017.
 */
public class ObservableConfigurationTest {

  @Test
  public void testBackpressureEquals() {
    final ObservableConfiguration configuration =
        builder().withBackpressure(BackpressureMode.NONE).apply();
    assertThat(configuration).isNotEqualTo(
        builder().withBackpressure(BackpressureMode.DROP).apply());
    assertThat(
        configuration.builderFrom().withBackpressure(BackpressureMode.DROP).apply()).isEqualTo(
        ObservableConfiguration.<String>builder().withBackpressure(BackpressureMode.DROP).apply());
    assertThat(configuration).isNotEqualTo(
        ObservableConfiguration.<String>builder().withBackpressure(BackpressureMode.DROP).apply());
  }

  @Test
  public void testBuildFrom() {
    final ObservableConfiguration configuration =
        builder().withBackpressure(BackpressureMode.NONE).apply();
    assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().apply().hashCode()).isEqualTo(configuration.hashCode());
    assertThat(builderFrom(null).apply()).isEqualTo(ObservableConfiguration.defaultConfiguration());
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
      new Builder<Object, Object>(null, ObservableConfiguration.defaultConfiguration());
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testBuilderFromEquals() {
    final ObservableConfiguration<String> configuration =
        ObservableConfiguration.<String>builder().withInput("test").apply();
    assertThat(ObservableConfiguration.<String>builder().with(configuration).apply()).isEqualTo(
        configuration);
    assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().with(null).apply()).isEqualTo(
        ObservableConfiguration.defaultConfiguration());
  }

  @Test
  public void testInputsEquals() {
    final ObservableConfiguration<String> configuration =
        ObservableConfiguration.<String>builder().withInputs("test", "test").apply();
    assertThat(configuration).isNotEqualTo(
        ObservableConfiguration.<String>builder().withInput("test").apply());
    assertThat(configuration.builderFrom().withInput("test").apply()).isEqualTo(
        ObservableConfiguration.<String>builder().withInput("test").apply());
    assertThat(configuration).isNotEqualTo(
        ObservableConfiguration.<String>builder().withInput("test").apply());
  }

  @Test
  public void testToString() {
    assertThat(builder().withInput("test123").apply().toString()).contains("test123");
  }
}
