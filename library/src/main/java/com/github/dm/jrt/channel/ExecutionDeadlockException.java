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
package com.github.dm.jrt.channel;

import javax.annotation.Nullable;

/**
 * Exception indicating a possible deadlock while waiting for results to become available.
 * <p/>
 * Created by davide-maestroni on 19/07/15.
 */
public class ExecutionDeadlockException extends DeadlockException {

    /**
     * Constructor.<br/>
     * A default message will be set.
     */
    public ExecutionDeadlockException() {

        super("cannot wait on the invocation runner thread: " + Thread.currentThread());
    }

    /**
     * Constructor.
     *
     * @param message the error message.
     */
    public ExecutionDeadlockException(@Nullable final String message) {

        super(message + "\nTry binding the output channel or employing a different runner");
    }
}
