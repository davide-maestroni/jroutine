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

import com.bmd.jrt.builder.RoutineChannelBuilder.OrderBy;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

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

            new RoutineConfigurationBuilder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withAvailableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withAvailableTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withInputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withInputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withOutputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withOutputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationError() {

        try {

            new RoutineConfiguration(null, null, 0, 0, null, null, null, 1, null, null, 1, null,
                                     null, null, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfiguration(null, null, 1, -1, null, null, null, 1, null, null, 1, null,
                                     null, null, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfiguration(null, null, 1, 0, null, null, null, 0, null, null, 1, null,
                                     null, null, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfiguration(null, null, 1, 0, null, null, null, 1, null, null, 0, null,
                                     null, null, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testEquals() {

        final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
        builder.withAvailableTimeout(TimeDuration.millis(100))
               .withInputOrder(OrderBy.INSERTION)
               .withRunner(Runners.queuedRunner())
               .withLog(new NullLog())
               .withOutputSize(100);

        final RoutineConfiguration configuration = builder.buildConfiguration();
        assertThat(new RoutineConfigurationBuilder(configuration).buildConfiguration()).isEqualTo(
                configuration);
    }
}
