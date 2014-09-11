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

import com.bmd.jrt.process.Processor;
import com.bmd.jrt.process.UnitProcessor;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.wtf.fll.Classification;

/**
 * Created by davide on 9/7/14.
 */
public class JRoutine {

    private static final int DEFAULT_INSTANCES = 10;

    private static volatile Runner sBackgroundRunner;

    private final int mInstanceCount;

    private final Runner mRunner;

    private JRoutine(final Runner runner, final int instanceCount) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (instanceCount < 0) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mInstanceCount = instanceCount;
    }

    public static JRoutine jrt() {

        if (sBackgroundRunner == null) {

            sBackgroundRunner = Runners.pool(getBestPoolSize());
        }

        return new JRoutine(sBackgroundRunner, DEFAULT_INSTANCES);
    }

    private static int getBestPoolSize() {

        final int processors = Runtime.getRuntime().availableProcessors();

        if (processors < 4) {

            return Math.max(1, processors - 1);
        }

        return (processors / 2);
    }

    public JRoutine in(final Runner runner) {

        return new JRoutine(runner, mInstanceCount);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> onExecute(
            final Classification<? extends Processor<INPUT, OUTPUT>> classification,
            final Object... args) {

        if (classification == null) {

            throw new IllegalArgumentException();
        }

        //TODO: avoid call inside processors?

        return new ProcessorRoutine<INPUT, OUTPUT>(mRunner, mInstanceCount,
                                                   classification.getRawType(), args);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> onProcess(
            final Classification<? extends UnitProcessor<INPUT, OUTPUT>> classification,
            final Object... args) {

        if (classification == null) {

            throw new IllegalArgumentException();
        }

        //TODO: avoid call inside processors?

        return new UnitProcessorRoutine<INPUT, OUTPUT>(mRunner, mInstanceCount,
                                                       classification.getRawType(), args);
    }

    public JRoutine retainInstances(final int instanceCount) {

        return new JRoutine(mRunner, instanceCount);
    }
}