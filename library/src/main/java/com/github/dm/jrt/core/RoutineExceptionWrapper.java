/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core;

import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.invocation.InvocationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class handling routine exceptions so that they can be transferred through the routine
 * channels.
 * <p/>
 * Created by davide-maestroni on 09/08/2014.
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
     * Creates an instance wrapping the specified exception.
     *
     * @param t the throwable to wrap.
     * @return the new wrapper instance.
     */
    @NotNull
    static RoutineExceptionWrapper wrap(@Nullable final Throwable t) {

        return new RoutineExceptionWrapper(t);
    }

    /**
     * Returns a routine exception wrapping the cause one.
     *
     * @return the routine exception.
     */
    @NotNull
    RoutineException raise() {

        return InvocationException.wrapIfNeeded(mCause);
    }
}
