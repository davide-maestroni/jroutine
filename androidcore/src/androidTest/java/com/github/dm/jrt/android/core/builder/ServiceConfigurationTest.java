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
import com.github.dm.jrt.android.core.runner.AndroidRunners;
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

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .setConfiguration();

        assertThat(builderFrom(configuration).setConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).setConfiguration()).isEqualTo(
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

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .setConfiguration();
        assertThat(builder().with(configuration).setConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().setConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).setConfiguration()).isEqualTo(
                ServiceConfiguration.defaultConfiguration());
    }

    public void testConfigurationErrors() {

        try {

            builder().withRunnerClass(
                    AndroidRunners.looperRunner(Looper.getMainLooper()).getClass());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            builder().withLogClass(MyLog.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testLogClassEquals() {

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withLogClass(SystemLog.class).setConfiguration());
        assertThat(builder().withLogClass(SystemLog.class).setConfiguration()).isNotEqualTo(
                builder().withLogClass(NullLog.class).setConfiguration());
    }

    public void testLooperEquals() {

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withResultLooper(Looper.myLooper()).setConfiguration());
        assertThat(builder().withResultLooper(Looper.myLooper()).setConfiguration()).isNotEqualTo(
                builder().withResultLooper(Looper.getMainLooper()).setConfiguration());
    }

    public void testRunnerClassEquals() {

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withRunnerClass(TestRunner.class).setConfiguration());
        assertThat(builder().withRunnerClass(TestRunner.class).setConfiguration()).isNotEqualTo(
                builder().withRunnerClass(MainRunner.class).setConfiguration());
    }

    public static class TestRunner extends RunnerDecorator {

        public TestRunner() {

            super(Runners.sharedRunner());
        }
    }

    private class MyLog extends NullLog {

    }
}
