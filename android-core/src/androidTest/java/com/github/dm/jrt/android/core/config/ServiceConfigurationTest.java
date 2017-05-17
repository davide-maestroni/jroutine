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

package com.github.dm.jrt.android.core.config;

import android.os.Looper;
import android.test.AndroidTestCase;

import com.github.dm.jrt.android.core.config.ServiceConfiguration.Builder;
import com.github.dm.jrt.android.core.executor.MainExecutor;
import com.github.dm.jrt.android.core.log.AndroidLog;
import com.github.dm.jrt.core.executor.ScheduledExecutorDecorator;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.log.SystemLog;

import static com.github.dm.jrt.android.core.config.ServiceConfiguration.builder;
import static com.github.dm.jrt.android.core.config.ServiceConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service invocation configuration unit tests.
 * <p>
 * Created by davide-maestroni on 04/23/2015.
 */
public class ServiceConfigurationTest extends AndroidTestCase {

  public void testBuildFrom() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withExecutorClass(MainExecutor.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configuration();

    assertThat(builderFrom(configuration).configuration()).isEqualTo(configuration);
    assertThat(builderFrom(null).configuration()).isEqualTo(
        ServiceConfiguration.defaultConfiguration());
  }

  @SuppressWarnings("ConstantConditions")
  public void testBuildNullPointerError() {

    try {

      new Builder<Object>(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      new Builder<Object>(null, ServiceConfiguration.defaultConfiguration());

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testBuilderFromEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withExecutorClass(MainExecutor.class)
                                                        .withExecutorArgs(1)
                                                        .withLogClass(AndroidLog.class)
                                                        .withLogArgs("test")
                                                        .configuration();
    assertThat(builder().withPatch(configuration).configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configuration()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().withPatch(null).configuration()).isEqualTo(
        configuration);
    assertThat(configuration.builderFrom().withDefaults().configuration()).isEqualTo(
        ServiceConfiguration.defaultConfiguration());
  }

  public void testExecutorArgsEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withExecutorArgs("test")
                                                        .withLogClass(AndroidLog.class)
                                                        .configuration();
    assertThat(configuration).isNotEqualTo(builder().withExecutorArgs("test").configuration());
    assertThat(builder().withExecutorArgs("test").configuration()).isNotEqualTo(
        builder().withExecutorArgs().configuration());
  }

  public void testExecutorClassEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withExecutorClass(MainExecutor.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configuration();
    assertThat(configuration).isNotEqualTo(
        builder().withExecutorClass(TestExecutor.class).configuration());
    assertThat(builder().withExecutorClass(TestExecutor.class).configuration()).isNotEqualTo(
        builder().withExecutorClass(MainExecutor.class).configuration());
  }

  public void testLogArgsEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withExecutorClass(MainExecutor.class)
                                                        .withLogArgs(1)
                                                        .configuration();
    assertThat(configuration).isNotEqualTo(builder().withLogArgs(1).configuration());
    assertThat(builder().withLogArgs(1).configuration()).isNotEqualTo(
        builder().withLogArgs().configuration());
  }

  public void testLogClassEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withExecutorClass(MainExecutor.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configuration();
    assertThat(configuration).isNotEqualTo(builder().withLogClass(SystemLog.class).configuration());
    assertThat(builder().withLogClass(SystemLog.class).configuration()).isNotEqualTo(
        builder().withLogClass(NullLog.class).configuration());
  }

  public void testLooperEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withExecutorClass(MainExecutor.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configuration();
    assertThat(configuration).isNotEqualTo(
        builder().withMessageLooper(Looper.myLooper()).configuration());
    assertThat(builder().withMessageLooper(Looper.myLooper()).configuration()).isNotEqualTo(
        builder().withMessageLooper(Looper.getMainLooper()).configuration());
  }

  public static class TestExecutor extends ScheduledExecutorDecorator {

    public TestExecutor() {

      super(ScheduledExecutors.defaultExecutor());
    }
  }
}
