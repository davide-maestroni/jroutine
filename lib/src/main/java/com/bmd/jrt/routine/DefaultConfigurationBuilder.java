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
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import javax.annotation.Nonnull;

import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.seconds;

/**
 * Configuration builder pre-initialized with the default routine configuration.
 * <p/>
 * Created by davide on 11/15/14.
 */
class DefaultConfigurationBuilder extends RoutineConfigurationBuilder {

    private static final TimeDuration DEFAULT_AVAIL_TIMEOUT = seconds(5);

    private final Log mDefaultLog = Logger.getDefaultLog();

    private final LogLevel mDefaultLogLevel = Logger.getDefaultLogLevel();

    /**
     * Constructor.
     */
    DefaultConfigurationBuilder() {

    }

    @Nonnull
    @Override
    public RoutineConfiguration buildConfiguration() {

        final RoutineConfiguration configuration = super.buildConfiguration();
        final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();

        builder.availableTimeout(configuration.getAvailTimeout(DEFAULT_AVAIL_TIMEOUT));
        builder.inputMaxSize(configuration.getInputMaxSize(Integer.MAX_VALUE));
        builder.inputOrder(configuration.getInputOrder(DataOrder.DELIVERY));
        builder.inputTimeout(configuration.getInputTimeout(ZERO));
        builder.logLevel(configuration.getLogLevel(mDefaultLogLevel));
        builder.loggedWith(configuration.getLog(mDefaultLog));
        builder.maxRetained(configuration.getMaxRetained(10));
        builder.maxRunning(configuration.getMaxRunning(Integer.MAX_VALUE));
        builder.outputMaxSize(configuration.getOutputMaxSize(Integer.MAX_VALUE));
        builder.outputOrder(configuration.getOutputOrder(DataOrder.DELIVERY));
        builder.outputTimeout(configuration.getOutputTimeout(ZERO));
        builder.runBy(configuration.getRunner(Runners.sharedRunner()));
        builder.syncRunner(configuration.getSyncRunner(RunnerType.QUEUED));

        return builder.buildConfiguration();
    }
}
