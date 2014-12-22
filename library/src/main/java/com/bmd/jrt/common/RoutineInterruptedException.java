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
package com.bmd.jrt.common;

import javax.annotation.Nullable;

/**
 * Exception wrapping a thread interrupted exception caught inside a routine execution.
 * <p/>
 * Created by davide on 9/8/14.
 */
public class RoutineInterruptedException extends RoutineException {

    /**
     * Constructor.
     *
     * @param cause the wrapped exception.
     */
    public RoutineInterruptedException(@Nullable final InterruptedException cause) {

        super(cause);
    }

    /**
     * Sets the interrupt flag of the current thread and throws a wrapped exception.
     *
     * @param exception the thread interrupted exception.
     * @throws RoutineInterruptedException always.
     */
    public static void interrupt(@Nullable final InterruptedException exception) throws
            RoutineInterruptedException {

        Thread.currentThread().interrupt();
        throw new RoutineInterruptedException(exception);
    }

    /**
     * Sets the interrupt flag of the current thread and returns this exception.
     *
     * @return this exception.
     */
    public RoutineInterruptedException interrupt() {

        Thread.currentThread().interrupt();
        return this;
    }
}
