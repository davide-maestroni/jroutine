/**
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
package com.bmd.jrt.builder;

import com.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.bmd.jrt.builder.RoutineConfiguration.RunnerType;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logs;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.builder.RoutineConfiguration.OrderBy;
import static com.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.bmd.jrt.builder.RoutineConfiguration.builderFrom;
import static com.bmd.jrt.builder.RoutineConfiguration.withAvailableTimeout;
import static com.bmd.jrt.builder.RoutineConfiguration.withCoreInvocations;
import static com.bmd.jrt.builder.RoutineConfiguration.withInputOrder;
import static com.bmd.jrt.builder.RoutineConfiguration.withInputSize;
import static com.bmd.jrt.builder.RoutineConfiguration.withInputTimeout;
import static com.bmd.jrt.builder.RoutineConfiguration.withLog;
import static com.bmd.jrt.builder.RoutineConfiguration.withLogLevel;
import static com.bmd.jrt.builder.RoutineConfiguration.withMaxInvocations;
import static com.bmd.jrt.builder.RoutineConfiguration.withOutputOrder;
import static com.bmd.jrt.builder.RoutineConfiguration.withOutputSize;
import static com.bmd.jrt.builder.RoutineConfiguration.withOutputTimeout;
import static com.bmd.jrt.builder.RoutineConfiguration.withReadTimeout;
import static com.bmd.jrt.builder.RoutineConfiguration.withRunner;
import static com.bmd.jrt.builder.RoutineConfiguration.withSyncRunner;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Configuration builder unit tests.
 * <p/>
 * Created by davide on 11/22/14.
 */
public class RoutineConfigurationBuilderTest extends TestCase {

    @SuppressWarnings("ConstantConditions")
    public void testBuilderError() {

        try {

            builderFrom(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withConfiguration(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withInputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            RoutineConfiguration.withInputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withInputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            RoutineConfiguration.withInputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            builder().withInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            RoutineConfiguration.withInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            builder().withOutputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            withOutputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withOutputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withOutputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            builder().withOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            builder().withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            builder().withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testEquals() {

        final Builder builder = builder();
        builder.withAvailableTimeout(TimeDuration.millis(100))
               .withInputOrder(OrderBy.PASSING)
               .withRunner(Runners.queuedRunner())
               .withLog(new NullLog())
               .withOutputSize(100);

        final RoutineConfiguration configuration = builder.buildConfiguration();
        assertThat(builder().withConfiguration(configuration).buildConfiguration()).isEqualTo(
                configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);

        assertThat(withAvailableTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withAvailableTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withAvailableTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withAvailableTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withCoreInvocations(3)).isEqualTo(
                builder().withCoreInvocations(3).buildConfiguration());
        assertThat(withInputOrder(OrderBy.PASSING)).isEqualTo(
                builder().withInputOrder(OrderBy.PASSING).buildConfiguration());
        assertThat(withInputSize(10)).isEqualTo(builder().withInputSize(10).buildConfiguration());
        assertThat(withInputTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withInputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withInputTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withLog(Logs.nullLog())).isEqualTo(
                builder().withLog(Logs.nullLog()).buildConfiguration());
        assertThat(withLogLevel(LogLevel.DEBUG)).isEqualTo(
                builder().withLogLevel(LogLevel.DEBUG).buildConfiguration());
        assertThat(withMaxInvocations(4)).isEqualTo(
                builder().withMaxInvocations(4).buildConfiguration());
        assertThat(withOutputOrder(OrderBy.DELIVERY)).isEqualTo(
                builder().withOutputOrder(OrderBy.DELIVERY).buildConfiguration());
        assertThat(withOutputSize(10)).isEqualTo(builder().withOutputSize(10).buildConfiguration());
        assertThat(withOutputTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withOutputTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withOutputTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withReadTimeout(TimeDuration.ZERO)).isEqualTo(
                builder().withReadTimeout(TimeDuration.ZERO).buildConfiguration());
        assertThat(withReadTimeout(1, TimeUnit.MILLISECONDS)).isEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).buildConfiguration());
        assertThat(withRunner(Runners.sharedRunner())).isEqualTo(
                builder().withRunner(Runners.sharedRunner()).buildConfiguration());
        assertThat(withSyncRunner(RunnerType.QUEUED)).isEqualTo(
                builder().withSyncRunner(RunnerType.QUEUED).buildConfiguration());
    }
}
