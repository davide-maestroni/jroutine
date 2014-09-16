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

import com.bmd.jrt.util.RoutineException;

/**
 * Created by davide on 9/8/14.
 */
public class RoutineExceptionWrapper {

    private final Throwable mCause;

    private RoutineExceptionWrapper(final Throwable cause) {

        mCause = cause;
    }

    public static void raise(final Object obj) {

        if (obj instanceof RoutineExceptionWrapper) {

            throw ((RoutineExceptionWrapper) obj).raise();
        }
    }

    public static RoutineExceptionWrapper wrap(final Throwable t) {

        return new RoutineExceptionWrapper(t);
    }

    public Throwable getCause() {

        final Throwable cause = mCause;

        if (cause instanceof RoutineException) {

            return cause.getCause();
        }

        return cause;
    }

    public RoutineException raise() {

        final Throwable cause = mCause;

        if (cause instanceof RoutineException) {

            return ((RoutineException) cause);
        }

        return new RoutineException(cause);
    }
}