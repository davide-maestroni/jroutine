/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.builder;

import android.os.Looper;
import android.test.AndroidTestCase;

import com.gh.bmd.jrt.android.core.TestService;
import com.gh.bmd.jrt.android.log.AndroidLog;
import com.gh.bmd.jrt.android.runner.MainRunner;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.log.SystemLog;
import com.gh.bmd.jrt.runner.RunnerDecorator;
import com.gh.bmd.jrt.runner.Runners;

import static com.gh.bmd.jrt.android.builder.ServiceConfiguration.builder;
import static com.gh.bmd.jrt.android.builder.ServiceConfiguration.builderFrom;
import static com.gh.bmd.jrt.android.builder.ServiceConfiguration.dispatchingOn;
import static com.gh.bmd.jrt.android.builder.ServiceConfiguration.withLogClass;
import static com.gh.bmd.jrt.android.builder.ServiceConfiguration.withRunnerClass;
import static com.gh.bmd.jrt.android.builder.ServiceConfiguration.withServiceClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation configuration unit tests.
 * <p/>
 * Created by davide on 23/04/15.
 */
public class ServiceConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final ServiceConfiguration configuration =
                dispatchingOn(Looper.getMainLooper()).withServiceClass(TestService.class)
                                                     .withRunnerClass(MainRunner.class)
                                                     .withLogClass(AndroidLog.class)
                                                     .buildConfiguration();

        assertThat(builderFrom(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).buildConfiguration()).isEqualTo(
                ServiceConfiguration.EMPTY_CONFIGURATION);
    }

    public void testBuilderFromEquals() {

        final ServiceConfiguration configuration =
                dispatchingOn(Looper.getMainLooper()).withServiceClass(TestService.class)
                                                     .withRunnerClass(MainRunner.class)
                                                     .withLogClass(AndroidLog.class)
                                                     .buildConfiguration();
        assertThat(builder().apply(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply(null).buildConfiguration()).isEqualTo(
                configuration);
    }

    public void testLogClassEquals() {

        assertThat(withLogClass(SystemLog.class).buildConfiguration()).isEqualTo(
                builder().withLogClass(SystemLog.class).buildConfiguration());

        final ServiceConfiguration configuration =
                dispatchingOn(Looper.getMainLooper()).withServiceClass(TestService.class)
                                                     .withRunnerClass(MainRunner.class)
                                                     .withLogClass(AndroidLog.class)
                                                     .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withLogClass(SystemLog.class).buildConfiguration());
        assertThat(withLogClass(SystemLog.class).buildConfiguration()).isNotEqualTo(
                withLogClass(NullLog.class).buildConfiguration());
    }

    public void testLooperEquals() {

        assertThat(dispatchingOn(Looper.myLooper()).buildConfiguration()).isEqualTo(
                builder().dispatchingOn(Looper.myLooper()).buildConfiguration());

        final ServiceConfiguration configuration =
                dispatchingOn(Looper.getMainLooper()).withServiceClass(TestService.class)
                                                     .withRunnerClass(MainRunner.class)
                                                     .withLogClass(AndroidLog.class)
                                                     .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                dispatchingOn(Looper.myLooper()).buildConfiguration());
        assertThat(dispatchingOn(Looper.myLooper()).buildConfiguration()).isNotEqualTo(
                dispatchingOn(Looper.getMainLooper()).buildConfiguration());
    }

    public void testRunnerClassEquals() {

        assertThat(withRunnerClass(TestRunner.class).buildConfiguration()).isEqualTo(
                builder().withRunnerClass(TestRunner.class).buildConfiguration());

        final ServiceConfiguration configuration =
                dispatchingOn(Looper.getMainLooper()).withServiceClass(TestService.class)
                                                     .withRunnerClass(MainRunner.class)
                                                     .withLogClass(AndroidLog.class)
                                                     .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withRunnerClass(TestRunner.class).buildConfiguration());
        assertThat(withRunnerClass(TestRunner.class).buildConfiguration()).isNotEqualTo(
                withRunnerClass(MainRunner.class).buildConfiguration());
    }

    public void testServiceClassEquals() {

        assertThat(withServiceClass(RoutineService.class).buildConfiguration()).isEqualTo(
                builder().withServiceClass(RoutineService.class).buildConfiguration());

        final ServiceConfiguration configuration =
                dispatchingOn(Looper.getMainLooper()).withServiceClass(TestService.class)
                                                     .withRunnerClass(MainRunner.class)
                                                     .withLogClass(AndroidLog.class)
                                                     .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                withServiceClass(RoutineService.class).buildConfiguration());
        assertThat(withServiceClass(RoutineService.class).buildConfiguration()).isNotEqualTo(
                withServiceClass(TestService.class).buildConfiguration());
    }

    public static class TestRunner extends RunnerDecorator {

        public TestRunner() {

            super(Runners.sharedRunner());
        }
    }
}
