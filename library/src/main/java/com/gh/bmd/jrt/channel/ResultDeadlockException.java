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
package com.gh.bmd.jrt.channel;

import javax.annotation.Nullable;

/**
 * Exception indicating a possible deadlock while waiting for results to become available.
 * <p/>
 * Created by davide-maestroni on 19/07/15.
 */
public class ResultDeadlockException extends DeadlockException {

    /**
     * Constructor.
     *
     * @param message the error message.
     */
    public ResultDeadlockException(@Nullable final String message) {

        super(message);
    }
}
