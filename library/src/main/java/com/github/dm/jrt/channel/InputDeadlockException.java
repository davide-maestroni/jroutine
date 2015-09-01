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
 * Exception indicating a possible deadlock while waiting for room in the input channel buffer.
 * <p/>
 * Created by davide-maestroni on 07/19/15.
 */
public class InputDeadlockException extends DeadlockException {

    /**
     * Constructor.
     *
     * @param message the error message.
     */
    public InputDeadlockException(@Nullable final String message) {

        super(message + "\nTry increasing the timeout or the max number of inputs");
    }
}
