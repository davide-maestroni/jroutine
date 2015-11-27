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
package com.github.dm.jrt.routine;

import com.github.dm.jrt.channel.Channel.OutputChannel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Empty abstract implementation of a routine.
 * <p/>
 * This class is useful to avoid the need of implementing some of the methods defined in the
 * interface.
 * <p/>
 * Created by davide-maestroni on 10/17/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class TemplateRoutine<IN, OUT> implements Routine<IN, OUT> {

    @NotNull
    public OutputChannel<OUT> asyncCall() {

        return asyncInvoke().result();
    }

    @NotNull
    public OutputChannel<OUT> asyncCall(@Nullable final IN input) {

        return asyncInvoke().pass(input).result();
    }

    @NotNull
    public OutputChannel<OUT> asyncCall(@Nullable final IN... inputs) {

        return asyncInvoke().pass(inputs).result();
    }

    @NotNull
    public OutputChannel<OUT> asyncCall(@Nullable final Iterable<? extends IN> inputs) {

        return asyncInvoke().pass(inputs).result();
    }

    @NotNull
    public OutputChannel<OUT> asyncCall(@Nullable final OutputChannel<? extends IN> inputs) {

        return asyncInvoke().pass(inputs).result();
    }

    @NotNull
    public OutputChannel<OUT> parallelCall() {

        return parallelInvoke().result();
    }

    @NotNull
    public OutputChannel<OUT> parallelCall(@Nullable final IN input) {

        return parallelInvoke().pass(input).result();
    }

    @NotNull
    public OutputChannel<OUT> parallelCall(@Nullable final IN... inputs) {

        return parallelInvoke().pass(inputs).result();
    }

    @NotNull
    public OutputChannel<OUT> parallelCall(@Nullable final Iterable<? extends IN> inputs) {

        return parallelInvoke().pass(inputs).result();
    }

    @NotNull
    public OutputChannel<OUT> parallelCall(@Nullable final OutputChannel<? extends IN> inputs) {

        return parallelInvoke().pass(inputs).result();
    }

    public void purge() {

    }

    @NotNull
    public OutputChannel<OUT> syncCall() {

        return syncInvoke().result();
    }

    @NotNull
    public OutputChannel<OUT> syncCall(@Nullable final IN input) {

        return syncInvoke().pass(input).result();
    }

    @NotNull
    public OutputChannel<OUT> syncCall(@Nullable final IN... inputs) {

        return syncInvoke().pass(inputs).result();
    }

    @NotNull
    public OutputChannel<OUT> syncCall(@Nullable final Iterable<? extends IN> inputs) {

        return syncInvoke().pass(inputs).result();
    }

    @NotNull
    public OutputChannel<OUT> syncCall(@Nullable final OutputChannel<? extends IN> inputs) {

        return syncInvoke().pass(inputs).result();
    }
}
