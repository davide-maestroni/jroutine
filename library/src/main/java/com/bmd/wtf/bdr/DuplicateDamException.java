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
 * This exception is thrown when the same instance of a {@link com.bmd.wtf.dam.Dam} is added more
 * than one time to the {@link com.bmd.wtf.Waterfall}.
 * <p/>
 * Created by davide on 3/4/14.
 */
public class DuplicateDamException extends IllegalArgumentException {

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException()}.
     */
    public DuplicateDamException() {

    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(String)}.
     */
    public DuplicateDamException(final String detailMessage) {

        super(detailMessage);
    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(String, Throwable)}.
     */
    public DuplicateDamException(final String message, final Throwable cause) {

        super(message, cause);
    }

    /**
     * Overrides {@link IllegalArgumentException#IllegalArgumentException(Throwable)}.
     */
    public DuplicateDamException(final Throwable cause) {

        super(cause);
    }
}