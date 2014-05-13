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
package com.bmd.wtf.bdr;

/**
 * This exception is thrown when a closed loop of {@link Stream}s is detected.
 * <p/>
 * Created by davide on 2/25/14.
 */
public class ClosedLoopException extends IllegalArgumentException {

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException()}.
     */
    public ClosedLoopException() {

    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(String)}.
     */
    public ClosedLoopException(final String detailMessage) {

        super(detailMessage);
    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(String, Throwable)}.
     */
    public ClosedLoopException(final String message, final Throwable cause) {

        super(message, cause);
    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(Throwable)}.
     */
    public ClosedLoopException(final Throwable cause) {

        super(cause);
    }
}