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

import com.bmd.jrt.builder.RoutineBuilder.DataOrder;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

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

            new RoutineConfigurationBuilder().availableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().availableTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().inputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().inputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().inputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().inputOrder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().outputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().outputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().outputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().outputOrder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineConfigurationBuilder().syncRunner(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testEquals() {

        final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
        builder.availableTimeout(TimeDuration.millis(100))
               .inputOrder(DataOrder.INSERTION)
               .runBy(Runners.queuedRunner())
               .loggedWith(new NullLog())
               .outputSize(100);

        final RoutineConfiguration configuration = builder.buildConfiguration();
        assertThat(new RoutineConfigurationBuilder(configuration).buildConfiguration()).isEqualTo(
                configuration);
    }
}
