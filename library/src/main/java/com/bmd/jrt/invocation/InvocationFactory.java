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
package com.bmd.jrt.invocation;

import javax.annotation.Nonnull;

/**
 * Invocation factory interface.
 * <p/>
 * Created by davide on 2/12/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface InvocationFactory<INPUT, OUTPUT> {

    /**
     * Creates and return a new invocation instance.
     *
     * @return the invocation instance.
     */
    @Nonnull
    public Invocation<INPUT, OUTPUT> createInvocation();
}
