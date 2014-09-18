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
import com.bmd.jrt.util.Classification;

/**
 * Created by davide on 9/7/14.
 */
public class JRoutine {

    private static final int DEFAULT_RETAIN_COUNT = 10;

    private final int mMaxRetained;

    private final Runner mRunner;

    private JRoutine(final Runner runner, final int maxRetained) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (maxRetained < 1) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mMaxRetained = maxRetained;
    }

    public static JRoutine jrt() {

        return new JRoutine(Runners.shared(), DEFAULT_RETAIN_COUNT);
    }

    public JRoutine inside(final Runner runner) {

        return new JRoutine(runner, mMaxRetained);
    }

    public JRoutine maxRetained(final int maxRetainedInstances) {

        return new JRoutine(mRunner, maxRetainedInstances);
    }

    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> routineOf(
            final Classification<? extends SubRoutine<INPUT, OUTPUT>> classification,
            final Object... ctorArgs) {

        if (classification == null) {

            throw new IllegalArgumentException();
        }

        return new DefaultRoutine<INPUT, OUTPUT>(mRunner, mMaxRetained, classification.getRawType(),
                                                 ctorArgs);
    }
}