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
package com.bmd.jrt.routine;

import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.execution.BasicExecution;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Routine builder unit tests.
 * <p/>
 * Created by davide on 10/16/14.
 */
public class JavaRoutineTest extends TestCase {

    public void testRoutineBuilder() {

        final Routine<String, String> routine =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughExecution.class))
                           .sequential()
                           .runBy(Runners.pool())
                           .maxRetained(0)
                           .maxRunning(1)
                           .availableTimeout(1, TimeUnit.SECONDS)
                           .buildRoutine();

        assertThat(routine.call("test1", "test2")).containsExactly("test1", "test2");

        final Routine<String, String> routine1 =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughExecution.class))
                           .queued()
                           .runBy(Runners.pool())
                           .maxRetained(0)
                           .maxRunning(1)
                           .availableTimeout(TimeDuration.ZERO)
                           .buildRoutine();

        assertThat(routine1.call("test1", "test2")).containsExactly("test1", "test2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        try {

            new RoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).availableTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).runBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).withArgs((Object[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static class PassThroughExecution extends BasicExecution<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

            results.pass(s);
        }
    }
}