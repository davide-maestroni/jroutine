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
package com.gh.bmd.jrt.android.runner;

import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.RunnerDecorator;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Implementation of a runner employing the main UI thread looper.<br/>
 * Note that, when the invocation runs in the main thread, the executions with a delay of 0 will be
 * performed synchronously, while the ones with a positive delay will be posted on the UI thread.
 * <p/>
 * Created by davide on 12/17/14.
 */
public class MainRunner extends RunnerDecorator {

    private static final Runner sMainRunner = Runners.mainRunner(new Runner() {

        private final Runner mMain = Runners.mainRunner(null);

        private final Runner mQueued = Runners.queuedRunner();

        @Override
        public void run(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            if (delay == 0) {

                mQueued.run(execution, delay, timeUnit);

            } else {

                mMain.run(execution, delay, timeUnit);
            }
        }
    });

    /**
     * Constructor.
     */
    public MainRunner() {

        super(sMainRunner);
    }
}
