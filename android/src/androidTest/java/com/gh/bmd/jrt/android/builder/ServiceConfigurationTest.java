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
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation configuration unit tests.
 * <p/>
 * Created by davide on 23/04/15.
 */
public class ServiceConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final ServiceConfiguration configuration = builder().dispatchingOn(Looper.getMainLooper())
                                                            .withServiceClass(TestService.class)
                                                            .withRunnerClass(MainRunner.class)
                                                            .withLogClass(AndroidLog.class)
                                                            .applied();

        assertThat(builderFrom(configuration).applied()).isEqualTo(configuration);
        assertThat(builderFrom(null).applied()).isEqualTo(
                ServiceConfiguration.DEFAULT_CONFIGURATION);
    }

    public void testBuilderFromEquals() {

        final ServiceConfiguration configuration = builder().dispatchingOn(Looper.getMainLooper())
                                                            .withServiceClass(TestService.class)
                                                            .withRunnerClass(MainRunner.class)
                                                            .withLogClass(AndroidLog.class)
                                                            .applied();
        assertThat(builder().with(configuration).applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).applied()).isEqualTo(
                ServiceConfiguration.DEFAULT_CONFIGURATION);
    }

    public void testLogClassEquals() {

        final ServiceConfiguration configuration = builder().dispatchingOn(Looper.getMainLooper())
                                                            .withServiceClass(TestService.class)
                                                            .withRunnerClass(MainRunner.class)
                                                            .withLogClass(AndroidLog.class)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(builder().withLogClass(SystemLog.class).applied());
        assertThat(builder().withLogClass(SystemLog.class).applied()).isNotEqualTo(
                builder().withLogClass(NullLog.class).applied());
    }

    public void testLooperEquals() {

        final ServiceConfiguration configuration = builder().dispatchingOn(Looper.getMainLooper())
                                                            .withServiceClass(TestService.class)
                                                            .withRunnerClass(MainRunner.class)
                                                            .withLogClass(AndroidLog.class)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(
                builder().dispatchingOn(Looper.myLooper()).applied());
        assertThat(builder().dispatchingOn(Looper.myLooper()).applied()).isNotEqualTo(
                builder().dispatchingOn(Looper.getMainLooper()).applied());
    }

    public void testRunnerClassEquals() {

        final ServiceConfiguration configuration = builder().dispatchingOn(Looper.getMainLooper())
                                                            .withServiceClass(TestService.class)
                                                            .withRunnerClass(MainRunner.class)
                                                            .withLogClass(AndroidLog.class)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withRunnerClass(TestRunner.class).applied());
        assertThat(builder().withRunnerClass(TestRunner.class).applied()).isNotEqualTo(
                builder().withRunnerClass(MainRunner.class).applied());
    }

    public void testServiceClassEquals() {

        final ServiceConfiguration configuration = builder().dispatchingOn(Looper.getMainLooper())
                                                            .withServiceClass(TestService.class)
                                                            .withRunnerClass(MainRunner.class)
                                                            .withLogClass(AndroidLog.class)
                                                            .applied();
        assertThat(configuration).isNotEqualTo(
                builder().withServiceClass(RoutineService.class).applied());
        assertThat(builder().withServiceClass(RoutineService.class).applied()).isNotEqualTo(
                builder().withServiceClass(TestService.class).applied());
    }

    public static class TestRunner extends RunnerDecorator {

        public TestRunner() {

            super(Runners.sharedRunner());
        }
    }
}
