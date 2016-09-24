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

package com.github.dm.jrt.android.core.builder;

import android.os.Looper;
import android.test.AndroidTestCase;

import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.config.ServiceConfiguration.Builder;
import com.github.dm.jrt.android.core.log.AndroidLog;
import com.github.dm.jrt.android.core.runner.MainRunner;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.log.SystemLog;
import com.github.dm.jrt.core.runner.RunnerDecorator;
import com.github.dm.jrt.core.runner.Runners;

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
                                                        .withRunnerClass(MainRunner.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configured();

    assertThat(builderFrom(configuration).configured()).isEqualTo(configuration);
    assertThat(builderFrom(null).configured()).isEqualTo(
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
                                                        .withRunnerClass(MainRunner.class)
                                                        .withRunnerArgs(1)
                                                        .withLogClass(AndroidLog.class)
                                                        .withLogArgs("test")
                                                        .configured();
    assertThat(builder().with(configuration).configured()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().with(null).configured()).isEqualTo(
        ServiceConfiguration.defaultConfiguration());
  }

  public void testLogArgsEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withRunnerClass(MainRunner.class)
                                                        .withLogArgs(1)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withLogArgs(1).configured());
    assertThat(builder().withLogArgs(1).configured()).isNotEqualTo(
        builder().withLogArgs().configured());
  }

  public void testLogClassEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withRunnerClass(MainRunner.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withLogClass(SystemLog.class).configured());
    assertThat(builder().withLogClass(SystemLog.class).configured()).isNotEqualTo(
        builder().withLogClass(NullLog.class).configured());
  }

  public void testLooperEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withRunnerClass(MainRunner.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(
        builder().withMessageLooper(Looper.myLooper()).configured());
    assertThat(builder().withMessageLooper(Looper.myLooper()).configured()).isNotEqualTo(
        builder().withMessageLooper(Looper.getMainLooper()).configured());
  }

  public void testRunnerArgsEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withRunnerArgs("test")
                                                        .withLogClass(AndroidLog.class)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(builder().withRunnerArgs("test").configured());
    assertThat(builder().withRunnerArgs("test").configured()).isNotEqualTo(
        builder().withRunnerArgs().configured());
  }

  public void testRunnerClassEquals() {

    final ServiceConfiguration configuration = builder().withMessageLooper(Looper.getMainLooper())
                                                        .withRunnerClass(MainRunner.class)
                                                        .withLogClass(AndroidLog.class)
                                                        .configured();
    assertThat(configuration).isNotEqualTo(
        builder().withRunnerClass(TestRunner.class).configured());
    assertThat(builder().withRunnerClass(TestRunner.class).configured()).isNotEqualTo(
        builder().withRunnerClass(MainRunner.class).configured());
  }

  public static class TestRunner extends RunnerDecorator {

    public TestRunner() {

      super(Runners.sharedRunner());
    }
  }
}
