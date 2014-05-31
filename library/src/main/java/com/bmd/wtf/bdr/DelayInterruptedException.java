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
 * This exception is thrown when a thread waiting on a data flow delay is interrupted by an
 * {@link InterruptedException}.
 * <p/>
 * Created by davide on 3/5/14.
 */
public class DelayInterruptedException extends RuntimeException {

    /**
     * Overrides {@link RuntimeException#RuntimeException()}.
     */
    public DelayInterruptedException() {

    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(String)}.
     */
    public DelayInterruptedException(final String detailMessage) {

        super(detailMessage);
    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(String, Throwable)}.
     */
    public DelayInterruptedException(final String detailMessage, final Throwable throwable) {

        super(detailMessage, throwable);
    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(String, Throwable, boolean, boolean)}.
     */
    public DelayInterruptedException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(Throwable)}.
     */
    public DelayInterruptedException(final Throwable throwable) {

        super(throwable);
    }
}