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

import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.jrt.subroutine.SubRoutineLoop;
import com.bmd.jrt.util.Classification;

/**
 * Created by davide on 9/7/14.
 */
public class JRoutine {

    private static final int DEFAULT_RECYCLE = 1;

    private static final int DEFAULT_RETAIN = 10;

    private final int mMaxParallel;

    private final int mMaxRetained;

    private final Runner mRunner;

    private JRoutine(final Runner runner, final int maxParallel, final int maxRetained) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (maxParallel < 1) {

            throw new IllegalArgumentException();
        }

        if (maxRetained < 1) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mMaxParallel = maxParallel;
        mMaxRetained = maxRetained;
    }

    public static JRoutine jrt() {

        return new JRoutine(Runners.shared(), DEFAULT_RECYCLE, DEFAULT_RETAIN);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> exec(
            final Classification<? extends SubRoutine<INPUT, OUTPUT>> classification,
            final Object... ctorArgs) {

        if (classification == null) {

            throw new IllegalArgumentException();
        }

        return new SubRoutineRoutine<INPUT, OUTPUT>(mRunner, mMaxParallel, mMaxRetained,
                                                    classification.getRawType(), ctorArgs);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> loop(
            final Classification<? extends SubRoutineLoop<INPUT, OUTPUT>> classification,
            final Object... ctorArgs) {

        if (classification == null) {

            throw new IllegalArgumentException();
        }

        return new SubRoutineLoopRoutine<INPUT, OUTPUT>(mRunner, mMaxParallel, mMaxRetained,
                                                        classification.getRawType(), ctorArgs);
    }
}