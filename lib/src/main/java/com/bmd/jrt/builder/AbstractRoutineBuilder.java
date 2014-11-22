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

import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import javax.annotation.Nonnull;

/**
 * Abstract implementation of a routine builder.
 * <p/>
 * Created by davide on 11/17/14.
 */
public abstract class AbstractRoutineBuilder implements RoutineBuilder {

    @Nonnull
    @Override
    public RoutineBuilder apply(@Nonnull final RoutineConfiguration configuration) {

        final Runner runner = configuration.getRunner(null);

        if (runner != null) {

            runBy(runner);
        }

        final RunnerType syncRunner = configuration.getSyncRunner(RunnerType.DEFAULT);

        if (syncRunner != RunnerType.DEFAULT) {

            syncRunner(syncRunner);
        }

        final int maxRunning = configuration.getMaxRunning(DEFAULT);

        if (maxRunning != DEFAULT) {

            maxRunning(maxRunning);
        }

        final int maxRetained = configuration.getMaxRetained(DEFAULT);

        if (maxRetained != DEFAULT) {

            maxRetained(maxRetained);
        }

        final TimeDuration availTimeout = configuration.getAvailTimeout(null);

        if (availTimeout != null) {

            availableTimeout(availTimeout);
        }

        final int inputMaxSize = configuration.getInputMaxSize(DEFAULT);

        if (inputMaxSize != DEFAULT) {

            inputMaxSize(inputMaxSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeout(null);

        if (inputTimeout != null) {

            inputTimeout(inputTimeout);
        }

        final DataOrder inputOrder = configuration.getInputOrder(DataOrder.DEFAULT);

        if (inputOrder != DataOrder.DEFAULT) {

            inputOrder(inputOrder);
        }

        final int outputMaxSize = configuration.getOutputMaxSize(DEFAULT);

        if (outputMaxSize != DEFAULT) {

            outputMaxSize(outputMaxSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeout(null);

        if (outputTimeout != null) {

            outputTimeout(outputTimeout);
        }

        final DataOrder outputOrder = configuration.getOutputOrder(DataOrder.DEFAULT);

        if (outputOrder != DataOrder.DEFAULT) {

            outputOrder(outputOrder);
        }

        final Log log = configuration.getLog(null);

        if (log != null) {

            loggedWith(log);
        }

        final LogLevel logLevel = configuration.getLogLevel(LogLevel.DEFAULT);

        if (logLevel != LogLevel.DEFAULT) {

            logLevel(logLevel);
        }

        return this;
    }
}
