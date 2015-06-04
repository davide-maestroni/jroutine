/*
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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.invocation.InvocationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helper class handling routine exceptions so that they can be transferred through the routine
 * channels.
 * <p/>
 * Created by davide-maestroni on 9/8/14.
 */
class RoutineExceptionWrapper {

    private final Throwable mCause;

    /**
     * Constructor.
     *
     * @param cause the cause exception.
     */
    private RoutineExceptionWrapper(@Nullable final Throwable cause) {

        mCause = cause;
    }

    /**
     * If the specified object is a wrapper instance, the wrapped routine exception is thrown.
     *
     * @param obj the object to check.
     * @throws com.gh.bmd.jrt.channel.RoutineException if the specified object is an exception
     *                                                wrapper instance.
     */
    static void raise(@Nullable final Object obj) {

        if (obj instanceof RoutineExceptionWrapper) {

            throw ((RoutineExceptionWrapper) obj).raise();
        }
    }

    /**
     * Creates an instance wrapping the specified exception.
     *
     * @param t the throwable to wrap.
     * @return the new wrapper instance.
     */
    @Nonnull
    static RoutineExceptionWrapper wrap(@Nullable final Throwable t) {

        return new RoutineExceptionWrapper(t);
    }

    /**
     * Returns the cause exception.
     *
     * @return the cause.
     */
    @Nullable
    Throwable getCause() {

        final Throwable cause = mCause;

        if (cause instanceof InvocationException) {

            return cause.getCause();
        }

        return cause;
    }

    /**
     * Returns a routine exception wrapping the cause one.
     *
     * @return the routine exception.
     */
    @Nonnull
    RoutineException raise() {

        final Throwable cause = mCause;

        if (cause instanceof RoutineException) {

            return ((RoutineException) cause);
        }

        return new InvocationException(cause);
    }
}
