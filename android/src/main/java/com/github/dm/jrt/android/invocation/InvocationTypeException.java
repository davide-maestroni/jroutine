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
package com.github.dm.jrt.android.invocation;

/**
 * Exception indicating a clash of routine invocations with same ID but different types.
 * <p/>
 * Created by davide-maestroni on 12/14/2014.
 */
public class InvocationTypeException extends LoaderInvocationException {

    /**
     * Constructor.
     *
     * @param id the loader ID.
     */
    public InvocationTypeException(final int id) {

        super(id);
    }
}
