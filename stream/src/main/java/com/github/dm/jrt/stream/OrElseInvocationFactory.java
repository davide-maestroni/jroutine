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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation factory generating outputs when no one has been received.
 * <p>
 * Created by davide-maestroni on 04/29/2016.
 *
 * @param <DATA> the data type.
 */
class OrElseInvocationFactory<DATA> extends InvocationFactory<DATA, DATA> {

    private final List<DATA> mOutputs;

    /**
     * Constructor.
     *
     * @param outputs the list of outputs.
     */
    OrElseInvocationFactory(@NotNull final List<DATA> outputs) {

        super(asArgs(outputs));
        mOutputs = outputs;
    }

    @NotNull
    @Override
    public Invocation<DATA, DATA> newInvocation() {

        return new OrElseInvocation<DATA>(mOutputs);
    }

    /**
     * Invocation generating outputs when no one has been received.
     *
     * @param <DATA> the data type.
     */
    private static class OrElseInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

        private final List<DATA> mOutputs;

        private boolean mHasOutputs;

        /**
         * Constructor.
         *
         * @param outputs the list of outputs.
         */
        OrElseInvocation(@NotNull final List<DATA> outputs) {

            mOutputs = outputs;
        }

        @Override
        public void onInitialize() {

            mHasOutputs = false;
        }

        @Override
        public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) {

            mHasOutputs = true;
            result.pass(input);
        }

        @Override
        public void onResult(@NotNull final ResultChannel<DATA> result) {

            if (!mHasOutputs) {
                result.pass(mOutputs);
            }
        }
    }
}
