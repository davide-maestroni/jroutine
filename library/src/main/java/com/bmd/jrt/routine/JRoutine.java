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

import com.bmd.jrt.procedure.LoopProcedure;
import com.bmd.jrt.procedure.Procedure;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.util.Classification;

/**
 * Created by davide on 9/7/14.
 */
public class JRoutine {

    private static final int DEFAULT_RECYCLE = 1;

    private static final int DEFAULT_RETAIN = 10;

    private static volatile Runner sBackgroundRunner;

    private final int mMaxInstancePerCall;

    private final int mMaxInstanceRecycled;

    private final Runner mRunner;

    private JRoutine(final Runner runner, final int maxPerCall, final int maxRecycled) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (maxPerCall < 1) {

            throw new IllegalArgumentException();
        }

        if (maxRecycled < 1) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mMaxInstancePerCall = maxPerCall;
        mMaxInstanceRecycled = maxRecycled;
    }

    public static JRoutine jrt() {

        if (sBackgroundRunner == null) {

            sBackgroundRunner = Runners.pool(getBestPoolSize());
        }

        return new JRoutine(sBackgroundRunner, DEFAULT_RECYCLE, DEFAULT_RETAIN);
    }

    private static int getBestPoolSize() {

        final int processors = Runtime.getRuntime().availableProcessors();

        if (processors < 4) {

            return Math.max(1, processors - 1);
        }

        return (processors / 2);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> exec(
            final Classification<? extends Procedure<INPUT, OUTPUT>> classification,
            final Object... ctorArgs) {

        if (classification == null) {

            throw new IllegalArgumentException();
        }

        return new ProcedureRoutine<INPUT, OUTPUT>(mRunner, mMaxInstancePerCall,
                                                   mMaxInstanceRecycled,
                                                   classification.getRawType(), ctorArgs);
    }

    public JRoutine in(final Runner runner) {

        return new JRoutine(runner, mMaxInstancePerCall, mMaxInstanceRecycled);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> loop(
            final Classification<? extends LoopProcedure<INPUT, OUTPUT>> classification,
            final Object... ctorArgs) {

        if (classification == null) {

            throw new IllegalArgumentException();
        }

        return new LoopProcedureRoutine<INPUT, OUTPUT>(mRunner, mMaxInstancePerCall,
                                                       mMaxInstanceRecycled,
                                                       classification.getRawType(), ctorArgs);
    }

    public JRoutine maxPerCall(final int instanceCount) {

        return new JRoutine(mRunner, instanceCount, mMaxInstanceRecycled);
    }

    public JRoutine maxRecycled(final int instanceCount) {

        return new JRoutine(mRunner, mMaxInstancePerCall, instanceCount);
    }
}