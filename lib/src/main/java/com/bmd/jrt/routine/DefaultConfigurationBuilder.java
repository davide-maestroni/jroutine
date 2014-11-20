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
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.runner.Runners;

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

        syncRunner(SyncRunnerType.QUEUED);
        runBy(Runners.poolRunner()); //TODO: too many thread!!!!
        availableTimeout(seconds(5));
        maxRunning(Integer.MAX_VALUE);
        maxRetained(10);
        inputMaxSize(Integer.MAX_VALUE);
        inputTimeout(ZERO);
        inputOrder(ChannelDataOrder.DELIVERY);
        outputMaxSize(Integer.MAX_VALUE);
        outputTimeout(ZERO);
        outputOrder(ChannelDataOrder.DELIVERY);
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

        apply(initialConfiguration);
    }
}
