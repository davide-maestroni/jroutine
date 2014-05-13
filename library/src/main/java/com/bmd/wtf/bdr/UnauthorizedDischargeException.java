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
 * This exception is thrown when a {@link com.bmd.wtf.src.Floodgate} is used outside the authorized
 * area. That is, inside the call to the {@link com.bmd.wtf.dam.Dam} methods.
 * <p/>
 * Created by davide on 2/28/14.
 */
public class UnauthorizedDischargeException extends IllegalStateException {

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException()}.
     */
    public UnauthorizedDischargeException() {

    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(String)}.
     */
    public UnauthorizedDischargeException(final String detailMessage) {

        super(detailMessage);
    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(String, Throwable)}.
     */
    public UnauthorizedDischargeException(final String message, final Throwable cause) {

        super(message, cause);
    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(Throwable)}.
     */
    public UnauthorizedDischargeException(final Throwable cause) {

        super(cause);
    }
}