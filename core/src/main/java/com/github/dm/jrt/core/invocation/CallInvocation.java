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

package com.github.dm.jrt.core.invocation;

import com.github.dm.jrt.core.channel.ResultChannel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Special abstract implementation that centralizes the routine invocation inside a single method,
 * which gets called only when all the inputs are available.
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class CallInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

    private ArrayList<IN> mInputs;

    @Override
    public final void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {
        if (mInputs == null) {
            mInputs = new ArrayList<IN>();
        }

        mInputs.add(input);
    }

    @Override
    public final void onResult(@NotNull final ResultChannel<OUT> result) throws Exception {
        final ArrayList<IN> inputs = mInputs;
        final ArrayList<IN> clone;
        if (inputs == null) {
            clone = new ArrayList<IN>(0);

        } else {
            clone = new ArrayList<IN>(inputs);
        }

        onCall(clone, result);
    }

    @Override
    public final void onTerminate() {
        final ArrayList<IN> inputs = mInputs;
        if (inputs != null) {
            inputs.clear();
        }
    }

    /**
     * This method is called when all the inputs are available and ready to be processed.
     *
     * @param inputs the input list.
     * @param result the result channel.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    protected abstract void onCall(@NotNull List<? extends IN> inputs,
            @NotNull ResultChannel<OUT> result) throws Exception;
}
