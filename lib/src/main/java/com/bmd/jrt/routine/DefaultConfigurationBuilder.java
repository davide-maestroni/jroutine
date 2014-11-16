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

import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import static com.bmd.jrt.builder.RoutineConfiguration.NOT_SET;
import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.seconds;

/**
 * Configuration builder pre-initialized with the default routine configuration.
 * <p/>
 * Created by davide on 11/15/14.
 */
class DefaultConfigurationBuilder extends RoutineConfigurationBuilder {

    /**
     * Constructor.
     */
    DefaultConfigurationBuilder() {

        queued();
        runBy(Runners.poolRunner());
        availableTimeout(seconds(5));
        maxRunning(Integer.MAX_VALUE);
        maxRetained(10);
        maxInputSize(Integer.MAX_VALUE);
        inputTimeout(ZERO);
        delayedInput();
        maxOutputSize(Integer.MAX_VALUE);
        outputTimeout(ZERO);
        delayedOutput();
        loggedWith(Logger.getDefaultLog());
        logLevel(Logger.getDefaultLogLevel());
    }

    /**
     * Constructor.
     *
     * @param initialConfiguration the initial configuration.
     * @throws NullPointerException if the specified configuration instance is null.
     */
    DefaultConfigurationBuilder(final RoutineConfiguration initialConfiguration) {

        this();

        final Runner runner = initialConfiguration.getRunner(null);

        if (runner != null) {

            runBy(runner);
        }

        final Boolean isSequential = initialConfiguration.getIsSequential(null);

        if (isSequential != null) {

            if (isSequential) {

                sequential();

            } else {

                queued();
            }
        }

        final int maxRunning = initialConfiguration.getMaxRunning(NOT_SET);

        if (maxRunning != NOT_SET) {

            maxRunning(maxRunning);
        }

        final int maxRetained = initialConfiguration.getMaxRetained(NOT_SET);

        if (maxRetained != NOT_SET) {

            maxRetained(maxRetained);
        }

        final TimeDuration availTimeout = initialConfiguration.getAvailTimeout(null);

        if (availTimeout != null) {

            availableTimeout(availTimeout);
        }

        final int maxInputSize = initialConfiguration.getMaxInputSize(NOT_SET);

        if (maxInputSize != NOT_SET) {

            maxInputSize(maxInputSize);
        }

        final TimeDuration inputTimeout = initialConfiguration.getInputTimeout(null);

        if (inputTimeout != null) {

            inputTimeout(inputTimeout);
        }

        final Boolean orderedInput = initialConfiguration.getOrderedInput(null);

        if (orderedInput != null) {

            if (orderedInput) {

                orderedInput();

            } else {

                delayedInput();
            }
        }

        final int maxOutputSize = initialConfiguration.getMaxOutputSize(NOT_SET);

        if (maxOutputSize != NOT_SET) {

            maxOutputSize(maxOutputSize);
        }

        final TimeDuration outputTimeout = initialConfiguration.getOutputTimeout(null);

        if (outputTimeout != null) {

            outputTimeout(outputTimeout);
        }

        final Boolean orderedOutput = initialConfiguration.getOrderedOutput(null);

        if (orderedOutput != null) {

            if (orderedOutput) {

                orderedOutput();

            } else {

                delayedOutput();
            }
        }

        final Log log = initialConfiguration.getLog(null);

        if (log != null) {

            loggedWith(log);
        }

        final LogLevel logLevel = initialConfiguration.getLogLevel(null);

        if (logLevel != null) {

            logLevel(logLevel);
        }
    }
}
