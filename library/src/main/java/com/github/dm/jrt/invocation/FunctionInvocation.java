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

import com.github.dm.jrt.channel.ResultChannel;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Special abstract implementation that centralizes the routine invocation inside a single method,
 * which gets called only when all the inputs are available.
 * <p/>
 * The implementing class may additionally override the invocation methods to specifically handle
 * the object lifecycle. Note anyway that the superclass must be invoked in order to properly work.
 * <p/>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class FunctionInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

    private ArrayList<IN> mInputs;

    @Override
    public void onInput(final IN input, @Nonnull final ResultChannel<OUT> result) {

        if (mInputs == null) {

            mInputs = new ArrayList<IN>();
        }

        mInputs.add(input);
    }

    @Override
    public void onResult(@Nonnull final ResultChannel<OUT> result) {

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
    public void onTerminate() {

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
     */
    protected abstract void onCall(@Nonnull List<? extends IN> inputs,
            @Nonnull ResultChannel<OUT> result);
}