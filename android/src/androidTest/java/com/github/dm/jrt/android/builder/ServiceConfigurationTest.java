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
package com.github.dm.jrt.android.builder;

import android.os.Looper;
import android.test.AndroidTestCase;

import com.github.dm.jrt.android.builder.ServiceConfiguration.Builder;
import com.github.dm.jrt.android.log.AndroidLog;
import com.github.dm.jrt.android.runner.MainRunner;
import com.github.dm.jrt.android.runner.Runners;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.log.SystemLog;
import com.github.dm.jrt.runner.RunnerDecorator;

import static com.github.dm.jrt.android.builder.ServiceConfiguration.builder;
import static com.github.dm.jrt.android.builder.ServiceConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service invocation configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 04/23/15.
 */
public class ServiceConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .set();

        assertThat(builderFrom(configuration).set()).isEqualTo(configuration);
        assertThat(builderFrom(null).set()).isEqualTo(ServiceConfiguration.DEFAULT_CONFIGURATION);
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuildNullPointerError() {

        try {

            new Builder<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new Builder<Object>(null, ServiceConfiguration.DEFAULT_CONFIGURATION);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testBuilderFromEquals() {

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                ServiceConfiguration.DEFAULT_CONFIGURATION);
    }

    public void testConfigurationErrors() {

        try {

            builder().withRunnerClass(Runners.looperRunner(Looper.getMainLooper()).getClass());

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
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withLogClass(SystemLog.class).set());
        assertThat(builder().withLogClass(SystemLog.class).set()).isNotEqualTo(
                builder().withLogClass(NullLog.class).set());
    }

    public void testLooperEquals() {

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withResultLooper(Looper.myLooper()).set());
        assertThat(builder().withResultLooper(Looper.myLooper()).set()).isNotEqualTo(
                builder().withResultLooper(Looper.getMainLooper()).set());
    }

    public void testRunnerClassEquals() {

        final ServiceConfiguration configuration =
                builder().withResultLooper(Looper.getMainLooper())
                         .withRunnerClass(MainRunner.class)
                         .withLogClass(AndroidLog.class)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withRunnerClass(TestRunner.class).set());
        assertThat(builder().withRunnerClass(TestRunner.class).set()).isNotEqualTo(
                builder().withRunnerClass(MainRunner.class).set());
    }

    public static class TestRunner extends RunnerDecorator {

        public TestRunner() {

            super(Runners.sharedRunner());
        }
    }

    private class MyLog extends NullLog {

    }
}
