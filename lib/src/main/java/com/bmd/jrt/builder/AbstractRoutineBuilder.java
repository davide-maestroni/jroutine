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

import static com.bmd.jrt.builder.RoutineConfiguration.NOT_SET;

/**
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

        final SyncRunnerType syncRunner = configuration.getSyncRunner(SyncRunnerType.DEFAULT);

        if (syncRunner != SyncRunnerType.DEFAULT) {

            syncRunner(syncRunner);
        }

        final int maxRunning = configuration.getMaxRunning(NOT_SET);

        if (maxRunning != NOT_SET) {

            maxRunning(maxRunning);
        }

        final int maxRetained = configuration.getMaxRetained(NOT_SET);

        if (maxRetained != NOT_SET) {

            maxRetained(maxRetained);
        }

        final TimeDuration availTimeout = configuration.getAvailTimeout(null);

        if (availTimeout != null) {

            availableTimeout(availTimeout);
        }

        final int inputMaxSize = configuration.getInputMaxSize(NOT_SET);

        if (inputMaxSize != NOT_SET) {

            inputMaxSize(inputMaxSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeout(null);

        if (inputTimeout != null) {

            inputTimeout(inputTimeout);
        }

        final ChannelDataOrder inputOrder = configuration.getInputOrder(ChannelDataOrder.DEFAULT);

        if (inputOrder != ChannelDataOrder.DEFAULT) {

            inputOrder(inputOrder);
        }

        final int outputMaxSize = configuration.getOutputMaxSize(NOT_SET);

        if (outputMaxSize != NOT_SET) {

            outputMaxSize(outputMaxSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeout(null);

        if (outputTimeout != null) {

            outputTimeout(outputTimeout);
        }

        final ChannelDataOrder outputOrder = configuration.getOutputOrder(ChannelDataOrder.DEFAULT);

        if (outputOrder != ChannelDataOrder.DEFAULT) {

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
