/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.v11.stream;

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class acting as a factory of stream routine builders.
 * <p>
 * A stream routine builder allows to easily build a concatenation of invocations as a single
 * routine.
 * <br>
 * For instance, a routine computing the root mean square of a number of integers can be defined as:
 * <pre>
 *     <code>
 *
 *         final Routine&lt;Integer, Double&gt; rms =
 *                 JRoutineLoaderStream.&lt;Integer&gt;withStream()
 *                                     .on(loaderFrom(activity))
 *                                     .map(i -&gt; i * i)
 *                                     .straight()
 *                                     .map(averageFloat())
 *                                     .map(Math::sqrt)
 *                                     .buildRoutine();
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 07/03/2016.
 */
public class JRoutineLoaderStream {

    /**
     * Avoid explicit instantiation.
     */
    protected JRoutineLoaderStream() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a stream routine builder.
     *
     * @param <IN> the input data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN> LoaderStreamBuilder<IN, IN> withStream() {
        return new DefaultLoaderStreamBuilder<IN, IN>();
    }
}
