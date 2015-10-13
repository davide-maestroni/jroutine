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
package com.github.dm.jrt.invocation;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract class defining an invocation factory.
 * <p/>
 * Created by davide-maestroni on 02/12/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class InvocationFactory<IN, OUT> {

    /**
     * Creates and return a new invocation instance.<br/>
     * A proper implementation will return a new invocation instance each time it is called, unless
     * the returned object is immutable and does not cause any side effect.<br/>
     * Any behavior other than that may lead to unexpected results.
     *
     * @return the invocation instance.
     */
    @NotNull
    public abstract Invocation<IN, OUT> newInvocation();
}
