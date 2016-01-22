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

package com.github.dm.jrt.invocation;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of an invocation filtering each input and transforming it in output data.
 * <p/>
 * Note that the implementing class must not retain an internal variable state.
 * <p/>
 * Created by davide-maestroni on 02/14/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class FilterInvocation<IN, OUT> extends InvocationFactory<IN, OUT>
        implements Invocation<IN, OUT> {

    @NotNull
    @Override
    public final Invocation<IN, OUT> newInvocation() {

        return this;
    }

    public final void onAbort(@NotNull final RoutineException reason) {

    }

    public final void onDestroy() {

    }

    public final void onInitialize() {

    }

    public final void onResult(@NotNull final ResultChannel<OUT> result) {

    }

    public final void onTerminate() {

    }
}
