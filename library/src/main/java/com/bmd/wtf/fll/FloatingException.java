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
package com.bmd.wtf.fll;

/**
 * This exception is thrown when an internal exception escapes the waterfall.
 * <p/>
 * Created by davide on 6/30/14.
 */
public class FloatingException extends RuntimeException {

    /**
     * Overrides {@link RuntimeException#RuntimeException(Throwable)}.
     */
    public FloatingException(final Throwable throwable) {

        super(throwable);
    }
}