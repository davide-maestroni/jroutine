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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.invocation.Invocation;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining an object managing the creation and the recycling of invocation instances.
 * <p>
 * Created by davide-maestroni on 11/26/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
interface InvocationManager<IN, OUT> {

    /**
     * Creates a new invocation instance.
     *
     * @param observer the invocation observer.
     * @return whether the invocation is immediately created (false if delayed).
     */
    boolean create(@NotNull InvocationObserver<IN, OUT> observer);

    /**
     * Discards the specified invocation.
     *
     * @param invocation the invocation instance.
     */
    void discard(@NotNull Invocation<IN, OUT> invocation);

    /**
     * Recycles the specified invocation.
     *
     * @param invocation the invocation instance.
     */
    void recycle(@NotNull Invocation<IN, OUT> invocation);
}
