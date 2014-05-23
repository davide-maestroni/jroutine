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
 * This exception is thrown when another exception need to be wrapped inside a
 * {@link RuntimeException}.
 * <p/>
 * Created by davide on 3/10/14.
 */
public class FloatingException extends RuntimeException {

    private final Object mDebris;

    /**
     * Overrides {@link RuntimeException#RuntimeException()}.
     */
    public FloatingException() {

        this((Object) null);
    }

    /**
     * Like {@link RuntimeException#RuntimeException()} with an additional debris parameter.
     */
    public FloatingException(final Object debris) {

        mDebris = debris;
    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(String)}.
     */
    public FloatingException(final String message) {

        this(message, (Object) null);
    }

    /**
     * Like {@link RuntimeException#RuntimeException(String)} with an additional debris parameter.
     */
    public FloatingException(final String message, final Object debris) {

        super(message);

        mDebris = debris;
    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(String, Throwable)}.
     */
    public FloatingException(final String message, final Throwable cause) {

        this(message, cause, null);
    }

    /**
     * Like {@link RuntimeException#RuntimeException(String, Throwable)} with an additional debris parameter.
     */
    public FloatingException(final String message, final Throwable cause, final Object debris) {

        super(message, cause);

        mDebris = debris;
    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(Throwable)}.
     */
    public FloatingException(final Throwable cause) {

        this(cause, null);
    }

    /**
     * Like {@link RuntimeException#RuntimeException(Throwable)} with an additional debris
     * parameter.
     */
    public FloatingException(final Throwable cause, final Object debris) {

        super(cause);

        mDebris = debris;
    }

    /**
     * Overrides {@link RuntimeException#RuntimeException(String, Throwable, boolean, boolean)}.
     */
    public FloatingException(final String message, final Throwable cause,
            final boolean enableSuppression, final boolean writableStackTrace) {

        this(message, cause, enableSuppression, writableStackTrace, null);
    }

    /**
     * Like {@link RuntimeException#RuntimeException(String, Throwable, boolean, boolean)} with an
     * additional debris parameter.
     */
    public FloatingException(final String message, final Throwable cause,
            final boolean enableSuppression, final boolean writableStackTrace,
            final Object debris) {

        super(message, cause, enableSuppression, writableStackTrace);

        mDebris = debris;
    }

    /**
     * Returns the debris associated with this exception.
     *
     * @return The debris.
     */
    public Object getDebris() {

        return mDebris;
    }
}