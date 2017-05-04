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

package com.github.dm.jrt.reflect.util;

import com.github.dm.jrt.reflect.config.WrapperConfiguration;
import com.github.dm.jrt.reflect.config.WrapperConfiguration.Builder;

import org.junit.Test;

import static com.github.dm.jrt.reflect.config.WrapperConfiguration.builder;
import static com.github.dm.jrt.reflect.config.WrapperConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Wrapper configuration unit tests.
 * <p>
 * Created by davide-maestroni on 04/21/2015.
 */
public class WrapperConfigurationTest {

  @Test
  public void testBuildFrom() {

    final WrapperConfiguration configuration = builder().withSharedFields("test").configuration();
    assertThat(configuration.builderFrom().configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configuration().hashCode()).isEqualTo(
        configuration.hashCode());
    assertThat(builderFrom(null).configuration()).isEqualTo(
        WrapperConfiguration.defaultConfiguration());
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

      new Builder<Object>(null, WrapperConfiguration.defaultConfiguration());

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testBuilderFromEquals() {

    final WrapperConfiguration configuration = builder().withSharedFields("test").configuration();
    assertThat(builder().withPatch(configuration).configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withPatch(null).configuration()).isEqualTo(
        configuration);
    assertThat(configuration.builderFrom().withDefaults().configuration()).isEqualTo(
        WrapperConfiguration.defaultConfiguration());
  }

  @Test
  public void testConstructor() {

    boolean failed = false;
    try {
      new InvocationReflection();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testSharedFieldsEquals() {

    final WrapperConfiguration configuration = builder().withSharedFields("group").configuration();
    assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").configuration());
    assertThat(configuration.builderFrom().withSharedFields("test").configuration()).isEqualTo(
        builder().withSharedFields("test").configuration());
    assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").configuration());
  }

  @Test
  public void testToString() {

    assertThat(builder().withSharedFields("testGroupName123").configuration().toString()).contains(
        "testGroupName123");
  }
}
